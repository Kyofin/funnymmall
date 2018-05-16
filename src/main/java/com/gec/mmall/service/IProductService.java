package com.gec.mmall.service;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.Product;
import com.github.pagehelper.PageInfo;

public interface IProductService {
	ServerResponse saveOrUpdateProduct(Product product);

	ServerResponse setSaleStatus(Integer productId, Integer status);

	ServerResponse manageProductDetail(Integer productId);

	ServerResponse<PageInfo> getProductList(int pageNum, int pageSize);
}
