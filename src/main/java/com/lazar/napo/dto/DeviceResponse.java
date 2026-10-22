package com.lazar.napo.dto;

import com.lazar.napo.entity.DevicePlatform;

import java.time.OffsetDateTime;

public record DeviceResponse(
        Long id,
        DevicePlatform platform,
        String installationId,
        String appVersion,
        String deviceName,
        boolean enabled,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
