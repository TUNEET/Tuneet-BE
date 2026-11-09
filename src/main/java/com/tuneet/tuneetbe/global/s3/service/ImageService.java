package com.tuneet.tuneetbe.global.s3.service;

import com.tuneet.tuneetbe.global.exception.CustomException;
import com.tuneet.tuneetbe.global.s3.S3Uploader;
import com.tuneet.tuneetbe.global.s3.dto.ImageUploadResponse;
import com.tuneet.tuneetbe.global.s3.enums.ImageDirectory;
import com.tuneet.tuneetbe.global.s3.exception.S3ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final S3Uploader s3Uploader;

    public ImageUploadResponse uploadImage(MultipartFile file, ImageDirectory directory) {
        if (file.isEmpty()) {
            throw new CustomException(S3ErrorCode.EMPTY_IMAGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.containsKey(contentType)) {
            throw new CustomException(S3ErrorCode.INVALID_IMAGE_TYPE);
        }

        String imageUrl = s3Uploader.upload(file, directory.getPath(), ALLOWED_IMAGE_TYPES.get(contentType));
        return ImageUploadResponse.builder()
                .imageUrl(imageUrl)
                .build();
    }
}
