package com.duanjiajun.bizmsg.report.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.duanjiajun.bizmsg.report.xml.model.ReportMessage;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

/**
 * XML 报文生成器：负责对象与 XML 的双向转换，不掺杂业务逻辑，方便单测。
 * JAXBContext 创建开销大且线程安全，这里只初始化一次并复用。
 */
@Component
public class ReportXmlBuilder {

    private static final JAXBContext CONTEXT = createContext();

    /** 对象转 XML 字符串，带缩进与 UTF-8 声明，便于人工核对报文。 */
    public String toXml(ReportMessage message) {
        try {
            Marshaller marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            StringWriter writer = new StringWriter();
            marshaller.marshal(message, writer);
            return writer.toString();
        } catch (JAXBException ex) {
            throw new IllegalStateException("生成 XML 报文失败，businessId=" + message.getBusinessId(), ex);
        }
    }

    /** XML 字符串转对象，报文字段缺失或格式非法时抛出 IllegalArgumentException。 */
    public ReportMessage parse(String xml) {
        try {
            return (ReportMessage) CONTEXT.createUnmarshaller().unmarshal(new StringReader(xml));
        } catch (JAXBException ex) {
            throw new IllegalArgumentException("解析 XML 报文失败", ex);
        }
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(ReportMessage.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException("初始化 JAXBContext 失败", ex);
        }
    }
}
