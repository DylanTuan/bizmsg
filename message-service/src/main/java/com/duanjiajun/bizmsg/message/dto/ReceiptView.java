package com.duanjiajun.bizmsg.message.dto;

/**
 * 报文回执视图：在 {@link ReceiptResponse} 基础上补上业务号与报文类型，供「报文记录」列表使用。
 * 回执查询与列表共用这一个结构，前端拿到的字段形状保持一致。
 */
public record ReceiptView(String businessId,
                          String messageType,
                          String receiptNo,
                          String storedPath,
                          String receivedAt,
                          boolean success,
                          String message) {

    /** 由落盘结果组装视图。 */
    public static ReceiptView of(String businessId, String messageType, ReceiptResponse receipt) {
        return new ReceiptView(businessId, messageType, receipt.receiptNo(), receipt.storedPath(),
                receipt.receivedAt(), receipt.success(), receipt.message());
    }
}
