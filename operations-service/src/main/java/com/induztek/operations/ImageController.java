package com.induztek.operations;

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
 * Controlador REST para el almacenamiento y entrega de evidencias fotográficas de protocolos y pruebas.
 * Guarda archivos en volumen persistente y sirve las fotos con control de caché.
 */
@RestController
@RequestMapping({"/api/imagenes", "/api/protocolos/imagenes", "/api/pruebas/imagenes"})
@Tag(name = "Evidencias Fotográficas", description = "Endpoints para la subida y visualización de imágenes de respaldo de protocolos y pruebas")
public class ImageController {

    private final Path uploadDirectory;
    private final String publicBaseUrl;

    /**
     * Constructor con inyección del directorio de almacenamiento y URL base pública.
     */
    public ImageController(
            @Value("${media.upload-dir:/app/upload}") String uploadDirectory,
            @Value("${media.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    /**
     * Sube una foto de evidencia fotográfica tomada en terreno.
     *
     * @param file Archivo multipart enviado por la aplicación móvil.
     * @return {@link ImageResponse} con el nombre de archivo generado y la URL pública.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Subir foto de evidencia", description = "Almacena una imagen de evidencia fotográfica (JPEG o PNG) tomada durante el mantenimiento o prueba.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen subida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío o formato no soportado"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ImageResponse upload(
            @Parameter(description = "Archivo de imagen", required = true)
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
     * Descarga una imagen de evidencia fotográfica a partir de su nombre de archivo UUID.
     *
     * @param filename Nombre del archivo.
     * @return {@link ResponseEntity} con el archivo binario.
     */
    @GetMapping("/{filename}")
    @Operation(summary = "Obtener foto de evidencia", description = "Descarga y visualiza una imagen de evidencia previamente almacenada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen encontrada"),
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

    /** DTO para la respuesta de subida de imagen. */
    public record ImageResponse(String filename, String url) {
    }
}
