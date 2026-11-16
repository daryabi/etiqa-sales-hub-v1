package com.etiqa.sales.auth;

import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Messages messages;

    @Transactional
    public UserAccount registerStaffAccount(RegisterRequest request) {
        String username = request.username().trim();
        if (userAccountRepository.existsByUsername(username)) {
            throw new UsernameTakenException(messages.get(MessageKey.USER_USERNAME_TAKEN, username));
        }
        return userAccountRepository.save(UserAccount.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.STAFF)
                .build());
    }
}
