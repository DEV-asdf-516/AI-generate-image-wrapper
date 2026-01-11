package com.communitcation.rest.controller;


import com.communitcation.rest.model.ImageRequest;
import com.communitcation.rest.model.ImageResponse;
import com.communitcation.rest.service.GenerateImageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class ApiController {

    private final GenerateImageService generateImageService;

    @PostMapping("/api/generate")
    @ResponseBody
    public ResponseEntity<ImageResponse> generateImage(
            @RequestBody ImageRequest imageRequest,
            HttpServletRequest request
    ) throws IOException {
        HttpSession session = request.getSession();

        String genImage
                =  generateImageService.generateImageUrl(imageRequest, session);

        session.setAttribute("genImage",genImage);
        session.setAttribute("prompt", imageRequest.getPrompt());

        return ResponseEntity.ok(
                new ImageResponse(genImage)
        );
    }
}
