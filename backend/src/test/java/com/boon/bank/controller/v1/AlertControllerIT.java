package com.boon.bank.controller.v1;

import com.boon.bank.entity.enums.AlertSeverity;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.fraud.Alert;
import com.boon.bank.entity.user.User;
import com.boon.bank.repository.AlertRepository;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AlertControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired AlertRepository alertRepository;
    @Autowired PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    private final List<UUID> seededAlertIds = new ArrayList<>();
    private String msgPrefix;

    @BeforeEach
    void seed() {
        tx = new TransactionTemplate(txManager);
        msgPrefix = "IT-" + UUID.randomUUID().toString().substring(0, 8) + "-";
        seededAlertIds.clear();

        tx.executeWithoutResult(s -> {
            // Fixture: 5 alerts spanning severity × resolved. Filter
            // severity=HIGH&resolved=false must match exactly ONE row among this run's
            // seeded data — foreign rows from parallel IT classes cannot pollute the
            // assertion because they wouldn't carry MSG_PREFIX.
            seededAlertIds.add(persist("R1", AlertSeverity.HIGH,     msgPrefix + "hi-open",   false));
            seededAlertIds.add(persist("R2", AlertSeverity.HIGH,     msgPrefix + "hi-closed", true));
            seededAlertIds.add(persist("R3", AlertSeverity.MEDIUM,   msgPrefix + "med-open",  false));
            seededAlertIds.add(persist("R4", AlertSeverity.LOW,      msgPrefix + "lo-open",   false));
            seededAlertIds.add(persist("R5", AlertSeverity.CRITICAL, msgPrefix + "crit-open", false));
        });
    }

    @AfterEach
    void cleanup() {
        tx.executeWithoutResult(s -> alertRepository.deleteAllById(seededAlertIds));
    }

    @Test
    void search_filtersBySeverityAndResolved() throws Exception {
        mockMvc.perform(get("/api/v1/alerts")
                        .param("severity", "HIGH")
                        .param("resolved", "false")
                        .param("size", "200")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                // Among this run's rows there is exactly one HIGH+unresolved (R1).
                // Foreign HIGH+unresolved rows from sibling IT classes would also match;
                // filter on our MSG_PREFIX to keep the assertion strict.
                .andExpect(jsonPath(
                        "$.data.content[?(@.message=='" + msgPrefix + "hi-open')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data.content[?(@.message=='" + msgPrefix + "hi-closed')].length()").value(0));
    }

    @Test
    void search_noFilters_returnsAtLeastOurFiveSeededRows() throws Exception {
        // totalElements is a GLOBAL count — other IT classes may leak rows into the
        // shared context, so assert a lower bound + that our 5 rule codes are all
        // present among the top-of-page (order by createdAt DESC, fresh seed is newest).
        mockMvc.perform(get("/api/v1/alerts")
                        .param("size", "200")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
                .andExpect(jsonPath(
                        "$.data.content[?(@.message=='" + msgPrefix + "hi-open')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data.content[?(@.message=='" + msgPrefix + "hi-closed')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data.content[?(@.message=='" + msgPrefix + "med-open')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data.content[?(@.message=='" + msgPrefix + "lo-open')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data.content[?(@.message=='" + msgPrefix + "crit-open')].length()").value(1));
    }

    @Test
    void search_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/alerts")
                        .with(authentication(auth(UserRole.CUSTOMER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void openEndpoint_backwardCompat_returnsDtosAndExcludesResolved() throws Exception {
        // Fixture has 4 unresolved among this run's rows: R1 (hi-open), R3 (med-open),
        // R4 (lo-open), R5 (crit-open). R2 (hi-closed) is resolved. Using MSG_PREFIX
        // filters out sibling leftovers so the count of OUR unresolved rows is exact.
        mockMvc.perform(get("/api/v1/alerts/open")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.message=='" + msgPrefix + "hi-open')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data[?(@.message=='" + msgPrefix + "med-open')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data[?(@.message=='" + msgPrefix + "lo-open')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data[?(@.message=='" + msgPrefix + "crit-open')].length()").value(1))
                .andExpect(jsonPath(
                        "$.data[?(@.message=='" + msgPrefix + "hi-closed')].length()").value(0));
    }

    private UUID persist(String ruleCode, AlertSeverity severity, String message, boolean resolved) {
        return alertRepository.save(Alert.builder()
                .ruleCode(ruleCode)
                .severity(severity)
                .message(message)
                .resolved(resolved)
                .build()).getId();
    }

    private static UsernamePasswordAuthenticationToken adminAuth() {
        return auth(UserRole.ADMIN);
    }

    private static UsernamePasswordAuthenticationToken auth(UserRole role) {
        User u = User.builder()
                .username("alert-it-" + role.name())
                .passwordHash("$2a$10$x")
                .roles(EnumSet.of(role))
                .build();
        AppUserDetails d = new AppUserDetails(u);
        return new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities());
    }
}
