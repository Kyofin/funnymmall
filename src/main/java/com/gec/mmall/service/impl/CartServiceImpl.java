package com.gec.mmall.service.impl;

import com.gec.mmall.common.Const;
import com.gec.mmall.common.ResponseCode;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.dao.CartMapper;
import com.gec.mmall.dao.ProductMapper;
import com.gec.mmall.pojo.Cart;
import com.gec.mmall.pojo.Product;
import com.gec.mmall.service.ICartService;
import com.gec.mmall.util.BigDecimalUtil;
import com.gec.mmall.util.PropertiesUtil;
import com.gec.mmall.vo.CartProductVO;
import com.gec.mmall.vo.CartVO;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service("iCartService")
public class CartServiceImpl implements ICartService {

	@Autowired
	private CartMapper cartMapper;
	@Autowired
	private ProductMapper productMapper;

	/**
	 * 添加商品到购物车
	 * @param userId
	 * @param productId
	 * @param count
	 * @return
	 */
	@Override
	public ServerResponse add(Integer userId, Integer productId, Integer count){
		if (productId == null || count == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
		}

		Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);
		if (cart == null) {
			//这个产品不在这个购物车中，需要新增一个这个产品的记录
			Cart cartItem = new Cart();
			cartItem.setQuantity(count);
			cartItem.setChecked(Const.Cart.CHECKED);
			cartItem.setProductId(productId);
			cartItem.setUserId(userId);
			cartMapper.insert(cartItem);
		} else {
			//这个产品已经在购物车中
			//这个产品已经存在，数量相加
			count = cart.getQuantity() +count;
			cart.setQuantity(count);
			cartMapper.updateByPrimaryKeySelective(cart);
		}
		return this.list(userId);


	}

	/**
	 * 更新购物车
	 * @param userId
	 * @param productId
	 * @param count
	 * @return
	 */
	@Override
	public ServerResponse update(Integer userId, Integer productId, Integer count){
		if (productId == null || count == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
		}

		Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);
		if (cart != null) {
			//更新数量
			cart.setQuantity(count);
		}
		cartMapper.updateByPrimaryKeySelective(cart);
		return this.list(userId);

	}

	/**
	 * 删除购物车中指定商品
	 * @param userId
	 * @param productIds
	 * @return
	 */
	@Override
	public ServerResponse<CartVO> deleteProduct(Integer userId, String productIds){
		//通过guava将productIds分割字符串集合
		List<String> productIdList = Splitter.on(",").splitToList(productIds);
		if (CollectionUtils.isEmpty(productIdList)){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
		}
		cartMapper.deleteByUserIdProductIds(userId,productIdList);
		return this.list(userId);

	}

	/**
	 * 查看整个购物车
	 * @param userId
	 * @return
	 */
	@Override
	public ServerResponse<CartVO> list(Integer userId){
		CartVO cartVO = this.getCartVOLimit(userId);
		return ServerResponse.createBySuccess(cartVO);
	}



	/**
	 * productId为null时，用于全选或者不全选
	 * productId不为null时，用于单选或者反选
	 */
	@Override
	public ServerResponse<CartVO> selectOrUnSelect(Integer userId,Integer productId,Integer checked) {
		cartMapper.checkedOrUnCheckedProduct(userId,productId,checked);
		return this.list(userId);
	}

	/**
	 * 查询当前用户的购物车里面的产品数量，如果一个产品有10个，那么数量就是10
	 * @param userId
	 * @return
	 */
	@Override
	public ServerResponse<Integer> getCartProductCount(Integer userId){
		if (userId == null){
			return ServerResponse.createBySuccess(0);
		}
		return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
	}


	/**
	 * 获取返回前端的(CartVOLimit)整个购物车对象（高复用）
	 * 1校验库存，更新购物车db
	 * 2计算购物车单品总价
	 * 3判断购物车单品是否勾选，是的话添加到总价
	 * 4计算购物车总价
	 * @param userId
	 * @return
	 */
	private CartVO getCartVOLimit(Integer userId){
		CartVO cartVO = new CartVO();
		List<Cart> cartList = cartMapper.selectCartByUserId(userId);
		List<CartProductVO> cartProductVOList = Lists.newArrayList();

		BigDecimal cartTotalPrice = new BigDecimal("0");

		if (CollectionUtils.isNotEmpty(cartList)){
			for (Cart cartItem : cartList) {
				//获取购物车信息，封装到cartProductVO中
				CartProductVO cartProductVO = new CartProductVO();
				cartProductVO.setId(cartItem.getId());
				cartProductVO.setUserId(cartItem.getUserId());
				cartProductVO.setProductId(cartItem.getProductId());

				//获取产品信息，封装到cartProductVO中
				Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
				if (product != null) {
					cartProductVO.setProductMainImage(product.getMainImage());
					cartProductVO.setProductName(product.getName());
					cartProductVO.setProductSubTitle(product.getSubtitle());
					cartProductVO.setProductStatus(product.getStatus());
					cartProductVO.setProductPrice(product.getPrice());
					cartProductVO.setProductStock(product.getStock());
					//判断购物车产品购买数量是否大于库存
					int buyLimitCount = 0 ;
					if (product.getStock() >= cartItem.getQuantity()){
						//库存充足的时候
						buyLimitCount = cartItem.getQuantity();
						cartProductVO.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
					}else {
						//库存不充足时，购买数量设置为最大库存
						buyLimitCount = product.getStock();
						cartProductVO.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
						//购物车中更新有效库存
						Cart cartForQuantity = new Cart();
						cartForQuantity.setId(cartItem.getId());
						cartForQuantity.setQuantity(buyLimitCount);
						cartMapper.updateByPrimaryKeySelective(cartForQuantity);
					}
					cartProductVO.setQuantity(buyLimitCount);
					//计算该购物车商品的总价
					cartProductVO.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartProductVO.getQuantity()));
					cartProductVO.setProductChecked(cartItem.getChecked());
				}

				if (cartItem.getChecked() == Const.Cart.CHECKED) {
					//如果已经勾选，增加到整个购物车总价中
					cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(),cartProductVO.getProductTotalPrice().doubleValue());
				}
				cartProductVOList.add(cartProductVO);
			}
		}
		//封装cartVO返回
		cartVO.setCartTotalPrice(cartTotalPrice);
		cartVO.setCartProductVoList(cartProductVOList);
		cartVO.setAllChecked(this.getAllCheckedStatus(userId));
		cartVO.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

		return cartVO;
	}

	/**
	 * 检查购物车是否全选
	 * @param userId
	 * @return
	 */
	private boolean getAllCheckedStatus(Integer userId){
		if (userId == null) {
			return false;
		}
		return cartMapper.selectCartProductCheckStatusByUserId(userId) == 0;
	}
}
