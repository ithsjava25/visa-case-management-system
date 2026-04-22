package org.example.visacasemanagementsystem.file;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png");

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB


    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.corsAllowedOrigins:http://localhost:8080}")
    private String corsAllowedOrigins;

    public FileService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @PostConstruct
    public void initializeBucket() {

        if (bucketName == null || bucketName.equals("test-bucket")) {
            log.info("Skipping bucket initialization (test profile or missing config)");
            return;
        }
        try {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
                log.info("Bucket '{}' already exists", bucketName);
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                    log.info("Bucket '{}' created successfully", bucketName);
                } else { throw e; }
            }
        } catch (Exception e) {
            log.error("Failed to verify/create bucket: {}", e.getMessage());
        }

        try {

            List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

            CORSRule corsRule = CORSRule.builder()
                    .allowedOrigins(origins)
                    .allowedMethods("GET","HEAD")
                    .allowedHeaders("*")
                    .build();

            s3Client.putBucketCors(PutBucketCorsRequest.builder()
                    .bucket(bucketName)
                    .corsConfiguration(CORSConfiguration.builder().corsRules(corsRule).build())
                    .build());
            log.info("CORS configuration applied to bucket '{}'", bucketName);
        } catch (S3Exception e) {
            log.warn("Failed to configure CORS to bucket  '{}': {}", bucketName, e.getMessage(), e);
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw  new IOException("File exceeds maximum allowed size of 10 MB");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IOException("Unsupported document type: " + contentType
                    + ". Allowed: PDF, JPEG, PNG");
        }

        // Sanitize filename
        String original = file.getOriginalFilename();
        String safeName = (original == null ? "file"
                : original.replaceAll("[^A-Za-z0-9._-]", "_"));

        String fileName = UUID.randomUUID() + "_" + safeName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return  fileName;
    }

    public String getPresignedDownloadUrl(String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public void deleteFile(String s3Key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }


}
