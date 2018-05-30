package com.gec.mmall.service.impl;

import com.gec.mmall.common.Const;
import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.common.TokenCache;
import com.gec.mmall.dao.UserMapper;
import com.gec.mmall.pojo.User;
import com.gec.mmall.service.IUserService;
import com.gec.mmall.util.MD5Util;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService {

	@Autowired
	private UserMapper userMapper;

	/**
	 * 登录逻辑
	 * @param username
	 * @param password
	 * @return
	 */
	@Override
	public ServerResponse<User> login(String username, String password) {
		int resultCount = userMapper.checkUserName(username);
		if (resultCount == 0) {
			return ServerResponse.createByErrorMessage("用户名不存在");
		}

		password = MD5Util.MD5EncodeUtf8(password);

		User user = userMapper.selectLogin(username,password);
		if (user == null){
			return ServerResponse.createByErrorMessage("密码错误");
		}
		//将密码置空返回前端
		user.setPassword(StringUtils.EMPTY);
		return ServerResponse.createBySuccess("登录成功",user);
	}

	/**
	 * 注册逻辑
	 * @param user
	 * @return
	 */
	@Override
	public ServerResponse<String> register(User user) {
		//校验用户名
		ServerResponse<String> validResponse = checkValid(user.getUsername(), Const.USERNAME);
		if (!validResponse.isSuccess()) {
			return validResponse;
		}
		//校验email
		validResponse = checkValid(user.getEmail(),Const.EMAIL);
		if (!validResponse.isSuccess()) {
			return validResponse;
		}
		//校验密码
		if(user.getPassword()==null){
			return ServerResponse.createByErrorMessage("密码不能为空");
		}
		//配置用户权限
		user.setRole(Const.Role.ROLE_CUSTMOER);
		//MD5加密密码
		user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
		//持久化数据
		int resultCount = userMapper.insert(user);
		if (resultCount == 0){
			return ServerResponse.createByErrorMessage("注册失败");
		}
		return ServerResponse.createBySuccessMessage("注册成功");

	}


	/**
	 * 校验提交参数
	 * @param str
	 * @param type
	 * @return
	 */
	@Override
	public ServerResponse<String> checkValid(String str, String type) {
		if (StringUtils.isNotBlank(type)){
			//开始校验
			if (type.equals(Const.USERNAME)){
				int resultCount = userMapper.checkUserName(str);
				if (resultCount > 0){
					return ServerResponse.createByErrorMessage("用户名已存在");
				}
			}
			if (type.equals(Const.EMAIL)){
				int resultCount = userMapper.checkEmail(str);
				if (resultCount > 0){
					return ServerResponse.createByErrorMessage("email已存在");
				}
			}

		}else {
			return ServerResponse.createByErrorMessage("参数错误");
		}
		return ServerResponse.createBySuccessMessage("校验成功");
	}

	/**
	 * 查找用户的找回密码问题
	 * @param username
	 * @return
	 */
	@Override
	public ServerResponse selectQuestion(String username){
		ServerResponse validResponse = checkValid(username,Const.USERNAME);
		if (validResponse.isSuccess()){
			//用户名不存在
			return ServerResponse.createByErrorMessage("用户名不存在");
		}
		String question = userMapper.selectQuestionByUsername(username);
		if (StringUtils.isNotBlank(question)){
			return ServerResponse.createBySuccess(question);
		}
		return ServerResponse.createByErrorMessage("找回密码的问题是空的");
	}

	/**
	 * 校验找回密码的问题和答案
	 * @param username
	 * @param question
	 * @param answer
	 * @return
	 */
	@Override
	public ServerResponse<String> checkAnswer(String username, String question, String answer){
		int resultCount = userMapper.checkAnswer( username, question, answer);
		if (resultCount > 0){
			//说明问题及答案是这个用户的，并且答案正确
			String forgetToken = UUID.randomUUID().toString();
			//把问题答案的token保存到缓存
			TokenCache.setKey(TokenCache.TOKEN_PREFIX+username,forgetToken);
			return ServerResponse.createBySuccess(forgetToken);
		}
		//答案和问题不匹配
		return ServerResponse.createByErrorMessage("问题的答案错误");
	}

	/**
	 * 找回密码（通过权限token重置用户密码）
	 * @param username
	 * @param passwordNew
	 * @param forgetToken
	 * @return
	 */
	@Override
	public ServerResponse<String> forgetResetPassword(String username,String passwordNew,String forgetToken){
		//检查传递参数有没有token
		if(org.apache.commons.lang3.StringUtils.isBlank(forgetToken)){
			return ServerResponse.createByErrorMessage("参数错误，参数token需要传递");
		}
		//检查用户名
		ServerResponse validResponse = this.checkValid(username,Const.USERNAME);
		if (validResponse.isSuccess()){
			return ServerResponse.createByErrorMessage("用户名不存在");
		}

		//检查缓存中该用户的token是否有效
		String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
		if (org.apache.commons.lang3.StringUtils.isBlank(token)){
			return ServerResponse.createByErrorMessage("token无效或者过期");
		}

		//检查缓存中token和传递的参数forgetToken是否一致
		if (org.apache.commons.lang3.StringUtils.equals(token,forgetToken)){
			//StringUtils.equals其中一个为null是不会报空指针，会返回false
			String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
			int resultCount = userMapper.updatePasswordByUsername(username,md5Password);
			if (resultCount > 0){
				return ServerResponse.createBySuccessMessage("修改密码成功");
			}
		}else {
			return ServerResponse.createByErrorMessage("token错误，请重新获取重置密码的token");
		}
		return ServerResponse.createByErrorMessage("修改密码错误");
	}

	/**
	 * 登录状态下重置密码
	 * @param passwordOld
	 * @param passwordNew
	 * @param user
	 * @return
	 */
	@Override
	public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user) {
		//防止横向越权，要检验一下用户的旧密码，一定要指定是这个用户，因为我们会count(1)，如果不指定id，那么count(1)结果很大概率就是true
		int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
		if(resultCount == 0){
			return ServerResponse.createByErrorMessage("旧密码错误");
		}
		user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
		int updateCount = userMapper.updateByPrimaryKeySelective(user);
		if (updateCount > 0){
			return ServerResponse.createBySuccessMessage("密码更新成功");
		}
		return ServerResponse.createByErrorMessage("密码更新失败");
	}

	/**
	 * 登录状态下更新用户信息
	 * @param user
	 * @return
	 */
	@Override
	public ServerResponse<User> updateInformation(User user){
		//username不能被更新
		//email要进行校验。校验新的email是不是已经被其他用户使用了
		int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
		if (resultCount > 0){
			return ServerResponse.createByErrorMessage("email已经存在，请更新email再尝试更新");
		}

		//将用户提交的信息填充到updateUser,并且只更新updateUser有的属性到数据库
		User updateUser = new User();
		updateUser.setId(user.getId());
		updateUser.setEmail(user.getEmail());
		updateUser.setPhone(user.getPhone());
		updateUser.setQuestion(user.getQuestion());
		updateUser.setAnswer(user.getAnswer());
		int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
		if (updateCount > 0){
			return ServerResponse.createBySuccess("更新个人信息成功",updateUser);
		}

		return  ServerResponse.createByErrorMessage("更新个人信息失败");
	}

	/**
	 * 查数据库中用户信息
	 * @param userId
	 * @return
	 */
	@Override
	public ServerResponse<User> getInformation(Integer userId) {
		User user = userMapper.selectByPrimaryKey(userId);
		if (user == null){
			return ServerResponse.createByErrorMessage("找不到当前用户");
		}
		//将密码置空返回前端
		user.setPassword(StringUtils.EMPTY);
		return ServerResponse.createBySuccess(user);
	}


	/**
	 * 校验用户是否管理员
	 * @param user
	 * @return
	 */
	@Override
	public ServerResponse checkAdminRole(User user){
		if (user.getRole() == Const.Role.ROLE_ADMIN){
			return ServerResponse.createBySuccess();
		}
		return ServerResponse.createByError();
	}


}
