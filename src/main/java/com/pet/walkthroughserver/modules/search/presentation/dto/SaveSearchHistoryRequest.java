package com.pet.walkthroughserver.modules.search.presentation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SaveSearchHistoryRequest {
    private String query;
}
