package com.duanjiajun.bizmsg.business.exception;

/** 由 GlobalExceptionHandler 映射为 404。 */
public class TransferNotFoundException extends RuntimeException {

    public TransferNotFoundException(String businessId) {
        super("业务不存在：" + businessId);
    }
}
