package com.gec.mmall.controller.backend;

import com.gec.mmall.common.Const;
import com.gec.mmall.common.ResponseCode;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.Product;
import com.gec.mmall.pojo.User;
import com.gec.mmall.service.IFileService;
import com.gec.mmall.service.IProductService;
import com.gec.mmall.service.IUserService;
import com.gec.mmall.util.PropertiesUtil;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/manage/product")
public class ProductManageController {

	@Autowired
	private IUserService iUserService;
	@Autowired
	private IProductService iProductService;
	@Autowired
	private IFileService iFileService;

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

	@RequestMapping("search.do")
	@ResponseBody
	public ServerResponse productSearch(HttpSession session,
								  String productName,
								  Integer productId,
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
			return iProductService.searchProduct(productName,productId,pageNum,pageSize);
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}
	}

	@RequestMapping("upload.do")
	@ResponseBody
	public  ServerResponse upload(HttpSession session,@RequestParam("upload_file") MultipartFile file, HttpServletRequest request){
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请登录");
		}
		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			//填充业务
			//该路径相当于部署项目的tomact下的webapp目录下创建的upload文件夹
			String path = request.getSession().getServletContext().getRealPath("upload");
			String targetFileName = iFileService.upload(file, path);//todo 检查为null情况
			if (StringUtils.isBlank(targetFileName)){
				return ServerResponse.createByErrorMessage("上传失败");
			}
			//返回成功结果包括uri和url
			String url = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFileName;
			Map fileInfoMap = Maps.newHashMap();
			fileInfoMap.put("uri",targetFileName);
			fileInfoMap.put("url",url);
			return ServerResponse.createBySuccess(fileInfoMap);
		}else {
			return ServerResponse.createByErrorMessage("无权操作，需要管理员权限");
		}

	}



	//富文本中对于返回值有自己的要求,我们使用是simditor所以按照simditor的要求进行返回
//        {
//            "success": true/false,
//                "msg": "error message", # optional
//            "file_path": "[real file path]"
//        }
	@RequestMapping("richtext_img_upload.do")
	@ResponseBody
	public  Map upload(HttpSession session, @RequestParam("upload_file") MultipartFile file, HttpServletRequest request, HttpServletResponse response){
		Map resultMap = Maps.newHashMap();
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null){
			resultMap.put("success",false);
			resultMap.put("msg","请先登录账号");
			return resultMap;
		}

		//检验是否管理员
		ServerResponse checkAdminRoleResponse = iUserService.checkAdminRole(user);
		if (checkAdminRoleResponse.isSuccess()){
			//填充业务
			//该路径相当于部署项目的tomact下的webapp目录下创建的upload文件夹
			String path = request.getSession().getServletContext().getRealPath("upload");
			String targetFileName = iFileService.upload(file, path);//todo 检查为null情况

			if (StringUtils.isBlank(targetFileName)){
				resultMap.put("success",false);
				resultMap.put("msg","上传失败");
				return resultMap;
			}


			String url = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFileName;

			resultMap.put("success",true);
			resultMap.put("msg","上传成功");
			resultMap.put("file_path",url);

			response.addHeader("Access-Control-Allow-Headers","X-File-Name");

			return resultMap;
		}else {
			resultMap.put("success",false);
			resultMap.put("msg","不是管理员，无权限操作");
			return resultMap;
		}

	}
}
