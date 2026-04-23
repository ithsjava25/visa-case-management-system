package org.example.visacasemanagementsystem.file;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import static org.assertj.core.api.Assertions.assertThat;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class FileServiceIntegrationTest {

    @Autowired
    private FileService fileService;

    @MockitoBean
    private S3Client s3Client;
    @MockitoBean
    private S3Presigner s3Presigner;


    @Test
    void uploadFile_ShouldReturnSKey_WhenUploadIsSuccessful() throws IOException {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String resultKey = fileService.uploadFile(mockFile);

        // Assert
        assertThat(resultKey).contains("test-document.pdf");
        assertThat(resultKey).contains("_");
        assertThat(resultKey).endsWith("test-document.pdf");

        verify(s3Client).putObject(
                argThat((PutObjectRequest req) -> req.bucket().equals("test-bucket")),
                any(RequestBody.class)
        );
    }

    @Test
    void uploadFile_shouldThrowException_WhenFileIsTooLarge() {
        // Arrange
        byte[] largeContent = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent
        );

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(largeFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("File exceeds maximum allowed size of 10 MB");

    }

    @Test
    void uploadFile_ShouldSanitizeFileName() throws IOException {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "my picture!.png",
                "image/png",
                "content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

        String resultKey = fileService.uploadFile(mockFile);

        assertThat(resultKey).contains("my_picture_.png");
        assertThat(resultKey).doesNotContain("!");
        assertThat(resultKey).doesNotContain(" ");

    }

    @Test
    void uploadFile_ShouldThrowException_WhenFileTypeIsInvalid() {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "virus.exe",
                "application/x-msdownload",
                "bad content".getBytes()
        );

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(mockFile))
        .isInstanceOf(IOException.class)
                .hasMessageContaining("Unsupported document type: " + mockFile.getContentType());


    }

    @Test
    void getPresignedDownloadUrl_ShouldReturnFormattedUrl() throws MalformedURLException {
        // Arrange
        String fileName = "test-file.pdf";
       String expectedUrl = "http://localhost:9000/test-bucket/" + fileName;

       PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);

       when(mockPresignedRequest.url()).thenReturn(URI.create(expectedUrl).toURL());

       when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
               .thenReturn(mockPresignedRequest);

       // Act
        String url = fileService.getPresignedDownloadUrl(fileName);

        // Assert
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    void deleteFile_ShouldCallS3DeleteObject() {
        // Arrange
        String s3Key = "some-uuid_test.pdf";

        // Act
        fileService.deleteFile(s3Key);

        // Assert
        verify(s3Client).deleteObject(argThat((DeleteObjectRequest req) ->
                req.bucket().equals("test-bucket") && req.key().equals(s3Key)));

    }

}
