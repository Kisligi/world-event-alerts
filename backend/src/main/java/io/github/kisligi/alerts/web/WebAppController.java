package io.github.kisligi.alerts.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the web app's page for its client-side routes, so that reloading or opening a link
 * such as {@code /admin/events} works when the app is served by this backend (the container
 * image, ADR-034). The built web app lies in {@code static/}; Spring Boot serves
 * {@code index.html} for {@code /} and the JavaScript and CSS files itself.
 *
 * <p>The paths are listed on purpose instead of forwarding everything unknown: an unknown
 * {@code /api/...} path must stay a 404, not turn into an HTML page. They must match the routes
 * in {@code frontend/src/App.tsx}.
 */
@Hidden
@Controller
class WebAppController {

    @GetMapping({"/notifications", "/admin", "/admin/{tab}"})
    String webApp() {
        return "forward:/index.html";
    }
}
