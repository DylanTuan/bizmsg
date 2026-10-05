package com.duanjiajun.bizmsg.message.exception;

/**
 * 回执不存在：业务尚未生成报文，或业务号有误，由 GlobalExceptionHandler 映射为 404。
 */
public class ReceiptNotFoundException extends RuntimeException {

    public ReceiptNotFoundException(String businessId) {
        super("未找到该业务号对应的报文回执：" + businessId);
    }
}
