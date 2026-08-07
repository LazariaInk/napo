package com.lazar.napo.dto;

import com.lazar.napo.entity.UserRole;

public record AuthResponse(
        Long id,
        String email,
        String displayName,
        UserRole role
) {
}
