package com.lazar.napo.dto;

import com.lazar.napo.entity.UserAuthProvider;
import com.lazar.napo.entity.UserPlan;
import com.lazar.napo.entity.UserRole;

public record AuthResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        UserPlan plan,
        UserAuthProvider authProvider,
        Boolean emailVerified,
        String accessToken
) {
}