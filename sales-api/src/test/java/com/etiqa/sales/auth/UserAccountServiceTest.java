package com.etiqa.sales.auth;

import com.etiqa.sales.support.TestMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserAccountService userAccountService;

    @BeforeEach
    void setUp() {
        userAccountService = new UserAccountService(userAccountRepository, passwordEncoder, TestMessages.create());
    }

    @Test
    void register_createsStaffWithHashedPassword() {
        when(userAccountRepository.existsByUsername("new.cashier")).thenReturn(false);
        when(passwordEncoder.encode("secret12")).thenReturn("hashed-secret");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount account = userAccountService.registerStaffAccount(new RegisterRequest("  new.cashier ", "secret12"));

        assertThat(account.getUsername()).isEqualTo("new.cashier");
        assertThat(account.getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(account.getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    void register_rejectsTakenUsername() {
        when(userAccountRepository.existsByUsername("staff")).thenReturn(true);

        assertThatThrownBy(() -> userAccountService.registerStaffAccount(new RegisterRequest("staff", "secret12")))
                .isInstanceOf(UsernameTakenException.class)
                .hasMessage("Username 'staff' is already taken.");

        verify(userAccountRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
