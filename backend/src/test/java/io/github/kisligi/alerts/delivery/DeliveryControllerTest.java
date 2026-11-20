package io.github.kisligi.alerts.delivery;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import io.github.kisligi.alerts.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DeliveryRepository deliveries;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void historyOfUnknownUserIs404() throws Exception {
        when(users.existsById(9L)).thenReturn(false);

        mvc.perform(get("/api/users/9/notifications"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User 9 not found"));
        verifyNoInteractions(deliveries);
    }

    @Test
    void historyUsesLimit() throws Exception {
        when(users.existsById(1L)).thenReturn(true);
        when(deliveries.findByUserIdOrderByCreatedAtDescIdDesc(1L, Limit.of(5))).thenReturn(List.of());

        mvc.perform(get("/api/users/1/notifications").param("limit", "5")).andExpect(status().isOk());

        verify(deliveries).findByUserIdOrderByCreatedAtDescIdDesc(1L, Limit.of(5));
    }

    @Test
    void adminLogCanBeFilteredByStatus() throws Exception {
        when(deliveries.findByStatusOrderByCreatedAtDescIdDesc(DeliveryStatus.FAILED, Limit.of(100))).thenReturn(List.of());

        mvc.perform(get("/api/admin/deliveries").param("status", "FAILED")).andExpect(status().isOk());

        verify(deliveries).findByStatusOrderByCreatedAtDescIdDesc(DeliveryStatus.FAILED, Limit.of(100));
    }

    @Test
    void adminLogWithoutFilterReturnsAll() throws Exception {
        when(deliveries.findAllByOrderByCreatedAtDescIdDesc(Limit.of(100))).thenReturn(List.of());

        mvc.perform(get("/api/admin/deliveries")).andExpect(status().isOk());

        verify(deliveries).findAllByOrderByCreatedAtDescIdDesc(Limit.of(100));
    }

    @Test
    void unknownStatusOrOutOfRangeLimitIsRejected() throws Exception {
        mvc.perform(get("/api/admin/deliveries").param("status", "LOST")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/deliveries").param("limit", "501")).andExpect(status().isBadRequest());
        verifyNoInteractions(deliveries);
    }
}
