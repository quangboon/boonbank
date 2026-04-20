package com.boon.bank.security.userdetails;

import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.entity.user.User;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.CustomerTypeRepository;
import com.boon.bank.repository.UserRepository;
import com.boon.bank.support.TestcontainersConfiguration;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AppUserDetailsServiceIntegrationTest {

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerTypeRepository customerTypeRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
    }

    @Test
    void loadUserByUsername_customerIdAccessibleAfterTxClose_noLazyInitException() {
        String username = "lazy-customer-" + UUID.randomUUID();

        UUID expectedCustomerId = tx.execute(status -> {
            CustomerType type = customerTypeRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "V1__init.sql must seed at least one customer_types row"));

            Customer customer = Customer.builder()
                    .customerCode("C-" + UUID.randomUUID().toString().substring(0, 8))
                    .fullName("Lazy Init Test")
                    .idNumber("ID-" + UUID.randomUUID().toString().substring(0, 12))
                    .email("lazy-test@example.invalid")
                    .phone("0000000000")
                    .customerType(type)
                    .build();
            customerRepository.saveAndFlush(customer);

            User user = User.builder()
                    .username(username)
                    .passwordHash("$2a$10$placeholder")
                    .customer(customer)
                    .build();
            userRepository.saveAndFlush(user);

            return customer.getId();
        });

        UserDetails loaded = userDetailsService.loadUserByUsername(username);
        assertThat(loaded).isInstanceOf(AppUserDetails.class);
        AppUserDetails principal = (AppUserDetails) loaded;

        // Real assertion the @EntityGraph is doing its job: proxy is initialized on return.
        // Without @EntityGraph, this would be false and getCustomerId() would throw outside a tx.
        assertThat(Hibernate.isInitialized(principal.getCustomer()))
                .as("@EntityGraph on UserRepository.findByUsername must eagerly initialize the customer")
                .isTrue();

        assertThatCode(principal::getCustomerId)
                .as("getCustomerId() must not throw LazyInitializationException after the service tx closes")
                .doesNotThrowAnyException();
        assertThat(principal.getCustomerId()).isEqualTo(expectedCustomerId);
    }

    @Test
    void loadUserByUsername_staffUserWithoutCustomer_getCustomerIdReturnsNull() {
        String username = "staff-" + UUID.randomUUID();

        tx.executeWithoutResult(status -> {
            User user = User.builder()
                    .username(username)
                    .passwordHash("$2a$10$placeholder")
                    .build();
            userRepository.saveAndFlush(user);
        });

        AppUserDetails principal = (AppUserDetails) userDetailsService.loadUserByUsername(username);

        assertThat(principal.getCustomerId()).isNull();
    }
}
