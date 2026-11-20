package io.github.kisligi.alerts.user;

/** A user as returned by the API. */
public record UserResponse(Long id, String name, String email) {

    static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
