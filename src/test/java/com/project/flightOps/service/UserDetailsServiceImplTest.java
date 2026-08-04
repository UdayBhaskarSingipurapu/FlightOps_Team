package com.project.flightOps.service;

import com.project.flightOps.entity.User;
import com.project.flightOps.enums.Role;
import com.project.flightOps.enums.UserStatus;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.util.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User mockUser;
    private final String email = "coordinator@airport.com";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId("USR-1");
        mockUser.setName("Jane Coordinator");
        mockUser.setPassword("encoded-password");
        mockUser.setRole(Role.AirlineCoordinator);
        mockUser.setEmail(email);
        mockUser.setPhone("555-1234");
        mockUser.setAirportId("JFK");
        mockUser.setStatus(UserStatus.Active);
    }

    @Test
    void loadUserByUsername_Success_ReturnsUserPrincipalWithMappedFields() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertNotNull(result);
        assertInstanceOf(UserPrincipal.class, result);
        assertEquals(email, result.getUsername());
        assertEquals("encoded-password", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isCredentialsNonExpired());

        UserPrincipal principal = (UserPrincipal) result;
        assertEquals("USR-1", principal.getUserId());
        assertEquals(email, principal.getEmail());
        assertEquals(Role.AirlineCoordinator, principal.getRole());

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_Success_AuthoritiesContainRolePrefixedName() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertEquals(1, result.getAuthorities().size());
        GrantedAuthority authority = result.getAuthorities().iterator().next();
        assertEquals("ROLE_AirlineCoordinator", authority.getAuthority());
    }

    @Test
    void loadUserByUsername_InactiveUser_IsDisabled() {
        mockUser.setStatus(UserStatus.Inactive);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertFalse(result.isEnabled());
    }

}
