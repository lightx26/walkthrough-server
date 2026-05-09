package com.pet.walkthroughserver.configs;

public final class CacheNames {

    private CacheNames() {}

    // GitHub API caches
    public static final String GITHUB_REPOS = "github:repos";
    public static final String GITHUB_REPO_SEARCH = "github:repo-search";
    public static final String GITHUB_REPO = "github:repo";
    public static final String GITHUB_PULLS = "github:pulls";
    public static final String GITHUB_PULL = "github:pull";
    public static final String GITHUB_PR_FILES = "github:pr-files";
    public static final String GITHUB_PR_COMMITS = "github:pr-commits";
    public static final String GITHUB_RECENT_PULLS = "github:recent-pulls";
    public static final String GITHUB_PR_SEARCH = "github:pr-search";

    // Walkthrough caches
    public static final String WALKTHROUGH_DETAIL = "walkthrough:detail";
    public static final String WALKTHROUGH_RECENT = "walkthrough:recent";
    public static final String WALKTHROUGH_COUNT_REPO = "walkthrough:count-repo";
    public static final String WALKTHROUGH_COUNT_REPOS = "walkthrough:count-repos";
    public static final String WALKTHROUGH_COUNT_PR = "walkthrough:count-pr";
    public static final String WALKTHROUGH_COUNT_PRS = "walkthrough:count-prs";
    public static final String WALKTHROUGH_COMMENT_COUNTS = "walkthrough:comment-counts";
    public static final String WALKTHROUGH_PROGRESS = "walkthrough:progress";

    // Profile caches
    public static final String PROFILE_STATS = "profile:stats";

    // Pinned repos caches
    public static final String PINNED_LIST = "pinned:list";
    public static final String PINNED_CHECK = "pinned:check";

    // User caches
    public static final String USER_SEARCH = "user:search";
}
