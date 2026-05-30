package com.pet.walkthroughserver.modules.analytics.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

/**
 * Custom analytics queries that span multiple modules' tables
 * (chapter_view_events, walkthrough.read_progress, walkthrough.walkthrough_comments, walkthroughs, users).
 *
 * Returns Tuple rows; the service translates them into business DTOs so callers
 * never see Object[].
 */
@Repository
public class AnalyticsQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Reviewers of a walkthrough = distinct users who have a walkthrough.read_progress row
     * for this walkthrough and are NOT the author.
     *
     * Columns: user_id, username, display_name, avatar_url,
     *          read_chapters, total_chapters, time_spent_sec, read_at
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> findReviewersForWalkthrough(UUID walkthroughId) {
        return em.createNativeQuery("""
                SELECT u.id            AS user_id,
                       u.username      AS username,
                       u.display_name  AS display_name,
                       u.avatar_url    AS avatar_url,
                       rp.read_chapters AS read_chapters,
                       rp.total_chapters AS total_chapters,
                       rp.time_spent_sec AS time_spent_sec,
                       rp.read_at      AS read_at
                FROM walkthrough.read_progress rp
                JOIN walkthrough.users u ON u.id = rp.user_id
                JOIN walkthrough.walkthroughs w ON w.id = rp.walkthrough_id
                WHERE rp.walkthrough_id = :walkthroughId
                  AND rp.user_id <> w.user_id
                ORDER BY rp.read_at DESC
                """, Tuple.class)
                .setParameter("walkthroughId", walkthroughId)
                .getResultList();
    }

    /**
     * Per (reviewer, chapter) stats for a walkthrough.
     * One row per (user_id, chapter_id) where the user either viewed the chapter or
     * explicitly marked it as read. Either side can be empty.
     *
     * Columns: user_id, chapter_id, time_spent_sec, marked_as_read,
     *          view_count, comment_count
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> findChapterStatsForWalkthrough(UUID walkthroughId) {
        return em.createNativeQuery("""
                WITH views AS (
                    SELECT cve.user_id,
                           cve.chapter_id,
                           COALESCE(SUM(cve.time_spent_sec), 0)::int AS time_spent_sec,
                           COUNT(cve.id)::int                        AS view_count
                    FROM walkthrough.chapter_view_events cve
                    JOIN walkthrough.walkthrough_chapters c ON c.id = cve.chapter_id
                    WHERE c.walkthrough_id = :walkthroughId
                    GROUP BY cve.user_id, cve.chapter_id
                ),
                marks AS (
                    SELECT m.user_id, m.chapter_id
                    FROM walkthrough.chapter_read_marks m
                    WHERE m.walkthrough_id = :walkthroughId
                )
                SELECT COALESCE(v.user_id, m.user_id)        AS user_id,
                       COALESCE(v.chapter_id, m.chapter_id)  AS chapter_id,
                       COALESCE(v.time_spent_sec, 0)         AS time_spent_sec,
                       (m.user_id IS NOT NULL)               AS marked_as_read,
                       COALESCE(v.view_count, 0)             AS view_count,
                       (
                           SELECT COUNT(cm.id)
                           FROM walkthrough.walkthrough_comments cm
                           LEFT JOIN walkthrough.walkthrough_files wf ON wf.id = cm.walkthrough_file_id
                           WHERE COALESCE(cm.chapter_id, wf.chapter_id) = COALESCE(v.chapter_id, m.chapter_id)
                             AND cm.user_id = COALESCE(v.user_id, m.user_id)
                       )::int                                AS comment_count
                FROM views v
                FULL OUTER JOIN marks m
                  ON m.user_id = v.user_id AND m.chapter_id = v.chapter_id
                """, Tuple.class)
                .setParameter("walkthroughId", walkthroughId)
                .getResultList();
    }

    /**
     * Chapter weights for skim-detection: file count + total patch line count.
     * Columns: chapter_id, file_count, patch_line_count
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> findChapterWeights(UUID walkthroughId) {
        return em.createNativeQuery("""
                SELECT c.id AS chapter_id,
                       COUNT(wf.id)::int AS file_count,
                       COALESCE(SUM(
                           CASE WHEN wf.raw_patch IS NULL OR wf.raw_patch = ''
                                THEN 0
                                ELSE (LENGTH(wf.raw_patch) - LENGTH(REPLACE(wf.raw_patch, E'\\n', '')))
                           END
                       ), 0)::int AS patch_line_count
                FROM walkthrough.walkthrough_chapters c
                LEFT JOIN walkthrough.walkthrough_files wf ON wf.chapter_id = c.id
                WHERE c.walkthrough_id = :walkthroughId
                GROUP BY c.id
                """, Tuple.class)
                .setParameter("walkthroughId", walkthroughId)
                .getResultList();
    }

    /**
     * Total comment counts grouped by chapter for a walkthrough.
     * Columns: chapter_id, total_comments
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> countCommentsByChapter(UUID walkthroughId) {
        return em.createNativeQuery("""
                SELECT c.id           AS chapter_id,
                       COUNT(cm.id)::int AS total_comments
                FROM walkthrough.walkthrough_chapters c
                LEFT JOIN (
                    SELECT cm.id, COALESCE(cm.chapter_id, wf.chapter_id) AS effective_chapter_id
                    FROM walkthrough.walkthrough_comments cm
                    LEFT JOIN walkthrough.walkthrough_files wf ON wf.id = cm.walkthrough_file_id
                    WHERE cm.walkthrough_id = :walkthroughId
                ) cm ON cm.effective_chapter_id = c.id
                WHERE c.walkthrough_id = :walkthroughId
                GROUP BY c.id
                """, Tuple.class)
                .setParameter("walkthroughId", walkthroughId)
                .getResultList();
    }

    // ── Repo-level (6.4) ──

    /**
     * Repo summary: walkthrough count, review session count, avg completion,
     * avg time-to-complete, avg chapters, active reviewer count.
     *
     * "Review session" = one walkthrough.read_progress row.
     * "active reviewers" = distinct users with at least one walkthrough.read_progress on
     * a walkthrough in the repo within [from, to].
     */
    public Tuple repoSummary(String owner, String repo, Instant from, Instant to) {
        return (Tuple) em.createNativeQuery("""
                WITH wts AS (
                    SELECT w.id, w.user_id, (SELECT COUNT(*) FROM walkthrough.walkthrough_chapters c WHERE c.walkthrough_id = w.id) AS ch_count
                    FROM walkthrough.walkthroughs w
                    WHERE w.owner = :owner AND w.repo = :repo
                      AND w.created_at >= :from AND w.created_at < :to
                ),
                reviews AS (
                    SELECT rp.user_id, rp.walkthrough_id, rp.read_chapters, rp.total_chapters,
                           rp.time_spent_sec, rp.read_at
                    FROM walkthrough.read_progress rp
                    JOIN wts w ON w.id = rp.walkthrough_id
                    WHERE rp.user_id <> w.user_id
                      AND rp.read_at >= :from AND rp.read_at < :to
                )
                SELECT
                    (SELECT COUNT(*) FROM wts)::int                                                         AS total_walkthroughs,
                    (SELECT COUNT(*) FROM reviews)::int                                                     AS total_reviews,
                    COALESCE((SELECT AVG(CASE WHEN total_chapters > 0
                                              THEN read_chapters::float / total_chapters
                                              ELSE 0 END) FROM reviews), 0)                                 AS avg_completion_rate,
                    COALESCE((SELECT AVG(time_spent_sec) FROM reviews), 0)::int                             AS avg_time_to_complete_sec,
                    COALESCE((SELECT AVG(ch_count) FROM wts), 0)                                            AS avg_chapters_per_walkthrough,
                    (SELECT COUNT(DISTINCT user_id) FROM reviews)::int                                      AS active_reviewers
                """, Tuple.class)
                .setParameter("owner", owner)
                .setParameter("repo", repo)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    /**
     * Per-member rollups for a repo over a period.
     * Columns: user_id, username, display_name, avatar_url,
     *          walkthroughs_reviewed, avg_completion_rate, avg_time_spent_sec,
     *          total_comments
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> repoMemberMetrics(String owner, String repo, Instant from, Instant to) {
        return em.createNativeQuery("""
                WITH wts AS (
                    SELECT w.id, w.user_id
                    FROM walkthrough.walkthroughs w
                    WHERE w.owner = :owner AND w.repo = :repo
                ),
                reviews AS (
                    SELECT rp.user_id, rp.walkthrough_id, rp.read_chapters, rp.total_chapters, rp.time_spent_sec
                    FROM walkthrough.read_progress rp
                    JOIN wts w ON w.id = rp.walkthrough_id
                    WHERE rp.user_id <> w.user_id
                      AND rp.read_at >= :from AND rp.read_at < :to
                )
                SELECT u.id                                                                       AS user_id,
                       u.username                                                                 AS username,
                       u.display_name                                                             AS display_name,
                       u.avatar_url                                                               AS avatar_url,
                       COUNT(DISTINCT r.walkthrough_id)::int                                      AS walkthroughs_reviewed,
                       AVG(CASE WHEN r.total_chapters > 0
                                THEN r.read_chapters::float / r.total_chapters ELSE 0 END)        AS avg_completion_rate,
                       AVG(r.time_spent_sec)::int                                                 AS avg_time_spent_sec,
                       (
                           SELECT COUNT(cm.id)
                           FROM walkthrough.walkthrough_comments cm
                           JOIN wts w2 ON w2.id = cm.walkthrough_id
                           WHERE cm.user_id = u.id
                             AND cm.created_at >= :from AND cm.created_at < :to
                       )::int                                                                      AS total_comments
                FROM reviews r
                JOIN walkthrough.users u ON u.id = r.user_id
                GROUP BY u.id, u.username, u.display_name, u.avatar_url
                ORDER BY walkthroughs_reviewed DESC
                """, Tuple.class)
                .setParameter("owner", owner)
                .setParameter("repo", repo)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /**
     * Weekly trend of avg completion rate + completed-review count.
     * Columns: week (e.g. "2025-W14"), avg_completion_rate, reviews_completed
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> repoWeeklyTrend(String owner, String repo, Instant from, Instant to) {
        return em.createNativeQuery("""
                WITH wts AS (
                    SELECT w.id, w.user_id
                    FROM walkthrough.walkthroughs w
                    WHERE w.owner = :owner AND w.repo = :repo
                ),
                reviews AS (
                    SELECT rp.user_id, rp.read_chapters, rp.total_chapters, rp.read_at
                    FROM walkthrough.read_progress rp
                    JOIN wts w ON w.id = rp.walkthrough_id
                    WHERE rp.user_id <> w.user_id
                      AND rp.read_at >= :from AND rp.read_at < :to
                )
                SELECT TO_CHAR(read_at, 'IYYY-"W"IW')                                              AS week,
                       AVG(CASE WHEN total_chapters > 0
                                THEN read_chapters::float / total_chapters ELSE 0 END)             AS avg_completion_rate,
                       COUNT(*)::int                                                               AS reviews_completed
                FROM reviews
                GROUP BY week
                ORDER BY week
                """, Tuple.class)
                .setParameter("owner", owner)
                .setParameter("repo", repo)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public static Instant startOfDayUtc(LocalDate d) {
        return d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
