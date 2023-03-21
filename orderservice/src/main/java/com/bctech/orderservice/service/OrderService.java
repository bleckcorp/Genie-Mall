package com.bctech.orderservice.service;

import com.bctech.orderservice.dto.request.OrderRequest;

public interface OrderService {
    String placeOrder(OrderRequest orderRequest);
}
