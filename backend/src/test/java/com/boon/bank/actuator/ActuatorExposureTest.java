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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,info",
        "management.endpoints.web.exposure.exclude="
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ActuatorExposureTest {

    @Autowired MockMvc mockMvc;

    @Test
    void metrics_notExposed_returns404EvenForAdmin() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .with(authentication(adminAuth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void info_exposed_returns200() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    private static UsernamePasswordAuthenticationToken adminAuth() {
        User u = User.builder()
                .username("exposure-admin")
                .passwordHash("$2a$10$x")
                .roles(EnumSet.of(UserRole.ADMIN))
                .build();
        AppUserDetails details = new AppUserDetails(u);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
