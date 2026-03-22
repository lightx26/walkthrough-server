package com.pet.walkthroughserver.modules.github.presentation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileChangeResponse {

    private String sha;
    private String filename;
    private String status;
    private int additions;
    private int deletions;
    private int changes;
    private String patch;
    private String previousFilename;
}
