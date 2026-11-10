package com.ferreteria.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ferreteria.entity.general.Estado;
import com.ferreteria.entity.Rol;
import com.ferreteria.entity.Usuario;
import com.ferreteria.seguridad.repository.UsuarioRepository;
import com.ferreteria.seguridad.security.AccessProperties;
import com.ferreteria.seguridad.security.UsuarioDetailsService;

class UsuarioDetailsServiceTest {

    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final UsuarioDetailsService service = new UsuarioDetailsService(repo, new AccessProperties(1L));

    private Usuario usuario(long idEstado, String... roles) {
        Estado estado = new Estado();
        estado.setIdEstado(idEstado);
        Usuario u = new Usuario();
        u.setUsername("juan");
        u.setPassword("$2a$10$hash");
        u.setEstado(estado);
        for (String r : roles) {
            Rol rol = new Rol();
            rol.setNombre(r);
            u.getRoles().add(rol);
        }
        return u;
    }

    @Test
    void usuarioActivoMapeaRolesConPrefijo() {
        when(repo.findByUsername("juan")).thenReturn(Optional.of(usuario(1, "ADMIN", "ROLE_VENDEDOR")));
        UserDetails d = service.loadUserByUsername("juan");
        assertTrue(d.isEnabled());
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_VENDEDOR"),
                d.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
    }

    @Test
    void usuarioConEstadoNoHabilitadoQuedaDeshabilitado() {
        when(repo.findByUsername("juan")).thenReturn(Optional.of(usuario(2, "ADMIN")));
        assertFalse(service.loadUserByUsername("juan").isEnabled());
    }

    @Test
    void usuarioInexistenteLanzaUsernameNotFound() {
        when(repo.findByUsername("nadie")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nadie"));
    }

    @Test
    void rolConEstadoNoActivoNoOtorgaAutoridad() {
        Usuario u = usuario(1, "ADMIN");
        Estado inactivo = new Estado();
        inactivo.setIdEstado(2L);
        Rol viejo = new Rol();
        viejo.setNombre("VIEJO");
        viejo.setEstado(inactivo);
        u.getRoles().add(viejo);
        when(repo.findByUsername("juan")).thenReturn(Optional.of(u));

        UserDetails d = service.loadUserByUsername("juan");

        assertEquals(Set.of("ROLE_ADMIN"),
                d.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
    }
}
