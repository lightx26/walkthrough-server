package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UpdateWalkthroughRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String status;

    @Valid
    private List<ChapterRequest> chapters = new ArrayList<>();
}
