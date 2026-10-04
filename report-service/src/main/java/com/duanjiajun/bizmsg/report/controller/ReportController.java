package com.duanjiajun.bizmsg.report.controller;

import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duanjiajun.bizmsg.report.dto.ReportGenerateRequest;
import com.duanjiajun.bizmsg.report.dto.ReportUploadResult;
import com.duanjiajun.bizmsg.report.service.ReportService;

/**
 * 报文入口：经网关 /api/report/** 转发进来。
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final String DEFAULT_MESSAGE_TYPE = "ACCEPT";

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 连通性探测，用于确认网关路由已生效。 */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "report-service", "status", "UP");
    }

    /**
     * 生成 XML 报文并上传，businessId 缺省时由服务端自动生成。
     * 优先读 JSON body：报文内容常含中文，放 body 可绕开 URL 编码问题；
     * 未传 body 时回退查询参数，便于手工调试——此时中文必须 URL 编码，否则 Tomcat 会直接返回 400。
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
