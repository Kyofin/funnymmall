package com.gec.mmall.common;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
//保证序列化json的时候，如果值是NULL的key忽略
public class ServerResponse<T> implements Serializable{
	private int code;
	private String msg;
	private T data;

	private ServerResponse(int code)
	{
		this.code = code;
	}

	private ServerResponse(int code,String msg)
	{
		this.code = code;
		this.msg = msg;
	}

	public ServerResponse(int code, T data) {
		this.code = code;
		this.data = data;
	}

	private ServerResponse(int code,String msg,T data)
	{
		this.code = code;
		this.msg = msg;
		this.data = data;
	}


	@JsonIgnore
	//使之不在json序列化中
	public boolean isSuccess()
	{
		return code == ResponseCode.SUCCESS.getCode();
	}

	public static <T> ServerResponse<T> createBySuccess(){
		return new ServerResponse<T>(ResponseCode.SUCCESS.getCode());
	}

	public static <T> ServerResponse<T> createBySuccessMessage(String msg){
		return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg);
	}

	public static <T> ServerResponse<T> createBySuccess(T data){
		return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),data);
	}

	public static <T> ServerResponse<T> createBySuccess(String msg,T data){
		return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg,data);
	}


	public static <T> ServerResponse<T> createByError(){
		return new ServerResponse<T>(ResponseCode.ERROR.getCode(),ResponseCode.ERROR.getDesc());
	}


	public static <T> ServerResponse<T> createByErrorMessage(String errorMessage){
		return new ServerResponse<T>(ResponseCode.ERROR.getCode(),errorMessage);
	}

	public static <T> ServerResponse<T> createByErrorCodeMessage(int errorCode,String errorMessage){
		return new ServerResponse<T>(errorCode,errorMessage);
	}

	public int getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	public T getData() {
		return data;
	}
}
