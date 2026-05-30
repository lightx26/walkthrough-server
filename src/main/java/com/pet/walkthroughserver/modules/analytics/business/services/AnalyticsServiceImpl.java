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

import com.pet.walkthroughserver.modules.analytics.presentation.dto.AuthorWalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.AuthorWalkthroughSummaryResponse.ReviewerStatus;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ChapterAttentionResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ChapterAttentionResponse.AttentionEntry;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.RepoMetricsResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ReviewProgressResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ReviewProgressResponse.Reviewer;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ReviewProgressResponse.ReviewerChapter;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.UnreadSummaryResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.UnreadSummaryResponse.UnreadByUser;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.UnreadSummaryResponse.UnreadChapter;
import com.pet.walkthroughserver.modules.analytics.repository.AnalyticsQueryRepository;
import com.pet.walkthroughserver.modules.comment.repository.CommentRepository;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.ChapterEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressRepository;
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
    private final ReadProgressRepository readProgressRepository;
    private final CommentRepository commentRepository;

    // Skim-detection heuristic: expected read time ~= 5s/file + 1s/patch-line, floored at 20s.
    // A reader who marked-as-read in under 30% of that is flagged as possibly skimmed.
    private static final int SEC_PER_FILE = 5;
    private static final int SEC_PER_PATCH_LINE = 1;
    private static final int MIN_EXPECTED_READ_SEC = 20;
    private static final double SKIM_THRESHOLD_RATIO = 0.3;

    private static int expectedReadSec(int fileCount, int patchLineCount) {
        return Math.max(MIN_EXPECTED_READ_SEC,
                fileCount * SEC_PER_FILE + patchLineCount * SEC_PER_PATCH_LINE);
    }

    // ── 6.1 Reading matrix ──

    @Override
    public ReviewProgressResponse getReviewProgress(UUID userId, UUID walkthroughId) {
        WalkthroughEntity wt = loadAsAuthor(userId, walkthroughId);
        List<ChapterEntity> chapters = sortedChapters(wt);
        int totalChapters = chapters.size();

        List<Tuple> reviewerRows = analyticsRepo.findReviewersForWalkthrough(walkthroughId);
        Map<UUID, Map<UUID, Tuple>> statsByUserChapter = groupChapterStats(walkthroughId);

        List<Reviewer> reviewers = new ArrayList<>();
        for (Tuple r : reviewerRows) {
            UUID reviewerId = (UUID) r.get("user_id");
            Map<UUID, Tuple> chapterStats = statsByUserChapter.getOrDefault(reviewerId, Map.of());

            List<ReviewerChapter> chapterDtos = new ArrayList<>();
            for (ChapterEntity c : chapters) {
                Tuple s = chapterStats.get(c.getId());
                chapterDtos.add(ReviewerChapter.builder()
                        .chapterId(c.getId())
                        .chapterTitle(c.getTitle())
                        .order(c.getSortOrder())
                        .markedAsRead(s != null && toBool(s.get("marked_as_read")))
                        .timeSpentSec(s != null ? toInt(s.get("time_spent_sec")) : 0)
                        .commentCount(s != null ? toInt(s.get("comment_count")) : 0)
                        .viewCount(s != null ? toInt(s.get("view_count")) : 0)
                        .build());
            }

            int readChapters = toInt(r.get("read_chapters"));
            double rate = totalChapters == 0 ? 0.0 : ((double) readChapters) / totalChapters;

            reviewers.add(Reviewer.builder()
                    .userId(reviewerId)
                    .username((String) r.get("username"))
                    .displayName((String) r.get("display_name"))
                    .avatarUrl((String) r.get("avatar_url"))
                    .readChapters(readChapters)
                    .totalChapters(totalChapters)
                    .completionRate(round2(rate))
                    .totalTimeSpentSec(toInt(r.get("time_spent_sec")))
                    .lastActiveAt(toInstant(r.get("read_at")))
                    .chapters(chapterDtos)
                    .build());
        }

        return ReviewProgressResponse.builder()
                .walkthroughId(walkthroughId)
                .totalChapters(totalChapters)
                .reviewers(reviewers)
                .build();
    }

    // ── 6.2 Chapter attention ──

    @Override
    public ChapterAttentionResponse getChapterAttention(UUID userId, UUID walkthroughId) {
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

        List<ChapterAttentionResponse.Chapter> chapterDtos = new ArrayList<>();
        for (ChapterEntity c : chapters) {
            List<Map.Entry<UUID, Tuple>> entries = byChapter.getOrDefault(c.getId(), List.of());
            int expectedSec = expectedSecByChapter.getOrDefault(c.getId(), MIN_EXPECTED_READ_SEC);
            List<AttentionEntry> attention = new ArrayList<>();
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
                attention.add(AttentionEntry.builder()
                        .userId(e.getKey())
                        .username(u != null ? (String) u.get("username") : null)
                        .displayName(u != null ? (String) u.get("display_name") : null)
                        .avatarUrl(u != null ? (String) u.get("avatar_url") : null)
                        .timeSpentSec(timeSpent)
                        .markedAsRead(marked)
                        .possiblySkimmed(skimmed)
                        .commentCount(toInt(s.get("comment_count")))
                        .viewCount(toInt(s.get("view_count")))
                        .build());
            }

            chapterDtos.add(ChapterAttentionResponse.Chapter.builder()
                    .chapterId(c.getId())
                    .chapterTitle(c.getTitle())
                    .order(c.getSortOrder())
                    .totalComments(commentsByChapter.getOrDefault(c.getId(), 0))
                    .allRead(reviewerCount > 0 && markedCount >= reviewerCount)
                    .possiblySkimmedCount(skimmedCount)
                    .attention(attention)
                    .build());
        }

        return ChapterAttentionResponse.builder()
                .walkthroughId(walkthroughId)
                .chapters(chapterDtos)
                .build();
    }

    // ── 6.3 Unread summary ──

    @Override
    public UnreadSummaryResponse getUnreadSummary(UUID userId, UUID walkthroughId) {
        WalkthroughEntity wt = loadAsAuthor(userId, walkthroughId);
        List<ChapterEntity> chapters = sortedChapters(wt);

        List<Tuple> reviewerRows = analyticsRepo.findReviewersForWalkthrough(walkthroughId);
        Map<UUID, Tuple> userMeta = new HashMap<>();
        for (Tuple r : reviewerRows) {
            userMeta.put((UUID) r.get("user_id"), r);
        }

        Map<UUID, Map<UUID, Tuple>> statsByUserChapter = groupChapterStats(walkthroughId);

        List<UnreadChapter> unreadChapters = new ArrayList<>();
        boolean allRead = true;
        for (ChapterEntity c : chapters) {
            List<UnreadByUser> unreadBy = new ArrayList<>();
            for (UUID reviewerId : userMeta.keySet()) {
                Map<UUID, Tuple> stats = statsByUserChapter.getOrDefault(reviewerId, Map.of());
                Tuple cell = stats.get(c.getId());
                boolean marked = cell != null && toBool(cell.get("marked_as_read"));
                if (!marked) {
                    Tuple u = userMeta.get(reviewerId);
                    unreadBy.add(UnreadByUser.builder()
                            .userId(reviewerId)
                            .username((String) u.get("username"))
                            .displayName((String) u.get("display_name"))
                            .avatarUrl((String) u.get("avatar_url"))
                            .build());
                }
            }
            if (!unreadBy.isEmpty()) {
                allRead = false;
                unreadChapters.add(UnreadChapter.builder()
                        .chapterId(c.getId())
                        .chapterTitle(c.getTitle())
                        .order(c.getSortOrder())
                        .unreadBy(unreadBy)
                        .build());
            }
        }

        return UnreadSummaryResponse.builder()
                .walkthroughId(walkthroughId)
                .allChaptersReadByAll(reviewerRows.isEmpty() ? false : allRead)
                .unreadChapters(unreadChapters)
                .build();
    }

    // ── 6.4 Repo metrics ──

    @Override
    public RepoMetricsResponse getRepoMetrics(UUID userId, String owner, String repo, LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now().plusDays(1);
        Instant fromI = AnalyticsQueryRepository.startOfDayUtc(from);
        Instant toI = AnalyticsQueryRepository.startOfDayUtc(to);

        Tuple s = analyticsRepo.repoSummary(owner, repo, fromI, toI);

        List<RepoMetricsResponse.Member> members = new ArrayList<>();
        for (Tuple m : analyticsRepo.repoMemberMetrics(owner, repo, fromI, toI)) {
            members.add(RepoMetricsResponse.Member.builder()
                    .userId((UUID) m.get("user_id"))
                    .username((String) m.get("username"))
                    .displayName((String) m.get("display_name"))
                    .avatarUrl((String) m.get("avatar_url"))
                    .walkthroughsReviewed(toInt(m.get("walkthroughs_reviewed")))
                    .avgCompletionRate(round2(toDouble(m.get("avg_completion_rate"))))
                    .avgTimeSpentSec(toInt(m.get("avg_time_spent_sec")))
                    .totalComments(toInt(m.get("total_comments")))
                    .build());
        }

        List<RepoMetricsResponse.TrendPoint> trend = new ArrayList<>();
        for (Tuple t : analyticsRepo.repoWeeklyTrend(owner, repo, fromI, toI)) {
            trend.add(RepoMetricsResponse.TrendPoint.builder()
                    .week((String) t.get("week"))
                    .avgCompletionRate(round2(toDouble(t.get("avg_completion_rate"))))
                    .reviewsCompleted(toInt(t.get("reviews_completed")))
                    .build());
        }

        return RepoMetricsResponse.builder()
                .repo(owner + "/" + repo)
                .period(RepoMetricsResponse.Period.builder().from(from).to(to).build())
                .summary(RepoMetricsResponse.Summary.builder()
                        .totalWalkthroughs(toInt(s.get("total_walkthroughs")))
                        .totalReviews(toInt(s.get("total_reviews")))
                        .avgCompletionRate(round2(toDouble(s.get("avg_completion_rate"))))
                        .avgTimeToCompleteSec(toInt(s.get("avg_time_to_complete_sec")))
                        .avgChaptersPerWalkthrough(round2(toDouble(s.get("avg_chapters_per_walkthrough"))))
                        .activeReviewers(toInt(s.get("active_reviewers")))
                        .build())
                .members(members)
                .trend(trend)
                .build();
    }

    // ── Author summary list (image #2) ──

    @Override
    public List<AuthorWalkthroughSummaryResponse> getAuthorSummary(UUID userId, String owner, String repo) {
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

        List<AuthorWalkthroughSummaryResponse> out = new ArrayList<>(wts.size());
        for (WalkthroughEntity wt : wts) {
            int totalChapters = wt.getChapters().size();
            long commentCount = commentRepository.countByWalkthroughId(wt.getId());

            List<Tuple> reviewerRows = analyticsRepo.findReviewersForWalkthrough(wt.getId());
            Map<UUID, Map<UUID, Tuple>> statsByUserChapter = groupChapterStats(wt.getId());

            List<ReviewerStatus> reviewers = new ArrayList<>();
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
                reviewers.add(ReviewerStatus.builder()
                        .userId((UUID) r.get("user_id"))
                        .username((String) r.get("username"))
                        .displayName((String) r.get("display_name"))
                        .avatarUrl((String) r.get("avatar_url"))
                        .readChapters(toInt(r.get("read_chapters")))
                        .totalChapters(totalChapters)
                        .lastActiveAt(readAt)
                        .build());
            }

            reviewers.sort(Comparator.comparing(
                    ReviewerStatus::getLastActiveAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));

            out.add(AuthorWalkthroughSummaryResponse.builder()
                    .walkthroughId(wt.getId())
                    .title(wt.getTitle())
                    .description(wt.getDescription())
                    .owner(wt.getOwner())
                    .repo(wt.getRepo())
                    .prNumber(wt.getPrNumber())
                    .status(wt.getStatus())
                    .totalChapters(totalChapters)
                    .totalComments((int) commentCount)
                    .unreadChapterCount(unreadChapterCount)
                    .lastActivityAt(lastActivity)
                    .reviewers(reviewers)
                    .build());
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

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }

    private static boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        return Boolean.parseBoolean(o.toString());
    }

    private static Instant toInstant(Object o) {
        return switch (o) {
            case Instant i -> i;
            case java.sql.Timestamp ts -> ts.toInstant();
            case java.time.OffsetDateTime odt -> odt.toInstant();
            case null, default -> null;
        };
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
