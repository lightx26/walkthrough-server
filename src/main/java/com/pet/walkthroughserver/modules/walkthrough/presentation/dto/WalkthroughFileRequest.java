package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WalkthroughFileRequest {

    @NotBlank
    private String filename;

    @NotBlank
    private String fileSha;

    @NotBlank
    private String fileStatus;

    @Valid
    private List<AnnotationRequest> annotations = new ArrayList<>();
}
