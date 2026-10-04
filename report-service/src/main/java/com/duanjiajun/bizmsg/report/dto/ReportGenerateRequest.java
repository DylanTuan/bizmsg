package com.duanjiajun.bizmsg.report.dto;

/**
 * 生成报文的入参：建议用 JSON body 传入，报文内容含中文时无需再做 URL 编码。
 * 字段都可缺省：businessId 为空由服务端生成，messageType 为空按 ACCEPT 处理。
 */
public record ReportGenerateRequest(String businessId, String messageType, String payload) {
}
