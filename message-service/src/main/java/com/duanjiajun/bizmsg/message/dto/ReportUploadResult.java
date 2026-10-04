package com.duanjiajun.bizmsg.message.dto;

import com.duanjiajun.bizmsg.message.xml.model.ReportMessage;

/**
 * 生成 + 上传的结果：上传失败时 uploaded=false 并带上 degradeReason，
 * 调用方据此决定是否重试，报文本身不会因为下游故障而丢失。
 */
public record ReportUploadResult(String businessId, String messageType, String createdAt, String xml,
                                 boolean uploaded, String receiptNo, String storedPath, String degradeReason) {

    public static ReportUploadResult uploaded(ReportMessage message, String xml, ReceiptResponse receipt) {
        return new ReportUploadResult(message.getBusinessId(), message.getMessageType(), message.getCreatedAt(),
                xml, true, receipt == null ? null : receipt.receiptNo(),
                receipt == null ? null : receipt.storedPath(), null);
    }

    public static ReportUploadResult degraded(ReportMessage message, String xml, String degradeReason) {
        return new ReportUploadResult(message.getBusinessId(), message.getMessageType(), message.getCreatedAt(),
                xml, false, null, null, degradeReason);
    }
}
