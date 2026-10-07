package com.fitgear.backend.auth;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        UserRole role
) {
    public static ProfileResponse from(AppUser user) {
        return new ProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
