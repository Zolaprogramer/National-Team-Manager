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
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Čuva fotografije igrača i selektora na disku, a u bazi ostaje samo naziv fajla.
 * Naziv generiše aplikacija, a tip se provjerava po sadržaju fajla, a ne po nazivu koji je poslao korisnik.
 */
@Service
public class PhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorageService.class);
    private static final int SIGNATURE_LENGTH = 12;
    private static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024;

    private final Path uploadDirectory;

    public PhotoStorageService(@Value("${app.upload-dir}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile photo) {
        if (photo.getSize() > MAX_PHOTO_BYTES) {
            throw new BusinessRuleException("error.photo.tooLarge");
        }
        try {
            String extension = detectExtension(photo)
                    .orElseThrow(() -> new BusinessRuleException("error.photo.format"));
            Files.createDirectories(uploadDirectory);
            String fileName = UUID.randomUUID() + extension;
            try (var input = photo.getInputStream()) {
                Files.copy(input, uploadDirectory.resolve(fileName));
            }
            return fileName;
        } catch (IOException exception) {
            throw new UncheckedIOException("Fotografiju nije moguće sačuvati.", exception);
        }
    }

    /** Vraća naziv fajla koji važi nakon izmjene obrasca: novi, postojeći ili null ako je fotografija uklonjena. */
    public String replace(String currentFileName, MultipartFile upload, boolean remove) {
        boolean hasUpload = upload != null && !upload.isEmpty();
        if (!hasUpload && !remove) {
            return currentFileName;
        }
        String newFileName = hasUpload ? store(upload) : null;
        delete(currentFileName);
        return newFileName;
    }

    public Optional<Resource> load(String fileName) {
        Path file = uploadDirectory.resolve(fileName).normalize();
        return file.startsWith(uploadDirectory) && Files.isRegularFile(file)
                ? Optional.of(new FileSystemResource(file))
                : Optional.empty();
    }

    public MediaType mediaTypeOf(String fileName) {
        if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        return fileName.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
    }

    public void delete(String fileName) {
        if (fileName == null) {
            return;
        }
        try {
            Files.deleteIfExists(uploadDirectory.resolve(fileName));
        } catch (IOException exception) {
            log.warn("Fotografiju {} nije moguće obrisati", fileName, exception);
        }
    }

    private Optional<String> detectExtension(MultipartFile photo) throws IOException {
        byte[] header;
        try (var input = photo.getInputStream()) {
            header = input.readNBytes(SIGNATURE_LENGTH);
        }
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(".jpg");
        }
        if (startsWith(header, 0x89, 'P', 'N', 'G')) {
            return Optional.of(".png");
        }
        if (header.length == SIGNATURE_LENGTH
                && "RIFF".equals(ascii(header, 0, 4)) && "WEBP".equals(ascii(header, 8, 12))) {
            return Optional.of(".webp");
        }
        return Optional.empty();
    }

    private boolean startsWith(byte[] header, int... signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((header[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String ascii(byte[] bytes, int from, int to) {
        return new String(Arrays.copyOfRange(bytes, from, to), StandardCharsets.US_ASCII);
    }
}
