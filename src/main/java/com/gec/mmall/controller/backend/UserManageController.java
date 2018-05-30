package com.gec.mmall.controller.backend;

import com.gec.mmall.common.Const;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.User;
import com.gec.mmall.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/manage/user")
public class UserManageController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserManageController.class);

	@Autowired
	private IUserService iUserService;

	@RequestMapping(value = "login.do",method = RequestMethod.POST)
	@ResponseBody
	public ServerResponse<User> login(String username, String password, HttpSession session){
		ServerResponse<User> response = iUserService.login(username,password);
		if (response.isSuccess()){
			User user = response.getData();
			if (user.getRole() == Const.Role.ROLE_ADMIN){
				//说明登录的是管理员,将用户保存到session
				session.setAttribute(Const.CURRENT_USER,user);
				LOGGER.info("登录用户：{}",user.toString());;
				return response;
			}else {
				return ServerResponse.createByErrorMessage("不是管理员，无法登录");
			}
		}
		return response;
	}
}
