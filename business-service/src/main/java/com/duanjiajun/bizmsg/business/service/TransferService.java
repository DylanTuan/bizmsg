package com.duanjiajun.bizmsg.business.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.duanjiajun.bizmsg.business.dto.TransferCompletedEvent;
import com.duanjiajun.bizmsg.business.dto.TransferCreateRequest;
import com.duanjiajun.bizmsg.business.dto.TransferView;
import com.duanjiajun.bizmsg.business.exception.TransferNotFoundException;
import com.duanjiajun.bizmsg.business.model.TransferBusiness;
import com.duanjiajun.bizmsg.business.mq.TransferEventPublisher;
import com.duanjiajun.bizmsg.business.repository.TransferRepository;

/**
 * 商品房转移业务：受理 → 办结。
 * 办结是唯一对外发消息的动作，状态机校验、幂等、投递失败不推进状态都收在这里。
 */
@Service
public class TransferService {

    /** message-service 侧据此选择报文模型。 */
    public static final String MESSAGE_TYPE = "HOUSE-TRANSFER";

    private static final DateTimeFormatter BUSINESS_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository repository;
    private final TransferEventPublisher publisher;

    public TransferService(TransferRepository repository, TransferEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /** 受理：生成业务流水号，状态 ACCEPTED。 */
    public TransferView accept(TransferCreateRequest request) {
        TransferBusiness business = new TransferBusiness(nextBusinessId(),
                request.sellerName(), request.sellerIdNo(), request.buyerName(), request.buyerIdNo(),
                request.houseAddress(), request.houseArea(), request.housePrice(),
                LocalDateTime.now().format(TIME_FORMATTER));
        repository.save(business);
        log.info("受理商品房转移业务 businessId={} 房屋={} 成交价={}",
                business.getBusinessId(), business.getHouseAddress(), business.getHousePrice());
        return TransferView.of(business);
    }

    /** 办结：投递事件成功后才推进为 COMPLETED；重复调用直接返回已办结视图，不重复发消息。 */
    public TransferView complete(String businessId) {
        TransferBusiness business = repository.findById(businessId)
                .orElseThrow(() -> new TransferNotFoundException(businessId));

        // 同一业务号并发办结时串行化，避免重复投递；生产环境应改为数据库条件更新（where status='ACCEPTED'）
        synchronized (business) {
            if (business.isCompleted()) {
                log.info("业务已是办结状态，幂等返回 businessId={} messageId={}", businessId, business.getMessageId());
                return TransferView.of(business);
            }

            TransferCompletedEvent event = buildEvent(business);
            // 投递失败会抛异常，状态保持 ACCEPTED，调用方直接重试
            publisher.publish(event);
            business.markCompleted(LocalDateTime.now().format(TIME_FORMATTER), event.messageId());
            log.info("业务办结完成 businessId={} messageId={}", businessId, event.messageId());
            return TransferView.of(business);
        }
    }

    public TransferView get(String businessId) {
        return repository.findById(businessId)
                .map(TransferView::of)
                .orElseThrow(() -> new TransferNotFoundException(businessId));
    }

    public List<TransferView> list() {
        return repository.findAll().stream().map(TransferView::of).toList();
    }

    /** 组装办结事件；messageId 用于去重与链路追踪。 */
    private TransferCompletedEvent buildEvent(TransferBusiness business) {
        return new TransferCompletedEvent(
                UUID.randomUUID().toString().replace("-", ""),
                business.getBusinessId(),
                MESSAGE_TYPE,
                LocalDateTime.now().format(TIME_FORMATTER),
                new TransferCompletedEvent.Party(business.getSellerName(), business.getSellerIdNo()),
                new TransferCompletedEvent.Party(business.getBuyerName(), business.getBuyerIdNo()),
                new TransferCompletedEvent.House(business.getHouseAddress(), business.getHouseArea(),
                        business.getHousePrice()));
    }

    /** 业务流水号：TRF-yyyyMMddHHmmssSSS-####，同毫秒并发靠自增序号区分。 */
    private String nextBusinessId() {
        return "TRF-" + LocalDateTime.now().format(BUSINESS_ID_FORMATTER) + "-"
                + String.format("%04d", SEQUENCE.incrementAndGet() % 10000);
    }
}
