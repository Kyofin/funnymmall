package com.gec.mmall.controller.portal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	public Object login(String username, String password, HttpSession session)
	{
		//service -> dao ->mybatis
		return null;
	}

}
