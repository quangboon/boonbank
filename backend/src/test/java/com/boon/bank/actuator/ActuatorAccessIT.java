package com.boon.bank.actuator;

import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,info,metrics",
        "management.endpoints.web.exposure.exclude="
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ActuatorAccessIT {

    @Autowired MockMvc mockMvc;

    @Test
    void health_anonymous_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void health_anonymous_bodyOmitsComponents() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void metrics_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .with(authentication(fakeAuth(UserRole.CUSTOMER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void metrics_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .with(authentication(fakeAuth(UserRole.ADMIN))))
                .andExpect(status().isOk());
    }

    private static UsernamePasswordAuthenticationToken fakeAuth(UserRole role) {
        User u = User.builder()
                .username("actuator-it-" + role.name())
                .passwordHash("$2a$10$x")
                .roles(EnumSet.of(role))
                .build();
        AppUserDetails details = new AppUserDetails(u);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
