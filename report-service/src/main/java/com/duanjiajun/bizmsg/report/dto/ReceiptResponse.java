package com.duanjiajun.bizmsg.report.dto;

/**
 * 回执响应体：upload-service 上传成功的凭证，与 upload-service 中的同名 DTO 保持字段一致。
 */
public record ReceiptResponse(String receiptNo, String storedPath, String receivedAt, boolean success, String message) {
}
