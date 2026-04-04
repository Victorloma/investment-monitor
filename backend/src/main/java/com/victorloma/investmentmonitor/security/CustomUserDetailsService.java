package com.victorloma.investmentmonitor.security;

import com.victorloma.investmentmonitor.user.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final AppUserRepository appUserRepository;

  public CustomUserDetailsService(AppUserRepository appUserRepository) {
    this.appUserRepository = appUserRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return appUserRepository
        .findByEmailIgnoreCase(username)
        .map(AuthenticatedUser::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
