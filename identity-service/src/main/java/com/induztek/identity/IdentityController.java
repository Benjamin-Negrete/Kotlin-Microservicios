package com.induztek.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el microservicio de Identidad.
 * Expone endpoints públicos de registro, login y recuperación de contraseña,
 * así como endpoints protegidos para consulta/edición de perfil y administración de usuarios.
 */
@RestController
@Tag(name = "Identidad y Usuarios", description = "Endpoints para registro, autenticación JWT, recuperación de claves y administración de cuentas")
public class IdentityController {

    private final IdentityService identity;

    /**
     * Constructor con inyección del servicio de identidad.
     *
     * @param identity Lógica de negocio de usuarios y autenticación.
     */
    public IdentityController(IdentityService identity) {
        this.identity = identity;
    }

    /**
     * Registra una nueva cuenta de usuario en el sistema.
     *
     * @param request Datos de registro (nombre, email, contraseña, empresa, rol solicitado).
     * @return {@link IdentityService.UserResponse} con los datos del usuario registrado.
     */
    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar nuevo usuario", description = "Crea una cuenta en el sistema. Si se exige aprobación previa, la cuenta quedará inactiva hasta que un ADMIN la habilite.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o términos no aceptados"),
            @ApiResponse(responseCode = "409", description = "El correo ya se encuentra registrado")
    })
    public IdentityService.UserResponse register(@Valid @RequestBody IdentityService.RegisterRequest request) {
        return identity.register(request);
    }

    /**
     * Inicia sesión con correo electrónico y contraseña.
     * Retorna un token JWT firmado y los datos del perfil si las credenciales son correctas y la cuenta está activa.
     *
     * @param request Credenciales de acceso.
     * @return {@link IdentityService.LoginResponse} con el JWT y datos del usuario.
     */
    @PostMapping("/api/auth/login")
    @Operation(summary = "Iniciar sesión", description = "Valida credenciales y genera un token JWT firmado (HS256) con vigencia configurada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas"),
            @ApiResponse(responseCode = "403", description = "Cuenta inactiva o pendiente de aprobación")
    })
    public IdentityService.LoginResponse login(@Valid @RequestBody IdentityService.LoginRequest request) {
        return identity.login(request);
    }

    /**
     * Solicita un código de 6 dígitos para recuperación de contraseña por correo electrónico.
     *
     * @param request Email de la cuenta a recuperar.
     * @return Mensaje informativo.
     */
    @PostMapping("/api/auth/password/recovery")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicitar código de recuperación", description = "Genera un código temporal y lo envía al correo registrado mediante el servicio de notificaciones.")
    @ApiResponse(responseCode = "202", description = "Solicitud procesada (si la cuenta existe, el código fue enviado)")
    public Message requestRecovery(@Valid @RequestBody IdentityService.RecoveryRequest request) {
        identity.requestPasswordReset(request.email());
        return new Message("If the account is active, a recovery code has been sent.");
    }

    /**
     * Restablece la contraseña de un usuario mediante el código de recuperación previamente enviado.
     *
     * @param request Datos con email, código de 6 dígitos y nueva contraseña.
     * @return Mensaje de confirmación.
     */
    @PostMapping("/api/auth/password/reset")
    @Operation(summary = "Restablecer contraseña", description = "Verifica el código de recuperación y asigna la nueva contraseña hasheada con BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Código inválido o expirado")
    })
    public Message resetPassword(@Valid @RequestBody IdentityService.ResetPasswordRequest request) {
        identity.resetPassword(request);
        return new Message("Password updated.");
    }

    /**
     * Valida si un código de recuperación es correcto antes de solicitar la nueva contraseña.
     *
     * @param request Email y código a comprobar.
     */
    @PostMapping("/api/auth/password/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Verificar código de recuperación", description = "Comprueba si el código ingresado coincide y no ha expirado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Código válido"),
            @ApiResponse(responseCode = "400", description = "Código incorrecto o vencido")
    })
    public void verifyRecoveryCode(@Valid @RequestBody IdentityService.VerifyResetCodeRequest request) {
        identity.verifyPasswordResetCode(request.email(), request.code());
    }

    /**
     * Obtiene el perfil completo del usuario autenticado en la sesión actual.
     *
     * @param jwt Token JWT del usuario decodificado.
     * @return {@link IdentityService.UserResponse} del usuario logueado.
     */
    @GetMapping("/api/usuarios/me")
    @Operation(summary = "Consultar mi perfil", description = "Retorna los datos del usuario identificado por el subject del token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public IdentityService.UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return identity.getCurrentUser(jwt.getSubject());
    }

    /**
     * Actualiza información del perfil propio (nombre, email, biografía, foto).
     *
     * @param jwt Token JWT autenticado.
     * @param request Campos a modificar.
     * @return {@link IdentityService.UserResponse} con los datos actualizados.
     */
    @PatchMapping("/api/usuarios/me/perfil")
    @Operation(summary = "Actualizar mi perfil", description = "Permite modificar datos personales como nombre, email, foto y biografía.")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    public IdentityService.UserResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody IdentityService.ProfileUpdateRequest request) {
        return identity.updateProfile(jwt.getSubject(), request);
    }

    /**
     * Lista todos los usuarios registrados en el sistema.
     * Operación restringida exclusivamente a usuarios con rol ADMIN.
     *
     * @return Lista de usuarios del sistema.
     */
    @GetMapping("/api/usuarios")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Listar todos los usuarios", description = "Retorna la lista completa de usuarios registrados. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public List<IdentityService.UserResponse> users() {
        return identity.listUsers();
    }

    /**
     * Habilita o deshabilita la cuenta de un usuario (aprobación de cuenta).
     * Requiere rol ADMIN.
     *
     * @param id ID del usuario a modificar.
     * @param request Estado activo true/false.
     * @return {@link IdentityService.UserResponse} actualizado.
     */
    @PatchMapping("/api/usuarios/{id}/estado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Aprobar o deshabilitar usuario", description = "Modifica el estado activo de un usuario (aprobación de registro). Requiere rol ADMIN.")
    @ApiResponse(responseCode = "200", description = "Estado de usuario actualizado")
    public IdentityService.UserResponse setUserState(
            @Parameter(description = "ID del usuario", required = true) @PathVariable String id,
            @Valid @RequestBody ActiveRequest request) {
        return identity.setActive(id, request.active());
    }

    /**
     * Elimina permanentemente una cuenta de usuario.
     * Requiere rol ADMIN.
     *
     * @param id ID del usuario a eliminar.
     */
    @DeleteMapping("/api/usuarios/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar usuario", description = "Elimina permanentemente la cuenta de un usuario. Requiere rol ADMIN.")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente")
    public void deleteUser(
            @Parameter(description = "ID del usuario a eliminar", required = true) @PathVariable String id) {
        identity.deleteUser(id);
    }

    /**
     * Modifica el rol o perfil asignado a un usuario.
     * Requiere rol ADMIN.
     *
     * @param id ID del usuario.
     * @param request Código del nuevo rol (ej: FIELD_TECHNICIAN, TEST_ENGINEER).
     * @return {@link IdentityService.UserResponse} con el nuevo rol asignado.
     */
    @PatchMapping("/api/usuarios/{id}/rol")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Cambiar rol de usuario", description = "Asigna un rol diferente a un usuario existente. Requiere rol ADMIN.")
    @ApiResponse(responseCode = "200", description = "Rol modificado exitosamente")
    public IdentityService.UserResponse setUserRole(
            @Parameter(description = "ID del usuario", required = true) @PathVariable String id,
            @Valid @RequestBody RoleRequest request) {
        return identity.setRole(id, request.roleCode());
    }

    /** DTO para habilitación/deshabilitación de cuenta. */
    public record ActiveRequest(@NotNull Boolean active) {
    }

    /** DTO para asignación de rol. */
    public record RoleRequest(@jakarta.validation.constraints.NotBlank String roleCode) {
    }

    /** DTO simple para mensajes informativos. */
    public record Message(String message) {
    }
}
