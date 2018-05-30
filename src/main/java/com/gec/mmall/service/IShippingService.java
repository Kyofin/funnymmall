package com.gec.mmall.service;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.Shipping;

public interface IShippingService {
	ServerResponse add(Integer userId, Shipping shipping);

	ServerResponse del(Integer userId, Integer shippingId);

	ServerResponse update(Integer userId, Shipping shipping);

	ServerResponse<Shipping> select(Integer userId, Integer shippingId);

	ServerResponse list(Integer userId, int pageNum, int pageSize);
}
