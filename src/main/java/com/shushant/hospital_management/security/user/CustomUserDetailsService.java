package com.shushant.hospital_management.security.user;

import com.shushant.hospital_management.common.exception.ResourceNotFoundException;
import com.shushant.hospital_management.modules.auth.entity.AppUser;
import com.shushant.hospital_management.modules.auth.entity.Role;
import com.shushant.hospital_management.modules.auth.repository.AppUserRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = appUserRepository.findByEmailIgnoreCaseAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(String userId) {
        AppUser user = appUserRepository.findById(UUID.fromString(userId))
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toPrincipal(user);
    }

    private AuthenticatedUser toPrincipal(AppUser user) {
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> toAuthorities(role).stream())
                .collect(Collectors.toSet());

        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                user.isAccountNonLocked(),
                authorities);
    }

    private Set<SimpleGrantedAuthority> toAuthorities(Role role) {
        Set<SimpleGrantedAuthority> roleAuthorities = role.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toSet());
        roleAuthorities.add(new SimpleGrantedAuthority(role.getName().name()));
        return roleAuthorities;
    }
}
