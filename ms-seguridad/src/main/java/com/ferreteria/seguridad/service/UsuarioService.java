package com.ferreteria.seguridad.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.RolDTO;
import com.ferreteria.commons.dto.UsuarioDTO;
import com.ferreteria.entity.Rol;
import com.ferreteria.entity.Usuario;
import com.ferreteria.entity.general.Estado;
import com.ferreteria.seguridad.exception.ApiException;
import com.ferreteria.seguridad.repository.EstadoRepository;
import com.ferreteria.seguridad.repository.RolRepository;
import com.ferreteria.seguridad.repository.UsuarioRepository;
import com.ferreteria.seguridad.security.AccessProperties;
import com.ferreteria.seguridad.security.PasswordPolicy;

/**
 * Alta de usuarios con el contrato de commons ({@link UsuarioDTO}). Mantiene la sencillez del repo de referencia
 * (username + clave BCrypt + roles) pero:
 * <ul>
 *   <li>solo se toman {@code username}, {@code password} y los NOMBRES de los roles: {@code idUsuario}, {@code idEstado}
 *       y los datos del rol que vengan en el body se ignoran (el usuario siempre nace activo);</li>
 *   <li>no crea roles "al vuelo": cada rol debe existir y estar activo;</li>
 *   <li>controla duplicados contra TODA la tabla y valida la contrasena;</li>
 *   <li>la respuesta se arma a mano y nunca incluye la contrasena ni su hash.</li>
 * </ul>
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final int MAX_ROLES = 10;
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._@-]{3,150}$");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstadoRepository estadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessProperties access;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            EstadoRepository estadoRepository, PasswordEncoder passwordEncoder, AccessProperties access) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.estadoRepository = estadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.access = access;
    }

    @Transactional
    public UsuarioDTO crear(UsuarioDTO request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (!USERNAME.matcher(username).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El usuario debe tener entre 3 y 150 caracteres: letras, numeros y . _ @ -");
        }
        PasswordPolicy.validate(request.getPassword(), username);
        if (usuarioRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
        Set<Rol> roles = resolverRoles(request.getRoles());
        Usuario guardado = usuarioRepository.save(
                nuevoUsuario(username, request.getPassword(), roles, estadoActivo()));
        log.info("Usuario '{}' creado con roles {}", guardado.getUsername(),
                guardado.getRoles().stream().map(Rol::getNombre).sorted().toList());
        return toDto(guardado);
    }

    /**
     * Crea el primer ADMIN solo si la tabla de usuarios esta vacia (si no, no hace nada).
     *
     * @return true si lo creo
     */
    @Transactional
    public boolean crearAdminInicial(String username, String rawPassword) {
        if (usuarioRepository.count() > 0) {
            return false;
        }
        String limpio = username.trim();
        if (!USERNAME.matcher(limpio).matches()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_USER invalido: entre 3 y 150 caracteres (letras, numeros . _ @ -)");
        }
        PasswordPolicy.validate(rawPassword, limpio);
        Estado activo = estadoActivo();
        Rol admin = rolRepository.findByNombre(ROL_ADMIN).orElseGet(() -> {
            Rol rol = new Rol();
            rol.setNombre(ROL_ADMIN);
            rol.setEstado(activo);
            return rolRepository.save(rol);
        });
        usuarioRepository.save(nuevoUsuario(limpio, rawPassword, Set.of(admin), activo));
        return true;
    }

    private Usuario nuevoUsuario(String username, String rawPassword, Set<Rol> roles, Estado activo) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuario.setEstado(activo);
        usuario.setRoles(new HashSet<>(roles));
        return usuario;
    }

    /** Mapeo propio (null-safe: roles.id_estado admite NULL) y sin password. */
    private static UsuarioDTO toDto(Usuario usuario) {
        List<RolDTO> roles = usuario.getRoles().stream()
                .sorted(Comparator.comparing(Rol::getNombre, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(rol -> new RolDTO()
                        .idRol(rol.getIdRol())
                        .nombre(rol.getNombre())
                        .descripcion(rol.getDescripcion())
                        .idEstado(rol.getEstado() == null ? null : rol.getEstado().getIdEstado()))
                .toList();
        UsuarioDTO dto = new UsuarioDTO()
                .idUsuario(usuario.getIdUsuario())
                .username(usuario.getUsername())
                .idEstado(usuario.getEstado() == null ? null : usuario.getEstado().getIdEstado());
        dto.setRoles(roles);
        return dto;
    }

    private Estado estadoActivo() {
        return estadoRepository.findById(access.activeEstadoId())
                .orElseThrow(() -> new IllegalStateException("No existe el estado activo (id_estado="
                        + access.activeEstadoId() + ") en la tabla estados; revise ESTADO_ACTIVO_ID"));
    }

    /** Cada nombre debe existir y estar activo; se acepta con o sin prefijo ROLE_. */
    private Set<Rol> resolverRoles(List<RolDTO> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe indicar al menos un rol");
        }
        if (pedidos.size() > MAX_ROLES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puede asignar mas de " + MAX_ROLES + " roles");
        }
        Set<Rol> roles = new LinkedHashSet<>();
        for (RolDTO pedido : pedidos) {
            String nombre = pedido == null || pedido.getNombre() == null ? "" : pedido.getNombre().trim();
            if (nombre.startsWith(ROLE_PREFIX)) {
                nombre = nombre.substring(ROLE_PREFIX.length());
            }
            if (nombre.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cada rol debe tener nombre");
            }
            String buscado = nombre;
            Rol rol = rolRepository.findByNombre(buscado)
                    .or(() -> rolRepository.findByNombre(ROLE_PREFIX + buscado))
                    .filter(this::rolActivo)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Rol inexistente: " + buscado));
            roles.add(rol);
        }
        return roles;
    }

    private boolean rolActivo(Rol rol) {
        return rol.getEstado() == null || Objects.equals(rol.getEstado().getIdEstado(), access.activeEstadoId());
    }
}
