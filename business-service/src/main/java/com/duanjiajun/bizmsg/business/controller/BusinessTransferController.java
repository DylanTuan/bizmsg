package com.duanjiajun.bizmsg.business.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duanjiajun.bizmsg.business.dto.TransferCreateRequest;
import com.duanjiajun.bizmsg.business.dto.TransferView;
import com.duanjiajun.bizmsg.business.service.TransferService;

import jakarta.validation.Valid;

/**
 * 商品房转移业务入口，经网关 /api/business/** 转发进来。
 * 受理和办结分成两个动作，办结才触发 MQ 事件。
 */
@RestController
@RequestMapping("/api/business")
public class BusinessTransferController {

    private final TransferService transferService;

    public BusinessTransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /** 连通性探测。 */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "business-service", "status", "UP");
    }

    /** 受理商品房转移业务。 */
    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferView accept(@Valid @RequestBody TransferCreateRequest request) {
        return transferService.accept(request);
    }

    /** 办结业务并投递 MQ 事件；重复调用是幂等的。 */
    @PostMapping("/transfer/{businessId}/complete")
    public TransferView complete(@PathVariable String businessId) {
        return transferService.complete(businessId);
    }

    /** 查询单笔业务。 */
    @GetMapping("/transfer/{businessId}")
    public TransferView get(@PathVariable String businessId) {
        return transferService.get(businessId);
    }

    /** 业务列表，按受理时间倒序。 */
    @GetMapping("/transfer")
    public List<TransferView> list() {
        return transferService.list();
    }
}
