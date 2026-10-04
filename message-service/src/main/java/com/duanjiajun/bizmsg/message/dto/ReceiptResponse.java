package com.duanjiajun.bizmsg.message.dto;

/**
 * 回执响应体：报文落盘成功后返回给调用方的凭证。
 * 合并前 report-service 与 upload-service 各留一份同名字段完全一致的副本（靠注释互相提醒同步），
 * 合并后只保留这一份，字段改动不会再出现两边不一致的问题。
 */
public record ReceiptResponse(String receiptNo, String storedPath, String receivedAt, boolean success, String message) {
}
