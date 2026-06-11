package com.project.flightOps.service;

import com.project.flightOps.entity.User;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.util.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Attempting to load user details for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Authentication failed: No user found with email: {}", email);
                    return new UsernameNotFoundException("No user found with email: " + email);
                });

        log.info("Successfully loaded user details for email: {}", email);
        return new UserPrincipal(user);
    }
}