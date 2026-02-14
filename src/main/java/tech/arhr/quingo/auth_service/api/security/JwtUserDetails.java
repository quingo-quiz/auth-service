package tech.arhr.quingo.auth_service.api.security;

import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tech.arhr.quingo.auth_service.dto.UserDto;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ToString
public class JwtUserDetails implements UserDetails {
    private final UserDto user;
    private Collection<? extends GrantedAuthority> authorities;

    public JwtUserDetails(UserDto user) {
        this.user = user;

        if (user != null && user.getRoles() != null) {
            this.authorities = user.getRoles().stream()
                    .map((role) -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isAccountBlocked();
    }
}
