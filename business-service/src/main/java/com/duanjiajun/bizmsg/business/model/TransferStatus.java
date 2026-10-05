package com.duanjiajun.bizmsg.business.model;

/** 商品房转移业务状态：受理（ACCEPTED）→ 办结（COMPLETED）。 */
public enum TransferStatus {

    ACCEPTED("已受理"),

    COMPLETED("已办结");

    private final String label;

    TransferStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
