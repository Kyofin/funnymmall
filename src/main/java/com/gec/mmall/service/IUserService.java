package com.gec.mmall.service;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.User;

public interface IUserService {

	public ServerResponse<User> login(String username, String password);

	public ServerResponse<String> register(User user);

	ServerResponse<String> checkValid(String str,String type);

}
