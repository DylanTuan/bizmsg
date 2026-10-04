package com.duanjiajun.bizmsg.message.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.duanjiajun.bizmsg.message.config.UploadProperties;
import com.duanjiajun.bizmsg.message.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.message.dto.UploadReceiptRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("合法报文落盘成功并返回带回执前缀的回执")
    void storeValidXml() throws Exception {
        ReceiptStorageService service = new ReceiptStorageService(properties());
        String xml = "<report><businessId>BIZ001</businessId></report>";

        ReceiptResponse receipt = service.store(new UploadReceiptRequest("BIZ001", "ACCEPT", xml));

        assertThat(receipt.success()).isTrue();
        assertThat(receipt.receiptNo()).startsWith("RCPT-");
        Path stored = Path.of(receipt.storedPath());
        assertThat(stored).exists();
        assertThat(Files.readString(stored, StandardCharsets.UTF_8)).isEqualTo(xml);
    }

    @Test
    @DisplayName("格式非法的报文被拒绝，不落盘")
    void rejectMalformedXml() {
        ReceiptStorageService service = new ReceiptStorageService(properties());

        assertThatThrownBy(() -> service.store(new UploadReceiptRequest("BIZ002", "ACCEPT", "<report>")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("格式良好");
        assertThat(tempDir.toFile().listFiles()).isEmpty();
    }

    @Test
    @DisplayName("禁用外部实体，DOCTYPE 报文被拒绝（防 XXE）")
    void rejectDoctype() {
        ReceiptStorageService service = new ReceiptStorageService(properties());
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<report><payload>&xxe;</payload></report>";

        assertThatThrownBy(() -> service.store(new UploadReceiptRequest("BIZ003", "ACCEPT", xxe)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private UploadProperties properties() {
        UploadProperties properties = new UploadProperties();
        properties.setStorageDir(tempDir.toString());
        properties.setReceiptPrefix("RCPT");
        return properties;
    }
}
