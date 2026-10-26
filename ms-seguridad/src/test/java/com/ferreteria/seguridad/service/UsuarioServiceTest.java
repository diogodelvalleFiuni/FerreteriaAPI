package com.ferreteria.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

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

class UsuarioServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private final EstadoRepository estados = mock(EstadoRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UsuarioService service =
            new UsuarioService(usuarios, roles, estados, encoder, new AccessProperties(1L));

    @BeforeEach
    void setUp() {
        Estado activo = new Estado();
        activo.setIdEstado(1L);
        when(estados.findById(1L)).thenReturn(Optional.of(activo));
        when(encoder.encode(any())).thenReturn("$2a$hash");
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Rol rol(String nombre, Long idEstado) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        if (idEstado != null) {
            Estado estado = new Estado();
            estado.setIdEstado(idEstado);
            rol.setEstado(estado);
        }
        return rol;
    }

    private UsuarioDTO request(String username, String password, String... nombresRol) {
        UsuarioDTO dto = new UsuarioDTO().username(username).password(password);
        for (String nombre : nombresRol) {
            dto.addRolesItem(new RolDTO().nombre(nombre));
        }
        return dto;
    }

    @Test
    void creaElUsuarioActivoConClaveHasheadaYSinDevolverLaClave() {
        when(roles.findByNombre("VENDEDOR")).thenReturn(Optional.of(rol("VENDEDOR", 1L)));

        UsuarioDTO resp = service.crear(request("juan", "una-frase-larga-123", "VENDEDOR"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("juan", guardado.getUsername());
        assertEquals("$2a$hash", guardado.getPassword());
        assertEquals(1L, guardado.getEstado().getIdEstado());
        assertEquals("juan", resp.getUsername());
        assertNull(resp.getPassword());
        assertEquals(List.of("VENDEDOR"), resp.getRoles().stream().map(RolDTO::getNombre).toList());
    }

    @Test
    void ignoraElEstadoYElIdQueVenganEnElBody() {
        when(roles.findByNombre("VENDEDOR")).thenReturn(Optional.of(rol("VENDEDOR", null)));
        UsuarioDTO dto = request("juan", "una-frase-larga-123", "VENDEDOR").idUsuario(99).idEstado(2L);

        service.crear(dto);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        assertEquals(1L, captor.getValue().getEstado().getIdEstado());
        assertNull(captor.getValue().getIdUsuario());
    }

    @Test
    void usuarioDuplicadoDa409() {
        when(usuarios.existsByUsername("juan")).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(request("juan", "una-frase-larga-123", "VENDEDOR")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(usuarios, never()).save(any());
    }

    @Test
    void rolInexistenteOInactivoDa400YNoSeCreaNada() {
        when(roles.findByNombre("FANTASMA")).thenReturn(Optional.empty());
        when(roles.findByNombre("VIEJO")).thenReturn(Optional.of(rol("VIEJO", 2L)));

        for (String nombre : List.of("FANTASMA", "VIEJO")) {
            ApiException ex = assertThrows(ApiException.class,
                    () -> service.crear(request("juan", "una-frase-larga-123", nombre)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }
        verify(usuarios, never()).save(any());
    }

    @Test
    void sinRolesDa400() {
        ApiException ex = assertThrows(ApiException.class, () -> service.crear(request("juan", "una-frase-larga-123")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void usernameConCaracteresRarosOClaveDebilDa400() {
        assertThrows(ApiException.class, () -> service.crear(request("ju an;", "una-frase-larga-123", "VENDEDOR")));
        assertThrows(ApiException.class, () -> service.crear(request("juan", "corta", "VENDEDOR")));
    }

    @Test
    void adminInicialSoloSeCreaConLaTablaVacia() {
        when(usuarios.count()).thenReturn(1L);
        assertFalse(service.crearAdminInicial("admin", "una-frase-larga-123"));
        verify(usuarios, never()).save(any());

        when(usuarios.count()).thenReturn(0L);
        when(roles.findByNombre("ADMIN")).thenReturn(Optional.of(rol("ADMIN", 1L)));
        assertTrue(service.crearAdminInicial("admin", "una-frase-larga-123"));
        verify(usuarios).save(any(Usuario.class));
    }
}
