package com.induztek.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para el almacenamiento y recuperación de imágenes (fotos de perfil).
 * Valida formatos JPEG/PNG, sanitiza nombres de archivo y sirve contenido con caché HTTP.
 */
@RestController
@RequestMapping({"/api/imagenes", "/api/usuarios/imagenes"})
@Tag(name = "Imágenes de Perfil", description = "Endpoints para la subida y visualización de avatares y fotos de perfil de usuarios")
public class ImageController {

    private final Path uploadDirectory;
    private final String publicBaseUrl;

    /**
     * Constructor con inyección del directorio de almacenamiento persistente y URL base pública.
     */
    public ImageController(
            @Value("${media.upload-dir:/app/upload}") String uploadDirectory,
            @Value("${media.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    /**
     * Sube y almacena una imagen de perfil.
     * Solo acepta archivos con formato de imagen válido (JPEG o PNG).
     *
     * @param file Archivo multipart enviado por el cliente.
     * @return {@link ImageResponse} con el nombre generado y la URL pública accesible.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Subir foto de perfil", description = "Recibe una imagen multipart (PNG o JPG), valida su contenido y la guarda en el volumen persistente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen subida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío o formato no soportado"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ImageResponse upload(
            @Parameter(description = "Archivo de imagen (JPEG o PNG, máx 10 MB)", required = true)
            @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is empty");
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only valid JPEG or PNG images are accepted");
            }
            String format = file.getContentType() != null && file.getContentType().equalsIgnoreCase("image/png")
                    ? "png" : "jpg";
            String filename = UUID.randomUUID() + "." + format;
            Files.createDirectories(uploadDirectory);
            Path destination = uploadDirectory.resolve(filename).normalize();
            if (!destination.getParent().equals(uploadDirectory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image path");
            }
            if (!ImageIO.write(image, format, destination.toFile())) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported image format");
            }
            return new ImageResponse(filename, publicBaseUrl + "/api/imagenes/" + filename);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store image", exception);
        }
    }

    /**
     * Descarga y sirve una imagen previamente almacenada a partir de su nombre de archivo UUID.
     *
     * @param filename Nombre del archivo con formato UUID.extensión (ej: a8c7b8d0-...-f12a.jpg).
     * @return {@link ResponseEntity} con el recurso de la imagen y encabezados de caché HTTP.
     */
    @GetMapping("/{filename}")
    @Operation(summary = "Obtener imagen por nombre", description = "Retorna el archivo binario de la imagen solicitada (acceso público).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen devuelta exitosamente"),
            @ApiResponse(responseCode = "400", description = "Nombre de archivo inválido"),
            @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    })
    public ResponseEntity<Resource> get(
            @Parameter(description = "Nombre UUID del archivo", required = true)
            @PathVariable String filename) {
        if (!filename.matches("[a-fA-F0-9-]{36}\\.(jpg|png)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image name");
        }
        Path file = uploadDirectory.resolve(filename).normalize();
        if (!file.getParent().equals(uploadDirectory) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
        MediaType contentType = filename.toLowerCase(Locale.ROOT).endsWith(".png")
                ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(contentType)
                .body(new FileSystemResource(file));
    }

    /** DTO de respuesta que contiene el nombre del archivo y su URL de acceso. */
    public record ImageResponse(String filename, String url) {
    }
}
