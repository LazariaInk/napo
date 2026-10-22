package com.lazar.napo.dto;

import com.lazar.napo.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotNull(message = "Platform is required")
        DevicePlatform platform,

        @NotBlank(message = "Push token is required")
        @Size(max = 4096, message = "Push token must have at most 4096 characters")
        String pushToken,

        @Size(max = 30, message = "App version must have at most 30 characters")
        String appVersion,

        @Size(max = 120, message = "Device name must have at most 120 characters")
        String deviceName
) {
}
