package com.gec.mmall.service;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.vo.OrderVO;
import com.github.pagehelper.PageInfo;

import java.util.Map;

public interface IOrderService {

	ServerResponse createOrder(Integer userId, Integer shippingId);

	ServerResponse pay(Long orderNo, Integer userId, String path);

	ServerResponse aliCallBack(Map<String, String> params);

	ServerResponse queryOrderPayStatus(Integer userId, Long orderNo);

	ServerResponse<OrderVO> getOrderDetail(Integer userId, Long orderNo);

	ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize);

	ServerResponse<String> cancel(Integer userId, Long orderNo);

	ServerResponse getOrderCartProduct(Integer userId);

	ServerResponse<PageInfo> manageList(int pageNum, int pageSize);

	ServerResponse<OrderVO> manageDetail(Long orderNo);

	ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize);

	ServerResponse<String> manageSendGoods(Long orderNo);
}
