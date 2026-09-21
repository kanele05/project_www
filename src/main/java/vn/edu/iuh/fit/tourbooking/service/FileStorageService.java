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

/**
 * Lưu và xoá ảnh tải lên.
 *
 * <p>Ảnh nằm ở thư mục cấu hình bởi {@code app.upload.dir}, <b>ngoài</b> classpath:
 * file ghi vào {@code target/classes} sẽ bị xoá sạch mỗi lần {@code mvn clean}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    /** Chỉ nhận đúng bốn đuôi này. Danh sách cho phép, không phải danh sách cấm. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /** Kiểm tra thêm kiểu nội dung để chặn việc đổi tên file .exe thành .jpg. */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final AppProperties appProperties;

    /**
     * Lưu một file và trả về đường dẫn <b>tương đối</b> để ghi vào CSDL,
     * ví dụ {@code tours/3f2a....jpg}.
     *
     * <p><b>Tên file mới luôn là một UUID.</b> Tuyệt đối không ghép
     * {@code getOriginalFilename()} vào đường dẫn: tên đó do trình duyệt gửi lên
     * nên hoàn toàn có thể là {@code ../../application.yml}, và như vậy là ghi đè
     * được file bất kỳ trên máy chủ. Dùng UUID vừa chặn hẳn hướng tấn công đó,
     * vừa tránh hai người tải lên hai ảnh trùng tên thì đè mất của nhau.</p>
     *
     * <p>CSDL lưu đường dẫn tương đối chứ không lưu {@code C:\...}: chỉ cần đổi
     * máy hoặc đổi ổ đĩa là toàn bộ ảnh chết liên kết.</p>
     *
     * @param subDirectory thư mục con, ví dụ {@code tours}
     */
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

        // Luôn dùng dấu / trong đường dẫn lưu vào CSDL, kể cả khi máy chủ chạy
        // Windows: chuỗi này sẽ được ghép thẳng vào URL của trình duyệt.
        return subDirectory + "/" + fileName;
    }

    /**
     * Xoá file <b>sau khi giao dịch đã commit</b>.
     *
     * <p>Xoá ngay lúc gọi là sai: nếu giao dịch bị huỷ sau đó, bản ghi trong CSDL
     * vẫn còn nhưng file ảnh thì đã mất - hỏng dữ liệu mà không có cách nào lấy
     * lại. Hệ thống file không tham gia vào giao dịch của CSDL nên phải tự xếp
     * lịch như thế này. Nếu không có giao dịch nào đang chạy thì xoá luôn.</p>
     */
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

    // ---------------------------------------------------------------------

    private void deleteNow(String relativePath) {
        try {
            Path root = rootDirectory();
            Path target = root.resolve(relativePath).normalize();

            // Chốt chặn thứ hai chống path traversal: dù đường dẫn trong CSDL có
            // bị sửa bằng cách nào đi nữa, cũng không xoá được file nằm ngoài
            // thư mục upload.
            if (!target.startsWith(root)) {
                log.warn("Bỏ qua yêu cầu xoá file nằm ngoài thư mục upload: {}", relativePath);
                return;
            }

            if (Files.deleteIfExists(target)) {
                log.debug("Đã xoá ảnh {}", target);
            }
        } catch (IOException e) {
            // File rác còn sót lại không đáng để làm hỏng cả thao tác nghiệp vụ.
            log.warn("Không xoá được file {}: {}", relativePath, e.getMessage());
        }
    }

    private Path rootDirectory() {
        return Paths.get(appProperties.upload().dir()).toAbsolutePath().normalize();
    }

    /** Lấy đuôi file, đã hạ chữ thường. Trả về chuỗi rỗng nếu không có đuôi. */
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
