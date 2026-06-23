package com.sima.backend.security;

import com.sima.backend.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Implementación de UserDetails que envuelve la entidad Usuario.
 * Spring Security usa esta clase para manejar la autenticación.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Integer idUsuario;
    private final String correo;
    private final String password;
    private final String rol;
    private final Boolean activo;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.correo = usuario.getCorreo();
        this.password = usuario.getPasswordHash();
        this.rol = usuario.getRol().getNombreRol();
        this.activo = usuario.getActivo();
        // Spring Security usa el prefijo ROLE_ para los roles
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombreRol()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}