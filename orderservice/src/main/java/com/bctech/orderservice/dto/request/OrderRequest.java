package com.bctech.orderservice.dto.request;

import com.bctech.orderservice.dto.response.CartItemsDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private List<CartItemsDto> cartItemsDto;
}
