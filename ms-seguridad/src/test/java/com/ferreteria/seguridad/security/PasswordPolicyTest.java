package com.ferreteria.seguridad.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.ferreteria.seguridad.exception.ApiException;

class PasswordPolicyTest {

    @Test
    void aceptaUnaContrasenaLargaQueNoContieneElUsuario() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("una-frase-larga-y-facil", "juan"));
    }

    @Test
    void rechazaContrasenasCortasNulasOEnBlanco() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("corta123", "juan"));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate(null, "juan"));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("            ", "juan"));
    }

    @Test
    void rechazaLasQueSuperanLos72BytesDeBcrypt() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("a".repeat(73), "juan"));
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(72), "juan"));
        // 40 caracteres de 2 bytes = 80 bytes
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("ñ".repeat(40), "juan"));
    }

    @Test
    void rechazaLasQueContienenElNombreDeUsuario() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("Mi-JUAN-secreto-123", "juan"));
    }
}
