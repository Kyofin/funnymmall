package com.gec.mmall.service.impl;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.dao.ShippingMapper;
import com.gec.mmall.pojo.Shipping;
import com.gec.mmall.service.IShippingService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService {

	@Autowired
	private ShippingMapper shippingMapper;


	/**
	 * 添加地址
	 * @param userId
	 * @param shipping
	 * @return
	 */
	@Override
	public ServerResponse add(Integer userId, Shipping shipping){
		shipping.setUserId(userId);
		int rowCount = shippingMapper.insert(shipping);
		if (rowCount > 0) {
			Map result = Maps.newHashMap();
			result.put("shippingId", shipping.getId());
			return ServerResponse.createBySuccess("新建地址成功",result);
		}
		return ServerResponse.createByErrorMessage("新建地址失败");
	}


	@Override
	public ServerResponse del(Integer userId, Integer shippingId){
		int resultCount = shippingMapper.deleteByShippingIdUserId(userId,shippingId);
		if (resultCount > 0) {
			return ServerResponse.createBySuccess("删除地址成功");
		}
		return ServerResponse.createByErrorMessage("删除地址失败");
	}

	@Override
	public ServerResponse update(Integer userId, Shipping shipping) {
		//从session获取userId，防止横向越权
		shipping.setUserId(userId);
		int rowCount = shippingMapper.updateByShipping(shipping);
		if (rowCount > 0) {
			return ServerResponse.createBySuccess("更新地址成功");
		}
		return ServerResponse.createByErrorMessage("更新地址失败");
	}

	/**
	 * 查看地址
	 * @param userId
	 * @param shippingId
	 * @return
	 */
	@Override
	public ServerResponse<Shipping> select(Integer userId, Integer shippingId) {

		Shipping shipping = shippingMapper.selectByShippingIdUserId(userId,shippingId);
		if (shipping  == null) {
			return ServerResponse.createByErrorMessage("无法查询到该地址");
		}
		return ServerResponse.createBySuccess("查询该地址成功 ",shipping);
	}

	/**
	 * 分页查看所有地址
	 * @param userId
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	@Override
	public ServerResponse list(Integer userId, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		List<Shipping> shippingList = shippingMapper.selectByUserId(userId);
		PageInfo pageInfo = new PageInfo(shippingList);
		return ServerResponse.createBySuccess(pageInfo);
	}



}
