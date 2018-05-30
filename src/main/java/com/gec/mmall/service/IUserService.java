package com.gec.mmall.service;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.User;

public interface IUserService {

	public ServerResponse<User> login(String username, String password);

	public ServerResponse<String> register(User user);

	ServerResponse<String> checkValid(String str,String type);

	ServerResponse selectQuestion(String username);

	ServerResponse<String> checkAnswer(String username, String question, String answer);

	ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken);

	ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user);

	ServerResponse<User> updateInformation(User user);

	ServerResponse<User> getInformation(Integer id);

	ServerResponse checkAdminRole(User user);
}
