package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
// Lưu và xoá ảnh tải lên; thư mục nằm ngoài classpath nên "mvn clean" không xoá mất.
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final AppProperties appProperties;

    // Kiểm định dạng/loại nội dung rồi ghi file với tên ngẫu nhiên, trả về đường dẫn tương đối.
    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("error.upload.empty");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessRuleException("error.upload.badExtension",
                    String.join(", ", ALLOWED_EXTENSIONS));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("error.upload.badContentType");
        }

        String fileName = UUID.randomUUID() + "." + extension;
        Path directory = rootDirectory().resolve(subDirectory);

        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(fileName);
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.debug("Đã lưu ảnh {}", target);

        } catch (IOException e) {
            throw new UncheckedIOException("Không ghi được file tải lên", e);
        }

        return subDirectory + "/" + fileName;
    }

    // Chỉ xoá file thật sau khi transaction đã commit thành công (tránh xoá nhầm khi rollback).
    public void deleteAfterCommit(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteNow(relativePath);
                }
            });
        } else {
            deleteNow(relativePath);
        }
    }

    // Xoá file thật, chặn đường dẫn cố tình thoát ra ngoài thư mục upload.
    private void deleteNow(String relativePath) {
        try {
            Path root = rootDirectory();
            Path target = root.resolve(relativePath).normalize();

            if (!target.startsWith(root)) {
                log.warn("Bỏ qua yêu cầu xoá file nằm ngoài thư mục upload: {}", relativePath);
                return;
            }

            if (Files.deleteIfExists(target)) {
                log.debug("Đã xoá ảnh {}", target);
            }
        } catch (IOException e) {

            log.warn("Không xoá được file {}: {}", relativePath, e.getMessage());
        }
    }

    private Path rootDirectory() {
        return Paths.get(appProperties.upload().dir()).toAbsolutePath().normalize();
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
