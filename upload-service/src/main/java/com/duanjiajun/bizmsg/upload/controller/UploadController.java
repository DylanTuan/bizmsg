package com.duanjiajun.bizmsg.upload.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duanjiajun.bizmsg.upload.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.upload.dto.UploadReceiptRequest;
import com.duanjiajun.bizmsg.upload.service.ReceiptStorageService;

import jakarta.validation.Valid;

/**
 * 上传入口：report-service 通过 Feign 调用 /api/upload/receipt。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final ReceiptStorageService storageService;

    public UploadController(ReceiptStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/receipt")
    public ReceiptResponse receipt(@Valid @RequestBody UploadReceiptRequest request) {
        return storageService.store(request);
    }
}
