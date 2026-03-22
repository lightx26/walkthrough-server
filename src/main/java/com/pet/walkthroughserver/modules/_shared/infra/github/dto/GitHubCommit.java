package com.pet.walkthroughserver.modules._shared.infra.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubCommit {

    private String sha;

    private CommitDetail commit;

    @JsonAlias("html_url")
    private String htmlUrl;

    private GitHubUserInfo author;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CommitDetail {
        private String message;

        private CommitAuthor author;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class CommitAuthor {
            private String name;
            private String email;
            private String date;
        }
    }
}
