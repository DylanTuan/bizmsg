package com.duanjiajun.bizmsg.message.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.duanjiajun.bizmsg.message.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.message.dto.ReportUploadResult;
import com.duanjiajun.bizmsg.message.dto.UploadReceiptRequest;
import com.duanjiajun.bizmsg.message.xml.ReportXmlBuilder;
import com.duanjiajun.bizmsg.message.xml.model.ReportMessage;

/** 本地生成 XML 后交给落盘服务；落盘失败只降级不抛错，避免 IO 故障把已生成的报文吞掉。 */
@Service
public class ReportService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportXmlBuilder xmlBuilder;
    private final ReceiptStorageService receiptStorageService;

    public ReportService(ReportXmlBuilder xmlBuilder, ReceiptStorageService receiptStorageService) {
        this.xmlBuilder = xmlBuilder;
        this.receiptStorageService = receiptStorageService;
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
            // 进程内直调：入参校验由 UploadController 的 @Valid 边界负责，这里只关心落盘结果
            ReceiptResponse receipt = receiptStorageService.store(
                    new UploadReceiptRequest(finalBusinessId, messageType, xml));
            log.info("报文落盘成功 businessId={} receiptNo={}", finalBusinessId,
                    receipt == null ? null : receipt.receiptNo());
            return ReportUploadResult.uploaded(message, xml, receipt);
        } catch (RuntimeException ex) {
            // 降级：报文已经生成，仅记录落盘失败原因并原样返回，避免磁盘/IO 故障导致报文丢失
            log.warn("报文落盘失败，降级返回本地报文 businessId={} 原因={}", finalBusinessId, ex.toString());
            return ReportUploadResult.degraded(message, xml, ex.getMessage());
        }
    }
}
