package com.mjsec.lms.media.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Getter
@Builder
public class ImageResponse {
    private Resource resource;
    private MediaType mediaType;
    private long contentLength;
    private String originalFilename;
}