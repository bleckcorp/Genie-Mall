package com.bctech.inventoryservice.service;

import com.bctech.inventoryservice.dto.InventoryResponse;
import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InventoryService {
    @Transactional(readOnly = true)
    @SneakyThrows
    List<InventoryResponse> isInStock(List<String> skuCode);
}
