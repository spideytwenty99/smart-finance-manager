package com.saurabh.financemanager.authentication;

import com.saurabh.financemanager.authentication.dto.AuthResponse;
import com.saurabh.financemanager.authentication.dto.LoginRequest;
import com.saurabh.financemanager.authentication.dto.RegisterRequest;
import com.saurabh.financemanager.category.CategoryService;
import com.saurabh.financemanager.exception.DuplicateResourceException;
import com.saurabh.financemanager.exception.InvalidCredentialsException;
import com.saurabh.financemanager.user.User;
import com.saurabh.financemanager.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class AuthService {



    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CategoryService categoryService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       CategoryService categoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.categoryService=categoryService;

    }
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);

        String encodedPassword = passwordEncoder.encode(request.password());
        user.setPassword(encodedPassword);
        User savedUser = userRepository.save(user);

        //The user is logged in right after registration is successful
        categoryService.createDefaultsFor(savedUser);
        log.info("User registered: {}", savedUser.getId());

        CustomUserDetails details = new CustomUserDetails(savedUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());

        return issueToken(authentication);


    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken((request.email().trim().toLowerCase()), request.password()));

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User logged in: {}", user.getId());

        return issueToken(authentication);
    }

    public AuthResponse issueToken(Authentication authentication) {
        String token = jwtService.generateToken(authentication);
        return AuthResponse.bearer(token, jwtService.getExpirySeconds());

    }
}

