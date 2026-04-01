package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import java.util.ArrayList;
import java.util.List;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWalkthroughRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private WalkthroughStatus status;

    @Valid
    private List<ChapterRequest> chapters = new ArrayList<>();
}
