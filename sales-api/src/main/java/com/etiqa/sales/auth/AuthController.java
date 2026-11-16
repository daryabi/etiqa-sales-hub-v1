package com.etiqa.sales.auth;

import com.etiqa.sales.common.config.AppProperties;
import com.etiqa.sales.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for sign up and JWT login")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserAccountService userAccountService;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT token")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        return ApiResponse.of("Login successful", issueToken(request.username()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a staff account and return a JWT token for it")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount account = userAccountService.registerStaffAccount(request);
        return ApiResponse.of("Account created", issueToken(account.getUsername()));
    }

    private AuthResponse issueToken(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        return new AuthResponse(
                jwtService.generateToken(userDetails),
                userDetails.getUsername(),
                role,
                appProperties.timezone().getId());
    }
}
