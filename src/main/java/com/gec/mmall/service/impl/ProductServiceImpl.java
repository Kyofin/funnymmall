package com.gec.mmall.service.impl;

import com.gec.mmall.common.ResponseCode;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.dao.CategoryMapper;
import com.gec.mmall.dao.ProductMapper;
import com.gec.mmall.pojo.Category;
import com.gec.mmall.pojo.Product;
import com.gec.mmall.service.IProductService;
import com.gec.mmall.util.DateTimeUtil;
import com.gec.mmall.util.PropertiesUtil;
import com.gec.mmall.vo.ProductDetailVO;
import com.gec.mmall.vo.ProductListVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("iProductService")
public class ProductServiceImpl implements IProductService {

	@Autowired
	private ProductMapper productMapper;

	@Autowired
	private CategoryMapper categoryMapper;

	/**
	 * 新增或更新产品
	 * @param product
	 * @return
	 */
	@Override
	public ServerResponse saveOrUpdateProduct(Product product) {
		if (product != null) {
			//判断子图是否存在
			if (StringUtils.isNotBlank(product.getSubImages())) {
				String[] subImageArray = product.getSubImages().split(",");
				//用子图第一张更新主图
				if (subImageArray.length > 0) {
					product.setMainImage(subImageArray[0]);
				}
			}
			//判断id是否存在
			if (product.getId() != null) {
				//更新
				int resultCount = productMapper.updateByPrimaryKey(product);
				if (resultCount > 0) {
					return ServerResponse.createBySuccessMessage("更新产品成功");
				}
				return ServerResponse.createBySuccessMessage("更新产品失败");
			} else {
				//新增
				int resultCount = productMapper.insert(product);
				if (resultCount > 0) {
					return ServerResponse.createBySuccessMessage("新增产品成功");
				}
				return ServerResponse.createBySuccessMessage("新增产品失败");
			}
		}
		return ServerResponse.createByErrorMessage("新增或更新产品参数不正确");
	}


	/**
	 * 产品上下架操作
	 * @param productId
	 * @param status
	 * @return
	 */
	@Override
	public ServerResponse setSaleStatus(Integer productId, Integer status){
		if (productId == null || status == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
		}
		Product product = new Product();
		product.setId(productId);
		product.setStatus(status);
		int resultCount = productMapper.updateByPrimaryKeySelective(product);
		if (resultCount > 0){
			return ServerResponse.createBySuccessMessage("修改产品销售状况成功");
		}
		return ServerResponse.createByErrorMessage("修改产品销售状况失败");
	}

	/**
	 * 后台查看产品详情
	 * @param productId
	 * @return
	 */
	@Override
	public ServerResponse<ProductDetailVO> manageProductDetail(Integer productId) {
		if (productId == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
		}
		Product product = productMapper.selectByPrimaryKey(productId);
		if (product == null){
			return ServerResponse.createByErrorMessage("产品已下架或者删除");//todo 区别产品状态
		}
		ProductDetailVO productDetailVO = assembleProductDetailVO(product);
		return ServerResponse.createBySuccess(productDetailVO);
	}

	/**
	 * 组装ProductDetailVO
	 * @param product
	 * @return
	 */
	private ProductDetailVO assembleProductDetailVO(Product product){
		ProductDetailVO productDetailVO = new ProductDetailVO();
		productDetailVO.setId(product.getId());
		productDetailVO.setSubTitle(product.getSubtitle());
		productDetailVO.setPrice(product.getPrice());
		productDetailVO.setMainImage(product.getMainImage());
		productDetailVO.setSubImage(product.getSubImages());
		productDetailVO.setCategoryId(product.getCategoryId());
		productDetailVO.setDetail(product.getDetail());
		productDetailVO.setName(product.getName());
		productDetailVO.setStatus(product.getStatus());
		productDetailVO.setStock(product.getStock());

		productDetailVO.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

		Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
		if (category == null){
			productDetailVO.setParentCategoryId(0);//默认根节点
		}else {
			productDetailVO.setParentCategoryId(category.getParentId());
		}

		//毫秒数转固定格式时间字符串
		 productDetailVO.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
		productDetailVO.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
		return productDetailVO;
	}


	@Override
	public ServerResponse<PageInfo> getProductList(int pageNum,int pageSize){
		//startPage----start
		PageHelper.startPage(pageNum,pageSize);
		//填充自己的sql查询逻辑
		List<Product> productList = productMapper.selectList();

		List<ProductListVO> productListVOList = Lists.newArrayList();
		for (Product productItem : productList) {
			ProductListVO productListVO = assembleProductListVO(productItem);
			productListVOList.add(productListVO);
		}
		//pageHelper收尾
		PageInfo pageResult = new PageInfo(productList);
		pageResult.setList(productListVOList);
		return ServerResponse.createBySuccess(pageResult);
	}

	/**
	 * 组装ProductListVO
	 * @param product
	 * @return
	 */
	private ProductListVO assembleProductListVO(Product product){
		ProductListVO productListVO = new ProductListVO();
		productListVO.setId(product.getId());
		productListVO.setName(product.getName());
		productListVO.setCategoryId(product.getCategoryId());
		productListVO.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
		productListVO.setMainImage(product.getMainImage());
		productListVO.setPrice(product.getPrice());
		productListVO.setSubtitle(product.getSubtitle());
		productListVO.setStatus(product.getStatus());
		return productListVO;
	}
}
