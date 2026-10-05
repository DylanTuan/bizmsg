package com.duanjiajun.bizmsg.message.service;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.duanjiajun.bizmsg.message.config.UploadProperties;
import com.duanjiajun.bizmsg.message.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.message.dto.ReceiptView;
import com.duanjiajun.bizmsg.message.dto.UploadReceiptRequest;
import com.duanjiajun.bizmsg.message.exception.ReceiptNotFoundException;

/**
 * 报文落盘与回执：先校验 XML 格式良好，再写入本地目录并生成回执编号。
 */
@Service
public class ReceiptStorageService {

    private static final DateTimeFormatter RECEIPT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Logger log = LoggerFactory.getLogger(ReceiptStorageService.class);

    /** 同毫秒内的并发请求靠自增序号区分，避免文件名冲突。 */
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final UploadProperties properties;

    /**
     * 业务号到回执的内存索引。落盘文件名是回执号，光看目录反查不到业务号，
     * 前端的「办结后查看报文」和记录列表都靠它。生产环境对应一张 business_id 唯一索引的表。
     */
    private final Map<String, ReceiptView> receiptIndex = new ConcurrentHashMap<>();

    public ReceiptStorageService(UploadProperties properties) {

        this.properties = properties;
    }

    public ReceiptResponse store(UploadReceiptRequest request) {
        // 非法报文直接拒绝，避免脏数据落盘
        requireWellFormedXml(request.xml());

        String receiptNo = properties.getReceiptPrefix() + "-" + LocalDateTime.now().format(RECEIPT_FORMATTER)
                + "-" + String.format("%04d", SEQUENCE.incrementAndGet() % 10000);
        Path directory = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
        Path target = directory.resolve(receiptNo + ".xml");
        try {
            Files.createDirectories(directory);
            Files.writeString(target, request.xml(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            throw new IllegalStateException("报文落盘失败：" + target, ex);
        }
        log.info("报文落盘成功 businessId={} messageType={} receiptNo={} path={}",
                request.businessId(), request.messageType(), receiptNo, target);
        ReceiptResponse response = new ReceiptResponse(receiptNo, target.toString(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), true, "回执已生成");
        // 落盘成功后才建索引，保证「索引里有 = 文件一定在」
        receiptIndex.put(request.businessId(), ReceiptView.of(request.businessId(), request.messageType(), response));
        return response;
    }

    /** 按业务号反查回执，供「办结后查看报文」使用。 */
    public Optional<ReceiptView> findByBusinessId(String businessId) {
        return Optional.ofNullable(receiptIndex.get(businessId));
    }

    /** 全部回执，按时间倒序，供「报文记录」页展示。 */
    public List<ReceiptView> listAll() {
        return receiptIndex.values().stream()
                .sorted(Comparator.comparing(ReceiptView::receivedAt).reversed())
                .toList();
    }

    /** 读取已落盘报文的原文，供前端回显与人工核对。 */
    public String readXml(String businessId) {
        ReceiptView receipt = findByBusinessId(businessId)
                .orElseThrow(() -> new ReceiptNotFoundException(businessId));
        try {
            return Files.readString(Path.of(receipt.storedPath()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("读取已落盘报文失败：" + receipt.storedPath(), ex);
        }
    }

    /** 校验 XML 格式良好；关掉 DTD 和外部实体，防 XXE 与实体膨胀。 */
    private void requireWellFormedXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new IllegalArgumentException("报文不是格式良好的 XML", ex);
        }
    }
}
