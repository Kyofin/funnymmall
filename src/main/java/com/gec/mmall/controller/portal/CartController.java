package com.gec.mmall.controller.portal;

import com.gec.mmall.common.Const;
import com.gec.mmall.common.ResponseCode;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.User;
import com.gec.mmall.service.ICartService;
import com.gec.mmall.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

	@Autowired
	ICartService iCartService;

	@RequestMapping("add.do")
	@ResponseBody
	public ServerResponse<CartVO> add(HttpSession session, Integer count, Integer productId){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.add(user.getId(),productId,count);
	}

	@RequestMapping("update.do")
	@ResponseBody
	public ServerResponse<CartVO> update(HttpSession session, Integer count, Integer productId){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.update(user.getId(),productId,count);
	}

	@RequestMapping("delete_product.do")
	@ResponseBody
	public ServerResponse<CartVO> deleteProduct(HttpSession session,String productIds){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.deleteProduct(user.getId(),productIds);
	}


	@RequestMapping("select_all.do")
	@ResponseBody
	public ServerResponse<CartVO> selectAll(HttpSession session){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.selectOrUnSelect(user.getId(),null,Const.Cart.CHECKED);
	}

	@RequestMapping("un_select_all.do")
	@ResponseBody
	public ServerResponse<CartVO> unSelectAll(HttpSession session){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.selectOrUnSelect (user.getId(),null,Const.Cart.UN_CHECKED);
	}

	@RequestMapping("un_select.do")
	@ResponseBody
	public ServerResponse<CartVO> unSelect(HttpSession session,Integer productId){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.selectOrUnSelect (user.getId(),productId,Const.Cart.UN_CHECKED);
	}

	@RequestMapping("select.do")
	@ResponseBody
	public ServerResponse<CartVO> select(HttpSession session,Integer productId){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.selectOrUnSelect (user.getId(),productId,Const.Cart.CHECKED);
	}

	@RequestMapping("get_cart_product_count.do")
	@ResponseBody
	public ServerResponse<Integer> getCartProductCount(HttpSession session){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.getCartProductCount (user.getId());
	}

	@RequestMapping("list.do")
	@ResponseBody
	public ServerResponse<CartVO> list(HttpSession session){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
		}
		return iCartService.list (user.getId());
	}
}
