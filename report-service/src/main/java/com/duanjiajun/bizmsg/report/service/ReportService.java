package com.duanjiajun.bizmsg.report.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.duanjiajun.bizmsg.report.client.UploadClient;
import com.duanjiajun.bizmsg.report.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.report.dto.ReportUploadResult;
import com.duanjiajun.bizmsg.report.dto.UploadReceiptRequest;
import com.duanjiajun.bizmsg.report.xml.ReportXmlBuilder;
import com.duanjiajun.bizmsg.report.xml.model.ReportMessage;

/**
 * 报文业务：本地生成 XML 后通过 Feign 上传，上传失败走降级而不是直接抛错。
 */
@Service
public class ReportService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportXmlBuilder xmlBuilder;
    private final UploadClient uploadClient;

    public ReportService(ReportXmlBuilder xmlBuilder, UploadClient uploadClient) {
        this.xmlBuilder = xmlBuilder;
        this.uploadClient = uploadClient;
    }

    public ReportUploadResult generateAndUpload(String businessId, String messageType, String payload) {
        // 流水号缺省时生成 32 位无连字符 UUID，下游以它做幂等键
        String finalBusinessId = StringUtils.hasText(businessId)
                ? businessId
                : UUID.randomUUID().toString().replace("-", "");
        ReportMessage message = new ReportMessage(finalBusinessId, messageType,
                LocalDateTime.now().format(TIME_FORMATTER), payload);
        String xml = xmlBuilder.toXml(message);

        try {
            ReceiptResponse receipt = uploadClient.upload(
                    new UploadReceiptRequest(finalBusinessId, messageType, xml));
            log.info("报文上传成功 businessId={} receiptNo={}", finalBusinessId,
                    receipt == null ? null : receipt.receiptNo());
            return ReportUploadResult.uploaded(message, xml, receipt);
        } catch (RuntimeException ex) {
            // 降级：报文已生成，仅记录上传失败原因并原样返回，避免下游故障导致报文丢失
            log.warn("报文上传失败，降级返回本地报文 businessId={} 原因={}", finalBusinessId, ex.toString());
            return ReportUploadResult.degraded(message, xml, ex.getMessage());
        }
    }
}
