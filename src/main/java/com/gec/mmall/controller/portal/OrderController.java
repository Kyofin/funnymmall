package com.gec.mmall.controller.portal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.gec.mmall.common.Const;
import com.gec.mmall.common.ResponseCode;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.dao.OrderItemMapper;
import com.gec.mmall.pojo.User;
import com.gec.mmall.service.IOrderService;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/order")
public class OrderController {

	private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

	@Autowired
	private IOrderService iOrderService;


	@RequestMapping("create.do")
	@ResponseBody
	public ServerResponse create(HttpSession session,Integer shippingId){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}

		return iOrderService.createOrder(user.getId(), shippingId);
	}

	/**
	 * 未付款时取消订单
	 * @param session
	 * @param orderNo
	 * @return
	 */
	@RequestMapping("cancel.do")
	@ResponseBody
	public ServerResponse cancel(HttpSession session,Long orderNo){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}

		return iOrderService.cancel(user.getId(), orderNo);
	}


	@RequestMapping("get_order_cart_product.do")
	@ResponseBody
	public ServerResponse getOrderCartProduct(HttpSession session,Long orderNo){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}

		return iOrderService.getOrderCartProduct(user.getId());
	}

	@RequestMapping("detail.do")
	@ResponseBody
	public ServerResponse detail(HttpSession session,Long orderNo){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}

		return iOrderService.getOrderDetail(user.getId(),orderNo );
	}

	@RequestMapping("list.do")
	@ResponseBody
	public ServerResponse lis(HttpSession session,
							  @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
							  @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}

		return iOrderService.getOrderList(user.getId(),pageNum,pageSize );
	}















	@RequestMapping("pay.do")
	@ResponseBody
	public ServerResponse pay(HttpSession session, Long orderNo, HttpServletRequest request){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		String path = request.getSession().getServletContext().getRealPath("upload");
		return iOrderService.pay(orderNo,user.getId(),path);
	}


	/**
	 	\\\\ 支付宝回调请求的接口 \\\\
	 *
	 * 程序执行完后必须打印输出“success”（不包含引号）。
	  如果商户反馈给支付宝的字符不是success这7个字符，支付宝服务器会不断重发通知，直到超过24小时22分钟。
	  一般情况下，25小时以内完成8次通知（通知的间隔频率一般是：4m,10m,10m,1h,2h,6h,15h）；
	 * @param request
	 * @return
	 */
	@RequestMapping("alipay_callback.do")
	@ResponseBody
	public Object alipayCallback(HttpServletRequest request){
		logger.info("接收到支付宝回调");
		//获取支付宝回调的所有请求参数
		Map<String, String> params = Maps.newHashMap();
		Map requestParams = request.getParameterMap();
		Iterator iterator = requestParams.keySet().iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			String[] value = (String[]) requestParams.get(name);
			String valueStr = "";
			for (int i = 0 ;i<value.length;i++) {
				//数组的最后一个后面不要追加逗号
				valueStr = (i == value.length - 1) ? valueStr + value[i] : valueStr + value[i] + ",";
			}
			params.put(name, valueStr);
		}
		/*for(Iterator ite = requestParams.keySet().iterator();ite.hasNext();) {
			String name = (String) iterator.next();
			String[] value = (String[]) requestParams.get(name);
			String valueStr = "";
			for (int i = 0 ;i<value.length;i++) {
				//数组的最后一个后面不要追加逗号
				valueStr = (i == value.length - 1) ? valueStr + value[i] : valueStr + value[i] + ",";
			}
			params.put(name, valueStr);
		}*/
		logger.info("支付宝回调请求参数：{}",params.toString());

		params.remove("sign_type");
		//验签
		try {
			boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(), "utf-8", Configs.getSignType());
			if (!alipayRSACheckedV2) {
				return ServerResponse.createByErrorMessage("非法请求，再恶意请求我就报警了");
			}

		} catch (AlipayApiException e) {
			logger.error("支付宝回调验签异常",e);
		}

		//todo 验证各种数据


		ServerResponse serverResponse = iOrderService.aliCallBack(params);
		if (serverResponse.isSuccess()) {
			return Const.AlipayCallback.RESPONSE_SUCCESS;
		}
		return Const.AlipayCallback.RESPONSE_FAILED;
	}



	@RequestMapping("query_order_pay_status.do")
	@ResponseBody
	public ServerResponse<Boolean> queryOrderPayStatus(HttpSession session, Long orderNo){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}

		ServerResponse serverResponse = iOrderService.queryOrderPayStatus(user.getId(),orderNo);
		if (serverResponse.isSuccess()) {
			return ServerResponse.createBySuccess(true);
		}
		return ServerResponse.createBySuccess(false);
	}
}
