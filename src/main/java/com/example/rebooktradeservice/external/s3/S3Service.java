package com.example.rebooktradeservice.external.s3;

import com.example.rebooktradeservice.common.exception.TradeError;
import com.example.rebooktradeservice.common.exception.TradeException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String upload(MultipartFile file) throws IOException {
        validateFile(file);
        String fileName = "trade/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        String contentType = file.getContentType();
        String region = "ap-northeast-2";
        log.info("Uploading file {}", fileName);
        log.info("Bucket Name: {}", bucketName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(fileName)
            .contentType(contentType)
            .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()
            ));
            log.info("image upload success");
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new TradeException(TradeError.S3_UPLOAD_FAILED);
        }

        String result = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
        if (result.isEmpty()) {
            throw new TradeException(TradeError.S3_UPLOAD_FAILED);
        }
        log.info("result: {}", result);
        return result;
    }

    public void deleteImage(String url) {
        String fileKey = url.substring(url.lastIndexOf("/") + 1);
        log.info("fileKey: {}", fileKey);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(fileKey)
            .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new TradeException(TradeError.INVALID_FILE_TYPE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new TradeException(TradeError.FILE_SIZE_EXCEEDED);
        }
    }
}
