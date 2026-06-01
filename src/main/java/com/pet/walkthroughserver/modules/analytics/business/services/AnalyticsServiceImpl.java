package com.pet.walkthroughserver.modules.analytics.business.services;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.pet.walkthroughserver.modules.analytics.utils.AnalyticsUtils.*;

import com.pet.walkthroughserver.modules.analytics.business.models.AuthorWalkthroughSummary;
import com.pet.walkthroughserver.modules.analytics.business.models.ChapterAttention;
import com.pet.walkthroughserver.modules.analytics.business.models.RepoMetrics;
import com.pet.walkthroughserver.modules.analytics.business.models.ReviewProgress;
import com.pet.walkthroughserver.modules.analytics.business.models.UnreadSummary;
import com.pet.walkthroughserver.modules.analytics.repository.AnalyticsQueryRepository;
import com.pet.walkthroughserver.modules.comment.repository.CommentRepository;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsQueryRepository analyticsRepo;
    private final WalkthroughRepository walkthroughRepository;
    private final CommentRepository commentRepository;

    // ── 6.1 Reading matrix ──

    @Override
    public ReviewProgress getReviewProgress(UUID userId, UUID walkthroughId) {
        WalkthroughEntity wt = loadAsAuthor(userId, walkthroughId);
        List<ChapterEntity> chapters = sortedChapters(wt);
        int totalChapters = chapters.size();

        List<Tuple> reviewerRows = analyticsRepo.findReviewersForWalkthrough(walkthroughId);
        Map<UUID, Map<UUID, Tuple>> statsByUserChapter = groupChapterStats(walkthroughId);

        List<ReviewProgress.Reviewer> reviewers = new ArrayList<>();
        for (Tuple r : reviewerRows) {
            UUID reviewerId = (UUID) r.get("user_id");
            Map<UUID, Tuple> chapterStats = statsByUserChapter.getOrDefault(reviewerId, Map.of());

            List<ReviewProgress.ChapterProgress> chapterModels = new ArrayList<>();
            for (ChapterEntity c : chapters) {
                Tuple s = chapterStats.get(c.getId());
                chapterModels.add(new ReviewProgress.ChapterProgress(
                        c.getId(),
                        c.getTitle(),
                        c.getSortOrder(),
                        s != null && toBool(s.get("marked_as_read")),
                        s != null ? toInt(s.get("time_spent_sec")) : 0,
                        s != null ? toInt(s.get("comment_count")) : 0,
                        s != null ? toInt(s.get("view_count")) : 0));
            }

            int readChapters = toInt(r.get("read_chapters"));
            double rate = totalChapters == 0 ? 0.0 : ((double) readChapters) / totalChapters;

            reviewers.add(new ReviewProgress.Reviewer(
                    reviewerId,
                    (String) r.get("username"),
                    (String) r.get("display_name"),
                    (String) r.get("avatar_url"),
                    readChapters,
                    totalChapters,
                    round2(rate),
                    toInt(r.get("time_spent_sec")),
                    toInstant(r.get("read_at")),
                    chapterModels));
        }

        return new ReviewProgress(walkthroughId, totalChapters, reviewers);
    }

    // ── 6.2 Chapter attention ──

    @Override
    public ChapterAttention getChapterAttention(UUID userId, UUID walkthroughId) {
        WalkthroughEntity wt = loadAsAuthor(userId, walkthroughId);
        List<ChapterEntity> chapters = sortedChapters(wt);

        Map<UUID, Map<UUID, Tuple>> statsByUserChapter = groupChapterStats(walkthroughId);
        // invert: chapter -> list of (user, stats)
        Map<UUID, List<Map.Entry<UUID, Tuple>>> byChapter = new HashMap<>();
        for (var byUser : statsByUserChapter.entrySet()) {
            for (var byChap : byUser.getValue().entrySet()) {
                byChapter.computeIfAbsent(byChap.getKey(), k -> new ArrayList<>())
                        .add(Map.entry(byUser.getKey(), byChap.getValue()));
            }
        }

        // user lookup table from reviewer rows
        Map<UUID, Tuple> userMeta = new HashMap<>();
        for (Tuple r : analyticsRepo.findReviewersForWalkthrough(walkthroughId)) {
            userMeta.put((UUID) r.get("user_id"), r);
        }

        Map<UUID, Integer> commentsByChapter = new HashMap<>();
        for (Tuple t : analyticsRepo.countCommentsByChapter(walkthroughId)) {
            commentsByChapter.put((UUID) t.get("chapter_id"), toInt(t.get("total_comments")));
        }

        Map<UUID, Integer> expectedSecByChapter = new HashMap<>();
        for (Tuple t : analyticsRepo.findChapterWeights(walkthroughId)) {
            int fileCount = toInt(t.get("file_count"));
            int patchLines = toInt(t.get("patch_line_count"));
            expectedSecByChapter.put((UUID) t.get("chapter_id"), expectedReadSec(fileCount, patchLines));
        }

        int reviewerCount = userMeta.size();

        List<ChapterAttention.Chapter> chapterModels = new ArrayList<>();
        for (ChapterEntity c : chapters) {
            List<Map.Entry<UUID, Tuple>> entries = byChapter.getOrDefault(c.getId(), List.of());
            int expectedSec = expectedSecByChapter.getOrDefault(c.getId(), MIN_EXPECTED_READ_SEC);
            List<ChapterAttention.AttentionEntry> attention = new ArrayList<>();
            int markedCount = 0;
            int skimmedCount = 0;
            for (var e : entries) {
                Tuple s = e.getValue();
                Tuple u = userMeta.get(e.getKey());
                boolean marked = toBool(s.get("marked_as_read"));
                int timeSpent = toInt(s.get("time_spent_sec"));
                boolean skimmed = marked && timeSpent < expectedSec * SKIM_THRESHOLD_RATIO;
                if (marked) markedCount++;
                if (skimmed) skimmedCount++;
                attention.add(new ChapterAttention.AttentionEntry(
                        e.getKey(),
                        u != null ? (String) u.get("username") : null,
                        u != null ? (String) u.get("display_name") : null,
                        u != null ? (String) u.get("avatar_url") : null,
                        timeSpent,
                        marked,
                        skimmed,
                        toInt(s.get("comment_count")),
                        toInt(s.get("view_count"))));
            }

            chapterModels.add(new ChapterAttention.Chapter(
                    c.getId(),
                    c.getTitle(),
                    c.getSortOrder(),
                    commentsByChapter.getOrDefault(c.getId(), 0),
                    reviewerCount > 0 && markedCount >= reviewerCount,
                    skimmedCount,
                    attention));
        }

        return new ChapterAttention(walkthroughId, chapterModels);
    }

    // ── 6.3 Unread summary ──

    @Override
    public UnreadSummary getUnreadSummary(UUID userId, UUID walkthroughId) {
        WalkthroughEntity wt = loadAsAuthor(userId, walkthroughId);
        List<ChapterEntity> chapters = sortedChapters(wt);

        List<Tuple> reviewerRows = analyticsRepo.findReviewersForWalkthrough(walkthroughId);
        Map<UUID, Tuple> userMeta = new HashMap<>();
        for (Tuple r : reviewerRows) {
            userMeta.put((UUID) r.get("user_id"), r);
        }

        Map<UUID, Map<UUID, Tuple>> statsByUserChapter = groupChapterStats(walkthroughId);

        List<UnreadSummary.UnreadChapter> unreadChapters = new ArrayList<>();
        boolean allRead = true;
        for (ChapterEntity c : chapters) {
            List<UnreadSummary.UnreadByUser> unreadBy = new ArrayList<>();
            for (UUID reviewerId : userMeta.keySet()) {
                Map<UUID, Tuple> stats = statsByUserChapter.getOrDefault(reviewerId, Map.of());
                Tuple cell = stats.get(c.getId());
                boolean marked = cell != null && toBool(cell.get("marked_as_read"));
                if (!marked) {
                    Tuple u = userMeta.get(reviewerId);
                    unreadBy.add(new UnreadSummary.UnreadByUser(
                            reviewerId,
                            (String) u.get("username"),
                            (String) u.get("display_name"),
                            (String) u.get("avatar_url")));
                }
            }
            if (!unreadBy.isEmpty()) {
                allRead = false;
                unreadChapters.add(new UnreadSummary.UnreadChapter(
                        c.getId(),
                        c.getTitle(),
                        c.getSortOrder(),
                        unreadBy));
            }
        }

        return new UnreadSummary(
                walkthroughId,
                !reviewerRows.isEmpty() && allRead,
                unreadChapters);
    }

    // ── 6.4 Repo metrics ──

    @Override
    public RepoMetrics getRepoMetrics(UUID userId, String owner, String repo, LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now().plusDays(1);
        Instant fromI = AnalyticsQueryRepository.startOfDayUtc(from);
        Instant toI = AnalyticsQueryRepository.startOfDayUtc(to);

        Tuple s = analyticsRepo.repoSummary(owner, repo, fromI, toI);

        List<RepoMetrics.Member> members = new ArrayList<>();
        for (Tuple m : analyticsRepo.repoMemberMetrics(owner, repo, fromI, toI)) {
            members.add(new RepoMetrics.Member(
                    (UUID) m.get("user_id"),
                    (String) m.get("username"),
                    (String) m.get("display_name"),
                    (String) m.get("avatar_url"),
                    toInt(m.get("walkthroughs_reviewed")),
                    round2(toDouble(m.get("avg_completion_rate"))),
                    toInt(m.get("avg_time_spent_sec")),
                    toInt(m.get("total_comments"))));
        }

        List<RepoMetrics.TrendPoint> trend = new ArrayList<>();
        for (Tuple t : analyticsRepo.repoWeeklyTrend(owner, repo, fromI, toI)) {
            trend.add(new RepoMetrics.TrendPoint(
                    (String) t.get("week"),
                    round2(toDouble(t.get("avg_completion_rate"))),
                    toInt(t.get("reviews_completed"))));
        }

        return new RepoMetrics(
                owner + "/" + repo,
                new RepoMetrics.Period(from, to),
                new RepoMetrics.Summary(
                        toInt(s.get("total_walkthroughs")),
                        toInt(s.get("total_reviews")),
                        round2(toDouble(s.get("avg_completion_rate"))),
                        toInt(s.get("avg_time_to_complete_sec")),
                        round2(toDouble(s.get("avg_chapters_per_walkthrough"))),
                        toInt(s.get("active_reviewers"))),
                members,
                trend);
    }

    // ── Author summary list (image #2) ──

    @Override
    public List<AuthorWalkthroughSummary> getAuthorSummary(UUID userId, String owner, String repo) {
        // Show only published walkthroughs (skip drafts), optionally scoped to a repo
        boolean hasRepoScope = owner != null && !owner.isBlank() && repo != null && !repo.isBlank();
        List<WalkthroughEntity> wts = (hasRepoScope
                ? walkthroughRepository.findByUserIdAndOwnerAndRepoOrderByUpdatedAtDesc(userId, owner, repo)
                : walkthroughRepository.findByUserIdOrderByUpdatedAtDesc(userId))
                .stream()
                .filter(w -> w.getStatus() != WalkthroughStatus.DRAFT)
                .sorted(Comparator.comparing(WalkthroughEntity::getPrNumber, Comparator.reverseOrder())
                        .thenComparing(WalkthroughEntity::getStatus)
                        .thenComparing(WalkthroughEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<AuthorWalkthroughSummary> out = new ArrayList<>(wts.size());
        for (WalkthroughEntity wt : wts) {
            int totalChapters = wt.getChapters().size();
            long commentCount = commentRepository.countByWalkthroughId(wt.getId());

            List<Tuple> reviewerRows = analyticsRepo.findReviewersForWalkthrough(wt.getId());
            Map<UUID, Map<UUID, Tuple>> statsByUserChapter = groupChapterStats(wt.getId());

            List<AuthorWalkthroughSummary.ReviewerStatus> reviewers = new ArrayList<>();
            Instant lastActivity = wt.getUpdatedAt();
            int unreadChapterCount = 0;

            // Count chapters not yet marked as read by at least one reviewer
            for (ChapterEntity c : wt.getChapters()) {
                for (UUID reviewerId : statsByUserChapter.keySet()) {
                    Tuple cell = statsByUserChapter.get(reviewerId).get(c.getId());
                    boolean marked = cell != null && toBool(cell.get("marked_as_read"));
                    if (!marked) {
                        unreadChapterCount++;
                        break;
                    }
                }
            }

            for (Tuple r : reviewerRows) {
                Instant readAt = toInstant(r.get("read_at"));
                if (lastActivity == null || (readAt != null && readAt.isAfter(lastActivity))) {
                    lastActivity = readAt;
                }
                reviewers.add(new AuthorWalkthroughSummary.ReviewerStatus(
                        (UUID) r.get("user_id"),
                        (String) r.get("username"),
                        (String) r.get("display_name"),
                        (String) r.get("avatar_url"),
                        toInt(r.get("read_chapters")),
                        totalChapters,
                        readAt));
            }

            reviewers.sort(Comparator.comparing(
                    AuthorWalkthroughSummary.ReviewerStatus::lastActiveAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));

            out.add(new AuthorWalkthroughSummary(
                    wt.getId(),
                    wt.getTitle(),
                    wt.getDescription(),
                    wt.getOwner(),
                    wt.getRepo(),
                    wt.getPrNumber(),
                    wt.getStatus(),
                    totalChapters,
                    (int) commentCount,
                    unreadChapterCount,
                    lastActivity,
                    reviewers));
        }

        return out;
    }

    // ── helpers ──

    private WalkthroughEntity loadAsAuthor(UUID userId, UUID walkthroughId) {
        WalkthroughEntity wt = walkthroughRepository.findById(walkthroughId)
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));
        if (!wt.getUserId().equals(userId)) {
            throw new WalkthroughAccessDeniedException("Only the author can view analytics for this walkthrough");
        }
        return wt;
    }

    private List<ChapterEntity> sortedChapters(WalkthroughEntity wt) {
        return wt.getChapters().stream()
                .sorted(Comparator.comparing(ChapterEntity::getSortOrder))
                .toList();
    }

    private Map<UUID, Map<UUID, Tuple>> groupChapterStats(UUID walkthroughId) {
        Map<UUID, Map<UUID, Tuple>> out = new HashMap<>();
        for (Tuple t : analyticsRepo.findChapterStatsForWalkthrough(walkthroughId)) {
            UUID u = (UUID) t.get("user_id");
            UUID c = (UUID) t.get("chapter_id");
            out.computeIfAbsent(u, k -> new HashMap<>()).put(c, t);
        }
        return out;
    }

}
