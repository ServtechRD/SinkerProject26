package com.sinker.app.dto.erp;

public class ErpOrderResponse {

    private String orderNo;
    private String status;

    public ErpOrderResponse() {}

    public ErpOrderResponse(String orderNo, String status) {
        this.orderNo = orderNo;
        this.status = status;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
