package com.duanjiajun.bizmsg.upload.dto;

/**
 * 回执响应体：与 report-service 中的同名 DTO 字段保持一致。
 */
public record ReceiptResponse(String receiptNo, String storedPath, String receivedAt, boolean success, String message) {
}
