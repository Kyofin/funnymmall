package com.gec.mmall.service;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.vo.CartVO;

public interface ICartService {
	ServerResponse add(Integer userId, Integer productId, Integer count);

	ServerResponse update(Integer userId, Integer productId, Integer count);

	ServerResponse<CartVO> deleteProduct(Integer userId, String productIds);

	ServerResponse<CartVO> list(Integer userId);

	ServerResponse<CartVO> selectOrUnSelect(Integer userId, Integer productId, Integer checked);

	ServerResponse<Integer> getCartProductCount(Integer userId);
}
