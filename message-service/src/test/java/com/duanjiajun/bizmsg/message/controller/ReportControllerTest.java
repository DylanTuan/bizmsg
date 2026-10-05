package com.duanjiajun.bizmsg.message.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.duanjiajun.bizmsg.message.config.UploadProperties;
import com.duanjiajun.bizmsg.message.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.message.dto.UploadReceiptRequest;
import com.duanjiajun.bizmsg.message.service.ReceiptStorageService;
import com.duanjiajun.bizmsg.message.service.ReportService;
import com.duanjiajun.bizmsg.message.xml.ReportXmlBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 控制器单测：用桩件替换落盘服务，不依赖 Nacos 与网络。
 * 合并前这里替换的是 Feign 客户端 UploadClient，接收方是另一个进程；
 * 现在是同进程的 ReceiptStorageService，因此还多了一条不 mock 的真实链路用例。
 * 注意 MockMvc 不走 Tomcat 连接器，因此这里验的是参数绑定与报文生成，不负责复现容器对 URI 的校验。
 */
class ReportControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReceiptStorageService storage = successStorage();
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(
                new ReportService(new ReportXmlBuilder(), storage), storage)).build();
    }

    @Test
    @DisplayName("JSON body 传中文报文：生成 XML 并落盘成功")
    void generateWithJsonBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/report/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"BIZ1001\",\"messageType\":\"ACCEPT\",\"payload\":\"开户受理\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("uploaded").asBoolean()).isTrue();
        assertThat(body.get("businessId").asText()).isEqualTo("BIZ1001");
        assertThat(body.get("receiptNo").asText()).isEqualTo("RCPT-TEST-0001");
        assertThat(body.get("xml").asText()).contains("<payload>开户受理</payload>");
    }

    @Test
    @DisplayName("查询参数方式兼容，messageType 缺省按 ACCEPT 处理且 businessId 自动生成")
    void generateWithQueryParams() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/report/generate")
                        .param("payload", "开户受理"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("uploaded").asBoolean()).isTrue();
        assertThat(body.get("messageType").asText()).isEqualTo("ACCEPT");
        assertThat(body.get("businessId").asText()).hasSize(32);
    }

    @Test
    @DisplayName("落盘失败时降级：HTTP 仍 200，返回 degradeReason 且报文不丢")
    void degradeWhenStorageFails() throws Exception {
        ReceiptStorageService failingStorage = mock(ReceiptStorageService.class);
        when(failingStorage.store(any(UploadReceiptRequest.class)))
                .thenThrow(new IllegalStateException("报文落盘失败：/readonly/data/upload"));

        MockMvc failingMvc = MockMvcBuilders.standaloneSetup(new ReportController(
                new ReportService(new ReportXmlBuilder(), failingStorage), failingStorage)).build();

        MvcResult result = failingMvc.perform(post("/api/report/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"BIZ1002\",\"payload\":\"开户受理\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("uploaded").asBoolean()).isFalse();
        assertThat(body.get("degradeReason").asText()).contains("报文落盘失败");
        assertThat(body.get("xml").asText()).contains("BIZ1002");
    }

    @Test
    @DisplayName("合并后的真实链路：生成 XML → 进程内落盘 → 回执里的文件确实存在")
    void storeForRealThroughInProcessChain(@TempDir Path tempDir) throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setStorageDir(tempDir.toString());
        properties.setReceiptPrefix("RCPT");

        ReceiptStorageService storage = new ReceiptStorageService(properties);
        MockMvc realMvc = MockMvcBuilders.standaloneSetup(
                new ReportController(new ReportService(new ReportXmlBuilder(), storage), storage)).build();

        MvcResult result = realMvc.perform(post("/api/report/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"BIZ-MERGE-1\",\"messageType\":\"ACCEPT\",\"payload\":\"合并后直调\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("uploaded").asBoolean()).isTrue();
        assertThat(body.get("degradeReason").isNull()).isTrue();

        Path stored = Path.of(body.get("storedPath").asText());
        assertThat(stored).exists().startsWith(tempDir);
        assertThat(Files.readString(stored, StandardCharsets.UTF_8))
                .contains("<businessId>BIZ-MERGE-1</businessId>")
                .contains("<payload>合并后直调</payload>");
    }

    @Test
    @DisplayName("按业务号查回执：已落盘可查到，未落盘 404")
    void queryReceiptByBusinessId(@TempDir Path tempDir) throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setStorageDir(tempDir.toString());
        properties.setReceiptPrefix("RCPT");
        ReceiptStorageService storage = new ReceiptStorageService(properties);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new ReportController(new ReportService(new ReportXmlBuilder(), storage), storage))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // 业务尚未生成报文 → 404，前端据此判断「异步报文还没到」
        mvc.perform(get("/api/report/receipt/TRF-NOT-EXIST")).andExpect(status().isNotFound());

        mvc.perform(post("/api/report/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"TRF-QUERY-1\",\"messageType\":\"HOUSE-TRANSFER\",\"payload\":\"商品房转移\"}"))
                .andExpect(status().isOk());

        MvcResult result = mvc.perform(get("/api/report/receipt/TRF-QUERY-1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("receiptNo").asText()).startsWith("RCPT-");
        assertThat(body.get("success").asBoolean()).isTrue();

        // 报文原文回显：前端「商品房转移」页用它展示与下载 XML
        MvcResult xmlResult = mvc.perform(get("/api/report/receipt/TRF-QUERY-1/xml"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readBody(xmlResult).get("xml").asText())
                .contains("<report>")
                .contains("<businessId>TRF-QUERY-1</businessId>");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private ReceiptStorageService successStorage() {
        ReceiptStorageService storage = mock(ReceiptStorageService.class);
        when(storage.store(any(UploadReceiptRequest.class))).thenReturn(new ReceiptResponse(
                "RCPT-TEST-0001", "/tmp/RCPT-TEST-0001.xml", "2026-10-03 16:00:00", true, "回执已生成"));
        return storage;
    }
}
