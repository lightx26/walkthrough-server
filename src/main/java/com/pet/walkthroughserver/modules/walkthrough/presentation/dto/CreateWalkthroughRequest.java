package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateWalkthroughRequest {

    @NotBlank
    private String owner;

    @NotBlank
    private String repo;

    @NotNull
    private Integer prNumber;

    @NotBlank
    private String title;

    private String description;

    @Valid
    private List<ChapterRequest> chapters = new ArrayList<>();
}
