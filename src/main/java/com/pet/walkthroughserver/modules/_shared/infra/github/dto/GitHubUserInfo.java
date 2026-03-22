package com.pet.walkthroughserver.modules._shared.infra.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubUserInfo {

    private Long id;

    private String login;

    private String name;

    private String email;

    @JsonAlias("avatar_url")
    private String avatarUrl;
}
