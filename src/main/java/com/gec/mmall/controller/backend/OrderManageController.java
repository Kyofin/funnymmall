package com.gec.mmall.controller.backend;

import com.gec.mmall.common.Const;
import com.gec.mmall.common.ResponseCode;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.User;
import com.gec.mmall.service.IOrderService;
import com.gec.mmall.service.IUserService;
import com.gec.mmall.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/manage/order")
public class OrderManageController {

	@Autowired
	private IUserService iUserService;
	@Autowired
	private IOrderService iOrderService;

	@RequestMapping("list.do")
	@ResponseBody
	public ServerResponse orderList(HttpSession session,
									@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
									@RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
		}
		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			//是管理员
			return iOrderService.manageList(pageNum,pageSize );
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}
	}


	@RequestMapping("search.do")
	@ResponseBody
	public ServerResponse orderSearch(HttpSession session,
									Long orderNo,
									@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
									@RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
		}
		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			//是管理员
			return iOrderService.manageSearch(orderNo ,pageNum,pageSize );
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}
	}

	@RequestMapping("send_goods.do")
	@ResponseBody
	public ServerResponse orderSendGoods(HttpSession session, Long orderNo){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
		}
		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			//是管理员
			return iOrderService.manageSendGoods(orderNo );
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}
	}

	@RequestMapping("detail.do")
	@ResponseBody
	public ServerResponse<OrderVO> orderDetail(HttpSession session, Long orderNo){

		User user = (User)session.getAttribute(Const.CURRENT_USER);
		if(user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录,请登录管理员");

		}
		if(iUserService.checkAdminRole(user).isSuccess()){
			//填充我们增加产品的业务逻辑

			return iOrderService.manageDetail(orderNo);
		}else{
			return ServerResponse.createByErrorMessage("无权限操作");
		}
	}


}
