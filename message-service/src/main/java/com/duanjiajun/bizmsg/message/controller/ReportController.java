package com.duanjiajun.bizmsg.message.controller;

import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duanjiajun.bizmsg.message.dto.ReceiptView;
import com.duanjiajun.bizmsg.message.dto.ReportGenerateRequest;
import com.duanjiajun.bizmsg.message.dto.ReportUploadResult;
import com.duanjiajun.bizmsg.message.exception.ReceiptNotFoundException;
import com.duanjiajun.bizmsg.message.service.ReceiptStorageService;
import com.duanjiajun.bizmsg.message.service.ReportService;

/** 报文生成入口，经网关 /api/report/** 转发进来。合并后路径没变，网关断言和前端 URL 都不用改。 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final String DEFAULT_MESSAGE_TYPE = "ACCEPT";

    private final ReportService reportService;
    private final ReceiptStorageService receiptStorageService;

    public ReportController(ReportService reportService, ReceiptStorageService receiptStorageService) {

        this.reportService = reportService;
        this.receiptStorageService = receiptStorageService;
    }

    /** 连通性探测。 */
    @GetMapping("/ping")
    public Map<String, String> ping() {

        return Map.of("service", "message-service", "status", "UP");
    }

    /** 按业务号查回执。报文是异步生成的，调用方拿 404 判断「还没生成」。 */
    @GetMapping("/receipt/{businessId}")
    public ReceiptView receipt(@PathVariable String businessId) {
        return receiptStorageService.findByBusinessId(businessId)
                .orElseThrow(() -> new ReceiptNotFoundException(businessId));
    }

    /** 全部回执，按时间倒序，供「报文记录」页展示。 */
    @GetMapping("/receipts")
    public List<ReceiptView> receipts() {
        return receiptStorageService.listAll();
    }

    /** 回显已落盘报文原文。用 JSON 包一层，避免 text/* 的字符集默认值把中文弄乱码。 */
    @GetMapping("/receipt/{businessId}/xml")
    public Map<String, String> receiptXml(@PathVariable String businessId) {
        return Map.of("businessId", businessId, "xml", receiptStorageService.readXml(businessId));
    }

    /**
     * 生成 XML 报文并落盘，businessId 缺省时由服务端生成。
     * 优先读 JSON body（报文含中文，放 body 可绕开 URL 编码）；未传 body 时回退查询参数，便于手工调试。
     */
    @PostMapping("/generate")
    public ReportUploadResult generate(@RequestBody(required = false) ReportGenerateRequest body,
                                       @RequestParam(name = "businessId", required = false) String businessId,
                                       @RequestParam(name = "messageType", required = false) String messageType,
                                       @RequestParam(name = "payload", required = false) String payload) {
        ReportGenerateRequest request = body != null
                ? body
                : new ReportGenerateRequest(businessId, messageType, payload);
        String finalMessageType = StringUtils.hasText(request.messageType()) ? request.messageType() : DEFAULT_MESSAGE_TYPE;
        return reportService.generateAndUpload(request.businessId(), finalMessageType, request.payload());
    }
}
