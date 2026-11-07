package com.nationalteammanager.service;

import com.nationalteammanager.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Čuva logotipe takmičenja u folderu uploads/competitions, a u bazu ide samo relativna putanja (npr. competitions/abc.png).
 * Tip se provjerava i po ekstenziji i po sadržaju fajla, a SVG koji sadrži skriptu ili vanjski sadržaj se odbija.
 */
@Service
public class CompetitionLogoStorage {

    static final String FOLDER = "competitions";

    private static final Logger log = LoggerFactory.getLogger(CompetitionLogoStorage.class);
    private static final long MAX_LOGO_BYTES = 5L * 1024 * 1024;
    private static final Pattern UNSAFE_SVG = Pattern.compile(
            "<script|<foreignobject|<iframe|<embed|<object|javascript:|\\bon[a-z]+\\s*=|href\\s*=\\s*[\"']\\s*(?!#)",
            Pattern.CASE_INSENSITIVE);

    private final Path uploadRoot;
    private final Path logoDirectory;

    public CompetitionLogoStorage(@Value("${app.upload-root}") String uploadRoot) {
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
        this.logoDirectory = this.uploadRoot.resolve(FOLDER);
    }

    /** Vraća putanju koja važi nakon izmjene obrasca: novu, postojeću ili null ako je logo uklonjen. */
    public String replace(String currentPath, MultipartFile upload, boolean remove) {
        boolean hasUpload = upload != null && !upload.isEmpty();
        if (!hasUpload && !remove) {
            return currentPath;
        }
        String newPath = hasUpload ? store(upload) : null;
        delete(currentPath);
        return newPath;
    }

    public String store(MultipartFile upload) {
        if (upload.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessRuleException("error.logo.tooLarge");
        }
        try {
            byte[] content = upload.getBytes();
            String extension = verifiedExtension(upload.getOriginalFilename(), content);
            Files.createDirectories(logoDirectory);
            String fileName = UUID.randomUUID() + "." + extension;
            Files.write(logoDirectory.resolve(fileName), content);
            return FOLDER + "/" + fileName;
        } catch (IOException exception) {
            throw new UncheckedIOException("Logo nije moguće sačuvati.", exception);
        }
    }

    public Optional<Resource> load(String logoPath) {
        Path file = uploadRoot.resolve(logoPath).normalize();
        return file.startsWith(logoDirectory) && Files.isRegularFile(file)
                ? Optional.of(new FileSystemResource(file))
                : Optional.empty();
    }

    public MediaType mediaTypeOf(String logoPath) {
        if (logoPath.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        return logoPath.endsWith(".svg") ? MediaType.valueOf("image/svg+xml") : MediaType.IMAGE_JPEG;
    }

    public void delete(String logoPath) {
        if (logoPath == null) {
            return;
        }
        Path file = uploadRoot.resolve(logoPath).normalize();
        if (!file.startsWith(logoDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            log.warn("Logo {} nije moguće obrisati", logoPath, exception);
        }
    }

    /** Ekstenzija mora odgovarati stvarnom sadržaju; .jpg i .jpeg su isti format. */
    private String verifiedExtension(String originalName, byte[] content) {
        String name = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        String detected = detectFormat(content);
        boolean matches = extension.equals(detected) || ("jpeg".equals(extension) && "jpg".equals(detected));
        if (detected == null || !matches) {
            throw new BusinessRuleException("error.logo.format");
        }
        if ("svg".equals(detected) && UNSAFE_SVG.matcher(new String(content, StandardCharsets.UTF_8)).find()) {
            throw new BusinessRuleException("error.logo.unsafeSvg");
        }
        return detected;
    }

    private String detectFormat(byte[] content) {
        if (startsWith(content, 0x89, 'P', 'N', 'G')) {
            return "png";
        }
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        String text = new String(content, 0, Math.min(content.length, 4096), StandardCharsets.UTF_8)
                .replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        boolean svg = (text.startsWith("<svg") || text.startsWith("<?xml") || text.startsWith("<!--")) && text.contains("<svg");
        return svg ? "svg" : null;
    }

    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
