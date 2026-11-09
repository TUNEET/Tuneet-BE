package com.tuneet.tuneetbe.global.s3.service;

import com.tuneet.tuneetbe.global.exception.CustomException;
import com.tuneet.tuneetbe.global.s3.S3Uploader;
import com.tuneet.tuneetbe.global.s3.dto.ImageUploadResponse;
import com.tuneet.tuneetbe.global.s3.enums.ImageDirectory;
import com.tuneet.tuneetbe.global.s3.exception.S3ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final String IMAGE_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/image";
    private static final byte[] CONTENT = {1, 2, 3};

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    private ImageService imageService;

    @ParameterizedTest
    @CsvSource({"image/jpeg, jpg", "image/png, png", "image/webp, webp"})
    @DisplayName("허용된 이미지 형식이면 형식에 맞는 확장자로 업로드하고 URL을 반환한다")
    void success(String contentType, String extension) {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "내 사진 (1).img", contentType, CONTENT);
        given(s3Uploader.upload(file, "profile-image", extension)).willReturn(IMAGE_URL);

        // when
        ImageUploadResponse response = imageService.uploadImage(file, ImageDirectory.PROFILE);

        // then
        assertThat(response.getImageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    @DisplayName("빈 파일이면 EMPTY_IMAGE 예외가 발생하고 업로드하지 않는다")
    void emptyFile() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        // when & then
        assertThatThrownBy(() -> imageService.uploadImage(file, ImageDirectory.PROFILE))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(S3ErrorCode.EMPTY_IMAGE);

        verify(s3Uploader, never()).upload(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("허용되지 않은 형식이면 INVALID_IMAGE_TYPE 예외가 발생하고 업로드하지 않는다")
    void invalidType() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", CONTENT);

        // when & then
        assertThatThrownBy(() -> imageService.uploadImage(file, ImageDirectory.PROFILE))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(S3ErrorCode.INVALID_IMAGE_TYPE);

        verify(s3Uploader, never()).upload(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Content-Type이 없으면 INVALID_IMAGE_TYPE 예외가 발생한다")
    void noContentType() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "a.png", null, CONTENT);

        // when & then
        assertThatThrownBy(() -> imageService.uploadImage(file, ImageDirectory.ROOM))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(S3ErrorCode.INVALID_IMAGE_TYPE);
    }
}
