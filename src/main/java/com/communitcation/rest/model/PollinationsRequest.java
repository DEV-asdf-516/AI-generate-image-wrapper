package com.communitcation.rest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
public record PollinationsRequest(
        String model,
        Integer width,
        Integer height,
        Integer seed,
        Boolean enhance
) {
    @Getter
    @AllArgsConstructor
    public enum  Model{
        NANO_BANANA("nanobanana"),
        FLUX("flux"),
        TURBO("turbo");

        private final String code;
    }

}
