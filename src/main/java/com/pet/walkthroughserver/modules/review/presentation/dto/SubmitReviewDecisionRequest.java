package com.pet.walkthroughserver.modules.review.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitReviewDecisionRequest {

    @NotBlank
    private String decision;

    private String comment;
}
