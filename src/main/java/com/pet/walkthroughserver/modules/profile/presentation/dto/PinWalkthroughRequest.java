package com.pet.walkthroughserver.modules.profile.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PinWalkthroughRequest {

    @NotNull
    private UUID walkthroughId;
}
