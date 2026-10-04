package com.duanjiajun.bizmsg.report.dto;

/**
 * 上传请求体：Feign 调用 upload-service 时传输的 JSON 结构。
 * xml 为完整报文文本，下游按原样落盘并做回执。
 */
public record UploadReceiptRequest(String businessId, String messageType, String xml) {
}
