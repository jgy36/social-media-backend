package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.service.PhotoUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/uploads")
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {

    @Autowired
    private PhotoUploadService photoUploadService;

    @GetMapping("/dating/{filename}")
    public ResponseEntity<byte[]> getDatingPhoto(@PathVariable String filename) {
        try {
            byte[] imageBytes = photoUploadService.getPhotoFile(filename);

            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (filename.toLowerCase().endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("public, max-age=2592000");

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
