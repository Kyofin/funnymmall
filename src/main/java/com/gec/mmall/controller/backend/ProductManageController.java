package com.gec.mmall.controller.backend;

import com.gec.mmall.common.Const;
import com.gec.mmall.common.ResponseCode;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.Product;
import com.gec.mmall.pojo.User;
import com.gec.mmall.service.IProductService;
import com.gec.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/manage/product")
public class ProductManageController {

	@Autowired
	private IUserService iUserService;
	@Autowired
	private IProductService iProductService;

	@RequestMapping("save.do")
	@ResponseBody
	public ServerResponse productSave(HttpSession session, Product product){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
		}
		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			//填充我们增加产品的业务逻辑
			return iProductService.saveOrUpdateProduct(product);
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}
	}


	@RequestMapping("set_sale_status.do")
	@ResponseBody
	public ServerResponse setSaleStatus(HttpSession session,
										Integer productId,
										Integer status){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
		}
		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			return iProductService.setSaleStatus(productId,status);
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}
	}

	@RequestMapping("detail.do")
	@ResponseBody
	public ServerResponse getDetail(HttpSession session, Integer productId){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
		}
		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			//填充业务
			return iProductService.manageProductDetail(productId);
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}
	}

	@RequestMapping("list.do")
	@ResponseBody
	public ServerResponse getList(HttpSession session,
								  @RequestParam(value = "pageNum",defaultValue = "1")int pageNum ,
								  @RequestParam(value = "pageSize",defaultValue = "10")int pageSize){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
		}
		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			//填充业务
			return iProductService.getProductList(pageNum,pageSize);
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}
	}
}
