package com.gec.mmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.gec.mmall.common.Const;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.dao.*;
import com.gec.mmall.pojo.*;
import com.gec.mmall.service.IOrderService;
import com.gec.mmall.util.BigDecimalUtil;
import com.gec.mmall.util.DateTimeUtil;
import com.gec.mmall.util.FtpUtil;
import com.gec.mmall.util.PropertiesUtil;
import com.gec.mmall.vo.OrderItemVO;
import com.gec.mmall.vo.OrderProductVO;
import com.gec.mmall.vo.OrderVO;
import com.gec.mmall.vo.ShippingVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {

	private static  AlipayTradeService tradeService;
	static {
		/** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
		 *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
		 */
		Configs.init("zfbinfo.properties");

		/** 使用Configs提供的默认参数
		 *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
		 */
		 tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

	}

	private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Autowired
	private OrderMapper orderMapper;
	@Autowired
	private OrderItemMapper orderItemMapper;
	@Autowired
	private PayInfoMapper payInfoMapper;
	@Autowired
	private ProductMapper productMapper;
	@Autowired
	private CartMapper cartMapper;
	@Autowired
	private ShippingMapper shippingMapper;

	/**
	 * 创建订单
	 * @param userId
	 * @param shippingId
	 * @return
	 */
	@Override
	public ServerResponse createOrder(Integer userId, Integer shippingId) {
		//从购物车中获取数据(已勾选的)
		List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);

		//获取该订单的商品集合
		ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
		if (!serverResponse.isSuccess()) {
			return serverResponse;
		}
		List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();
		if (CollectionUtils.isEmpty(orderItemList)) {
			return ServerResponse.createByErrorMessage("购物车为空");
		}
		//计算这个订单的总价
		BigDecimal payment = this.getOrderTotalPrice(orderItemList);

		//生成订单
		Order order = this.assembleOrder(userId, shippingId, payment);
		if (order == null) {
			return ServerResponse.createByErrorMessage("生成订单错误");
		}
		//为订单明细的每个商品附上订单号
		for (OrderItem orderItem : orderItemList) {
			orderItem.setOrderNo(order.getOrderNo());
		}
		//mybatis批量插入，持久化orderItem（ok）
		orderItemMapper.batchInsert(orderItemList);

		//生成成功，我们要减少产品的库存（ok）
		this.reduceProductStock(orderItemList);

		//清空一下购物车
		this.cleanCart(cartList);

		//返回前端数据
		OrderVO orderVO = assembleOrderVO(order, orderItemList);
		return ServerResponse.createBySuccess(orderVO);

	}

	/**
	 * 组装OrderVO，最终返回前端的对象
	 * (包括订单信息，商品明细，收货地址信息，总价格，各种时间等等)
	 * @param order
	 * @param orderItemList
	 * @return
	 */
	private OrderVO assembleOrderVO(Order order, List<OrderItem> orderItemList) {
		OrderVO orderVO = new OrderVO();
		orderVO.setOrderNo(order.getOrderNo());
		orderVO.setPayment(order.getPayment());
		orderVO.setPaymentType(order.getPaymentType());
		orderVO.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());

		orderVO.setPostage(order.getPostage());
		orderVO.setStatus(order.getStatus());
		orderVO.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());

		//为orderVO设置shipping
		orderVO.setShippingId(order.getShippingId());
		Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
		if (shipping != null) {
			orderVO.setReceiverName(shipping.getReceiverName());
			orderVO.setShippingVO(assembleShippingVO(shipping));
		}

		orderVO.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
		orderVO.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
		orderVO.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
		orderVO.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
		orderVO.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));
		
		orderVO.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

		//为orderVO设置订单明细商品集合
		List<OrderItemVO> orderItemVOList = Lists.newArrayList();
		for (OrderItem orderItem : orderItemList) {
			OrderItemVO orderItemVO = assembleOrderItemVO(orderItem);
			orderItemVOList.add(orderItemVO);
		}
		orderVO.setOrderItemVOList(orderItemVOList);
		return orderVO;
	}

	/**
	 * 组装 OrderItemVO
	 * @param orderItem
	 * @return
	 */
	private OrderItemVO assembleOrderItemVO(OrderItem orderItem) {
		OrderItemVO orderItemVO = new OrderItemVO();
		orderItemVO.setOrderNo(orderItem.getOrderNo());
		orderItemVO.setProductId(orderItem.getProductId());
		orderItemVO.setProductName(orderItem.getProductName());
		orderItemVO.setProductImage(orderItem.getProductImage());
		orderItemVO.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
		orderItemVO.setQuantity(orderItem.getQuantity());
		orderItemVO.setTotalPrice(orderItem.getTotalPrice());

		orderItemVO.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
		return orderItemVO;
	}

	/**
	 * 组装ShippingVO
	 * @param shipping
	 * @return
	 */
	private ShippingVO assembleShippingVO(Shipping shipping) {
		ShippingVO shippingVO = new ShippingVO();
		shippingVO.setReceiverName(shipping.getReceiverName());
		shippingVO.setReceiverAddress(shipping.getReceiverAddress());
		shippingVO.setReceiverProvince(shipping.getReceiverProvince());
		shippingVO.setReceiverCity(shipping.getReceiverCity());
		shippingVO.setReceiverDistrict(shipping.getReceiverDistrict());
		shippingVO.setReceiverMobile(shipping.getReceiverMobile());
		shippingVO.setReceiverZip(shipping.getReceiverZip());
		shippingVO.setReceiverProvince(shippingVO.getReceiverPhone());
		return shippingVO;
	}

	/**
	 * 清空购物车
	 * @param cartList
	 */
	private void cleanCart(List<Cart> cartList) {
		for (Cart cart : cartList) {
			cartMapper.deleteByPrimaryKey(cart.getId());
		}
	}

	/**
	 * 减少产品库存 todo 是否需要加同步
	 * @param orderItemList
	 */
	private void reduceProductStock(List<OrderItem> orderItemList) {
		for (OrderItem orderItem : orderItemList) {
			Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
			product.setStock(product.getStock()-orderItem.getQuantity());
			productMapper.updateByPrimaryKeySelective(product);
		}
	}

	/**
	 * 生成一个订单对象，并持久化到数据库
	 * @param userId
	 * @param shippingId
	 * @param payment
	 * @return
	 */
	private Order assembleOrder(Integer userId, Integer shippingId, BigDecimal payment) {
		Order order = new Order();
		long orderNo = this.generateOrderNo();
		order.setOrderNo(orderNo);
		order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
		order.setPostage(0);//运费
		order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
		order.setPayment(payment);

		order.setUserId(userId);
		order.setShippingId(shippingId);
		//发货时间等等
		//付款时间等等
		int rowCount = orderMapper.insert(order);
		if (rowCount > 0) {
			return order;
		}
		return null;

	}

	/**
	 * 根据时间戳生成订单号
	 * @return
	 */
	private long generateOrderNo() {
		long currentTime = System.currentTimeMillis();
		return currentTime + new Random().nextInt(100);
	}

	/**
	 * 计算订单明细中各个商品合起来的总价
	 * @param orderItemList
	 * @return
	 */
	private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList) {
		BigDecimal payment = new BigDecimal("0");
		for (OrderItem orderItem : orderItemList) {
			payment = BigDecimalUtil.add(payment.doubleValue(), orderItem.getTotalPrice().doubleValue());
		}
		return payment;
	}

	/**
	 * 根据购物车组装订单的商品明细(已校验产品状态及库存)
	 * @param userId
	 * @param cartList
	 * @return
	 */
	private ServerResponse getCartOrderItem(Integer userId, List<Cart> cartList) {
		List<OrderItem> orderItemList = Lists.newArrayList();
		if (CollectionUtils.isEmpty(cartList)) {
			return ServerResponse.createByErrorMessage("购物车为空");
		}

		//检验购物车的数据，包括产品的数量和状态
		for (Cart cartItem : cartList) {
			Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
			//检查产品是否在售
			if (product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()) {
				return ServerResponse.createByErrorMessage("产品" + product.getName() + "不是在线售卖状态");
			}
			//检查产品库存是否足够
			if (cartItem.getQuantity() > product.getStock()) {
				return ServerResponse.createByErrorMessage("产品" + product.getName() + "库存不足");
			}
			//组装orderItem
			OrderItem orderItem = new OrderItem();
			orderItem.setUserId(userId);
			orderItem.setProductId(product.getId());
			orderItem.setProductName(product.getName());
			orderItem.setCurrentUnitPrice(product.getPrice());
			orderItem.setQuantity(cartItem.getQuantity());
			orderItem.setProductImage(product.getMainImage());
			orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cartItem.getQuantity()));
			orderItemList.add(orderItem);
		}
		return ServerResponse.createBySuccess(orderItemList);
	}
















	/**
	 * 支付订单（支付宝预下单，返回用户支付二维码）
	 * @param orderNo
	 * @param userId
	 * @param path
	 * @return
	 */
	@Override
	public ServerResponse pay(Long orderNo, Integer userId, String path) {
		Map<String, String> resultMap = Maps.newHashMap();
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMessage("用户没有该订单");
		}
		resultMap.put("orderNo", String.valueOf(order.getOrderNo()));

		// (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
		// 需保证商户系统端不能重复，建议通过数据库sequence生成，
		String outTradeNo = order.getOrderNo().toString();

		// (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
		String subject = new StringBuilder().append("funnymmall扫码支付，订单号：").append(outTradeNo).toString();

		// (必填) 订单总金额，单位为元，不能超过1亿元
		// 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
		String totalAmount = order.getPayment().toString();

		// (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
		// 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
		String undiscountableAmount = "0";

		// 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
		// 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
		String sellerId = "";

		// 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
		String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

		// 商户操作员编号，添加此参数可以为商户操作员做销售统计
		String operatorId = "test_operator_id";

		// (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
		String storeId = "test_store_id";

		// 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
		ExtendParams extendParams = new ExtendParams();
		extendParams.setSysServiceProviderId("2088100200300400500");

		// 支付超时，定义为120分钟
		String timeoutExpress = "120m";

		// 商品明细列表，需填写购买商品详细信息，
		List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();

		List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(userId,orderNo);
		for (OrderItem orderItem : orderItemList) {
			// 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
			GoodsDetail goods = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(),
					BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),
					orderItem.getQuantity());
			// 创建好一个商品后添加至商品明细列表
			goodsDetailList.add(goods);
		}

		// 继续创建并添加第一条商品信息，用户购买的产品为“黑人牙刷”，单价为5.00元，购买了两件
		GoodsDetail goods2 = GoodsDetail.newInstance("goods_id002", "xxx牙刷", 500, 2);
		goodsDetailList.add(goods2);

		// 创建扫码支付请求builder，设置请求参数
		AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
				.setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
				.setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
				.setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
				.setTimeoutExpress(timeoutExpress)
				.setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
				.setGoodsDetailList(goodsDetailList);


		AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
		switch (result.getTradeStatus()) {
			case SUCCESS:
				logger.info("支付宝预下单成功");

				AlipayTradePrecreateResponse response = result.getResponse();
				dumpResponse(response);

				//判断传进的目录是否存在，不存在则创建
				File folder = new File(path);
				if (!folder.exists()) {
					folder.setWritable(true);
					folder.mkdirs();
				}

				// 需要修改为运行机器上的路径
				String qrPath = String.format(path+"/qr-%s.png", response.getOutTradeNo());
				String qrFileName = String.format("qr-%s.png", response.getOutTradeNo());
				//在项目发布目录下的upload文件夹生成二维码
				ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);
				logger.info("本地生成二维码成功，qrPath:" + qrPath);
				//将二维码上传到文件服务器
				File targetFile = new File(path, qrFileName);
				List<File> fileList = Lists.newArrayList(targetFile);
				try {
					FtpUtil.uploadFile(fileList);
				} catch (IOException e) {
					logger.error("上传二维码异常",e);
				}

				String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFile.getName();
				resultMap.put("qrUrl", qrUrl);
				return ServerResponse.createBySuccess(resultMap);

			case FAILED:
				logger.error("支付宝预下单失败!!!");
				return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

			case UNKNOWN:
				logger.error("系统异常，预下单状态未知!!!");
				return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

			default:
				logger.error("不支持的交易状态，交易返回异常!!!");
				return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
		}

	}

	/**
	 * 简单打印应答
	 */
	private void dumpResponse(AlipayResponse response) {
		if (response != null) {
			logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
			if (StringUtils.isNotEmpty(response.getSubCode())) {
				logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
						response.getSubMsg()));
			}
			logger.info("body:" + response.getBody());
		}
	}

	@Override
	public ServerResponse aliCallBack(Map<String, String> params) {
		//商城应用的订单号
		Long orderNo = Long.parseLong(params.get("out_trade_no"));
		//支付宝交易号
		String tradeNo = params.get("trade_no");
		//交易状态
		String tradeStatus = params.get("trade_status");
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMessage("非本商城的订单，忽略该支付宝回调");
		}
		if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
			return ServerResponse.createBySuccessMessage("支付宝重复调用");
		}
		if (Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)) {
			//更新交易付款时间
			order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
			//更新订单状态
			order.setStatus(Const.OrderStatusEnum.PAID.getCode());
			orderMapper.updateByPrimaryKeySelective(order);
		}

		PayInfo payInfo = new PayInfo();
		payInfo.setUserId(order.getUserId());
		payInfo.setOrderNo(order.getOrderNo());
		payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
		payInfo.setPlatformNumber(tradeNo);
		payInfo.setPlatformStatus(tradeStatus);
		payInfoMapper.insert(payInfo);

		return ServerResponse.createBySuccess();
	}


	/**
	 * 查询订单状态
	 * @param userId
	 * @param orderNo
	 * @return
	 */
	@Override
	public ServerResponse queryOrderPayStatus(Integer userId, Long orderNo) {
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMessage("用户没有该订单");
		}
		if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
			return ServerResponse.createBySuccess();
		}
		return ServerResponse.createByError();
	}

	/**
	 * 获取订单详情
	 * @param userId
	 * @param orderNo
	 * @return
	 */
	@Override
	public ServerResponse<OrderVO> getOrderDetail(Integer userId, Long orderNo) {
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
		if (order != null) {
			//获取订单商品明细
			List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(userId, orderNo);
			OrderVO orderVO = assembleOrderVO(order, orderItemList);
			return ServerResponse.createBySuccess(orderVO);
		}
		return ServerResponse.createByErrorMessage("没有该订单");
	}


	/**
	 * 用户获取自己的订单列表
	 * @param userId
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	@Override
	public ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		List<Order> orderList = orderMapper.selectByUserId(userId);
		List<OrderVO> orderVOList = assembleOrderVOList(orderList, userId);
		PageInfo pageInfo = new PageInfo(orderList);
		pageInfo.setList(orderVOList);
		return ServerResponse.createBySuccess(pageInfo);
	}

	/**
	 * 组装订单列表的VO
	 * (OrderVOList)
	 * @param orderList
	 * @param userId
	 * @return
	 */
	private List<OrderVO> assembleOrderVOList(List<Order> orderList, Integer userId) {
		List<OrderVO> orderVOList = Lists.newArrayList();
		for (Order order : orderList) {
			//获取每个订单对应的商品明细
			List<OrderItem> orderItemList = Lists.newArrayList();
			if (userId == null) {
				//管理员查询的时候，不需要传userId
				orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
			} else {
				orderItemList = orderItemMapper.getByOrderNoUserId(userId,order.getOrderNo());
			}
			OrderVO orderVO = assembleOrderVO(order, orderItemList);
			orderVOList.add(orderVO);
		}
		return orderVOList;
	}

	/**
	 * 取消订单
	 * @param userId
	 * @param orderNo
	 * @return
	 */
	@Override
	public ServerResponse<String> cancel(Integer userId, Long orderNo) {
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMessage("该用户此订单不存在");
		}
		//只有未支付状态的订单才可以取消
		if (order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()) {
			return ServerResponse.createByErrorMessage("已付款，无法取消订单");
		}
		Order updateOrder = new Order();
		updateOrder.setId(order.getId());
		updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode());

		int rowCount = orderMapper.updateByPrimaryKeySelective(updateOrder);
		if (rowCount > 0) {
			return ServerResponse.createBySuccess();
		}
		return ServerResponse.createByError();
	}


	/**
	 * 展示正在填写订单时,用户从购物车中选择的商品信息
	 * （该订单的商品明细信息orderItem未持久化）
	 *
	 */
	@Override
	public ServerResponse getOrderCartProduct(Integer userId) {
		OrderProductVO orderProductVO = new OrderProductVO();

		//从购物车中获取已勾选的数据
		List<Cart> cartList = cartMapper.selectCartByUserId(userId);
		ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
		if (!serverResponse.isSuccess()) {
			return serverResponse;
		}
		List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();

		List<OrderItemVO> orderItemVOList = Lists.newArrayList();

		BigDecimal payment = new BigDecimal("0");
		for (OrderItem orderItem : orderItemList) {
			payment = BigDecimalUtil.add(payment.doubleValue(), orderItem.getTotalPrice().doubleValue());
			orderItemVOList.add(assembleOrderItemVO(orderItem));
		}
		orderProductVO.setProductTotalPrice(payment);
		orderProductVO.setOrderItemVoList(orderItemVOList);
		orderProductVO.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

		return serverResponse.createBySuccess(orderProductVO);
	}












	//backend

	/**
	 * 管理员获取订单列表（所有）
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	@Override
	public ServerResponse<PageInfo> manageList(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		//管理员获取所有db中的订单
		List<Order> orderList = orderMapper.selectAllOrder();
		//组装返回前端的订单列表
		List<OrderVO> orderVOList = this.assembleOrderVOList(orderList, null);
		PageInfo pageInfo = new PageInfo(orderList);
		pageInfo.setList(orderVOList);
		return ServerResponse.createBySuccess(pageInfo);
	}

	/**
	 * 管理员查看订单详情
	 * @param orderNo
	 * @return
	 */
	@Override
	public ServerResponse<OrderVO> manageDetail(Long orderNo) {
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order != null) {
			List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
			OrderVO orderVO = assembleOrderVO(order, orderItemList);
			return ServerResponse.createBySuccess(orderVO);
		}
		return ServerResponse.createByErrorMessage("订单不存在");
	}


	/**
	 * 管理员根据订单号查询订单
	 * @param orderNo
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	@Override
	public ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order != null) {
			List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
			OrderVO orderVO = assembleOrderVO(order, orderItemList);
			//todo 一期只做精确匹配 ，二期要加上多条件模糊匹配
			PageInfo pageInfo = new PageInfo(Lists.newArrayList(order));
			pageInfo.setList(Lists.newArrayList(orderVO));
			return ServerResponse.createBySuccess(pageInfo);
		}
		return ServerResponse.createByErrorMessage("订单不存在");
	}

	/**
	 * 管理员发货
	 *
	 * @param orderNo
	 * @return
	 */
	@Override
	public ServerResponse<String> manageSendGoods(Long orderNo) {
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order != null) {
			//判断是否为已付款状态，是就改成发货
			if (order.getStatus() == Const.OrderStatusEnum.PAID.getCode()) {
				order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
				order.setSendTime(new Date());
				orderMapper.updateByPrimaryKeySelective(order);
				return ServerResponse.createBySuccess("发货成功");
			}
		}
		return ServerResponse.createByErrorMessage("订单不存在");
	}

}
