package io.github.kisligi.alerts.channel;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import io.github.kisligi.alerts.channel.ChannelSettingService.ChannelStatus;
import io.github.kisligi.alerts.common.ChannelType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChannelController.class)
class ChannelControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChannelSettingService service;

    @Test
    void listsChannels() throws Exception {
        when(service.list()).thenReturn(List.of(new ChannelStatus(ChannelType.EMAIL, true, true)));

        mvc.perform(get("/api/admin/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EMAIL"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].implemented").value(true));
    }

    @Test
    void switchesChannelOff() throws Exception {
        when(service.setEnabled(ChannelType.SLACK, false)).thenReturn(new ChannelStatus(ChannelType.SLACK, false, true));

        mvc.perform(put("/api/admin/channels/SLACK").contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        verify(service).setEnabled(ChannelType.SLACK, false);
    }

    @Test
    void missingEnabledFlagIsRejected() throws Exception {
        mvc.perform(put("/api/admin/channels/SLACK").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("enabled: must not be null"));
        verifyNoInteractions(service);
    }

    @Test
    void unknownChannelTypeIsRejected() throws Exception {
        mvc.perform(put("/api/admin/channels/PIGEON").contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
