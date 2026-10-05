package com.duanjiajun.bizmsg.message.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duanjiajun.bizmsg.message.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.message.dto.UploadReceiptRequest;
import com.duanjiajun.bizmsg.message.service.ReceiptStorageService;

import jakarta.validation.Valid;

/**
 * 对外暴露 /api/upload/receipt，供外部系统或手工调试直接提交报文。
 * 合并后 ReportService 直接调用 ReceiptStorageService，不再经过这里。
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
