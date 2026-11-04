package com.matriz.api;

import com.matriz.domain.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler logoutHandler;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SecurityContextLogoutHandler logoutHandler) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.logoutHandler = logoutHandler;
    }

    @GetMapping("/csrf")
    public ApiModels.CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new ApiModels.CsrfTokenResponse(csrfToken.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody ApiModels.LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim(),
                            request.password()));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiModels.ErrorResponse("INVALID_CREDENTIALS", "Email or password is incorrect."));
        }

        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return ResponseEntity.ok(toUserResponse(authentication));
    }

    @GetMapping("/me")
    public ApiModels.AuthUserResponse profile(Authentication authentication) {
        return toUserResponse(authentication);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        logoutHandler.logout(servletRequest, servletResponse, authentication);
        return ResponseEntity.noContent().build();
    }

    private ApiModels.AuthUserResponse toUserResponse(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AppUser user)) {
            throw new AuthenticationServiceException("Authenticated principal is not a Matriz user.");
        }
        return new ApiModels.AuthUserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
