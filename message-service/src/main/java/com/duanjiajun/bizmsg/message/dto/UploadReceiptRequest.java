package com.duanjiajun.bizmsg.message.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 落盘请求体：合并前 report 侧与 upload 侧各留一份，现在外部调用方直接 POST /api/upload/receipt
 * 与 ReportService 进程内调用共用这一份；@NotBlank 只在 UploadController 的 @Valid 边界上生效。
 */
public record UploadReceiptRequest(@NotBlank(message = "businessId 不能为空") String businessId,
                                   @NotBlank(message = "messageType 不能为空") String messageType,
                                   @NotBlank(message = "xml 不能为空") String xml) {
}
