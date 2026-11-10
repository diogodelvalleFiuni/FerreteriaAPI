package com.ferreteria.seguridad.security;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.entity.Rol;
import com.ferreteria.entity.Usuario;
import com.ferreteria.seguridad.repository.UsuarioRepository;

/**
 * Convierte el Usuario de commons (tabla usuarios) en UserDetails. No usa id_persona.
 * <ul>
 *   <li>Estado: {@code enabled} solo si el id_estado del usuario es {@code app.security.access.active-estado-id};
 *       en otro caso Spring Security lanza DisabledException al autenticar.</li>
 *   <li>Roles: cada Rol.nombre -> {@code ROLE_<nombre>}. Un rol con estado distinto del activo se ignora
 *       (un rol sin estado cuenta como activo).</li>
 * </ul>
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDetailsService.class);
    private static final String ROLE_PREFIX = "ROLE_";

    private final UsuarioRepository usuarioRepository;
    private final AccessProperties access;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository, AccessProperties access) {
        this.usuarioRepository = usuarioRepository;
        this.access = access;
    }

    /** Transaccional: estado y roles son LAZY y open-in-view esta desactivado. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente"));

        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            log.warn("Usuario '{}' sin password almacenado", username);
            throw new UsernameNotFoundException("Usuario sin credenciales");
        }

        boolean activo = usuario.getEstado() != null
                && Objects.equals(usuario.getEstado().getIdEstado(), access.activeEstadoId());
        if (!activo) {
            log.warn("Login rechazado: usuario '{}' con id_estado no habilitado", username);
        }

        List<GrantedAuthority> authorities = usuario.getRoles().stream()
                .filter(this::rolActivo)
                .map(Rol::getNombre)
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .map(n -> n.startsWith(ROLE_PREFIX) ? n : ROLE_PREFIX + n)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(authorities)
                .disabled(!activo)
                .build();
    }

    private boolean rolActivo(Rol rol) {
        return rol.getEstado() == null || Objects.equals(rol.getEstado().getIdEstado(), access.activeEstadoId());
    }
}
