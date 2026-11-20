package io.github.kisligi.alerts.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebAppController.class)
class WebAppControllerTest {

    @Autowired
    private MockMvc mvc;

    /** Every route of frontend/src/App.tsx except "/", which Spring Boot serves as the welcome page. */
    @ParameterizedTest
    @ValueSource(strings = {"/notifications", "/admin", "/admin/events", "/admin/deliveries", "/admin/channels"})
    void opensTheWebAppOnItsOwnRoutes(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/unknown", "/admin/events/1", "/anything-else"})
    void leavesOtherPathsAlone(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl(null));
    }
}
