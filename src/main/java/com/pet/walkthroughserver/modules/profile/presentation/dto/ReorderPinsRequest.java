package com.pet.walkthroughserver.modules.profile.presentation.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReorderPinsRequest {

    @NotEmpty
    private List<UUID> pinIds;
}
