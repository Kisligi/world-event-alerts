package io.github.kisligi.alerts.user;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Seeded demo users (no login in this prototype)")
public class UserController {

    private final AppUserRepository users;

    public UserController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Used by the UI's user switcher.")
    public List<UserResponse> list() {
        return users.findAllByOrderByIdAsc().stream().map(UserResponse::from).toList();
    }
}
