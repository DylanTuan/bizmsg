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
 * 落盘入口：对外暴露 /api/upload/receipt，供外部系统或手工调试直接提交报文。
 * 合并前这个接口只服务于 report-service 的 Feign 调用；现在 ReportService 在同一进程内
 * 直接调用 ReceiptStorageService，不经过本控制器，因此这里唯一的调用方变成了外部请求。
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
