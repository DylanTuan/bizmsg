package com.duanjiajun.bizmsg.business.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.duanjiajun.bizmsg.business.model.TransferBusiness;

/**
 * 商品房转移业务存储，内存实现，重启即清空。
 * 接口形态与 JPA Repository 一致，之后换 MySQL 只需替换本类，Service 层不动。
 */
@Repository
public class TransferRepository {

    private final Map<String, TransferBusiness> store = new ConcurrentHashMap<>();

    public TransferBusiness save(TransferBusiness business) {
        store.put(business.getBusinessId(), business);
        return business;
    }

    public Optional<TransferBusiness> findById(String businessId) {
        return Optional.ofNullable(store.get(businessId));
    }

    /** 按受理时间倒序。 */
    public List<TransferBusiness> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(TransferBusiness::getAcceptedAt).reversed())
                .toList();
    }
}
