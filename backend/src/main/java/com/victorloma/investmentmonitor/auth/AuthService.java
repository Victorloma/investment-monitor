package com.victorloma.investmentmonitor.auth;

import com.victorloma.investmentmonitor.auth.dto.AuthResponse;
import com.victorloma.investmentmonitor.auth.dto.LoginRequest;
import com.victorloma.investmentmonitor.auth.dto.RegisterRequest;
import com.victorloma.investmentmonitor.role.Role;
import com.victorloma.investmentmonitor.role.RoleRepository;
import com.victorloma.investmentmonitor.security.AuthenticatedUser;
import com.victorloma.investmentmonitor.security.JwtService;
import com.victorloma.investmentmonitor.user.AppUser;
import com.victorloma.investmentmonitor.user.AppUserRepository;
import java.time.Instant;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AppUserRepository appUserRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthService(
      AppUserRepository appUserRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService) {
    this.appUserRepository = appUserRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  public AuthResponse register(RegisterRequest request) {
    if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
      throw new IllegalArgumentException("Email already in use");
    }

    Role defaultRole =
        roleRepository
            .findByName("BASIC_USER")
            .orElseThrow(() -> new IllegalStateException("Default role not configured"));

    AppUser user = new AppUser();
    user.setEmail(request.email().trim().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setRoles(Set.of(defaultRole));

    AppUser savedUser = appUserRepository.save(user);
    AuthenticatedUser authenticatedUser = new AuthenticatedUser(savedUser);

    return new AuthResponse(
        savedUser.getId(),
        savedUser.getEmail(),
        authenticatedUser.getRoles(),
        jwtService.generateToken(authenticatedUser));
  }

  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    AppUser user =
        appUserRepository
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    user.setLastLogin(Instant.now());
    AppUser savedUser = appUserRepository.save(user);
    AuthenticatedUser authenticatedUser = new AuthenticatedUser(savedUser);

    return new AuthResponse(
        savedUser.getId(),
        savedUser.getEmail(),
        authenticatedUser.getRoles(),
        jwtService.generateToken(authenticatedUser));
  }
}
