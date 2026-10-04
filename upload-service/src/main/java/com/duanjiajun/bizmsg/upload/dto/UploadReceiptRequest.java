package com.duanjiajun.bizmsg.upload.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 上传请求体：与 report-service 中的同名 DTO 字段保持一致（后续可抽公共 api 模块）。
 */
public record UploadReceiptRequest(@NotBlank(message = "businessId 不能为空") String businessId,
                                   @NotBlank(message = "messageType 不能为空") String messageType,
                                   @NotBlank(message = "xml 不能为空") String xml) {
}
