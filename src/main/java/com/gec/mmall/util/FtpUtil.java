package com.gec.mmall.util;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class FtpUtil {

	private static final Logger logger = LoggerFactory.getLogger(FtpUtil.class);

	private static final String ftpIp = PropertiesUtil.getProperty("ftp.server.ip");
	private static final String ftpUser = PropertiesUtil.getProperty("ftp.user");
	private static final String ftpPass = PropertiesUtil.getProperty("ftp.pass");
	private static final String imgRemotePath = "/mmall/img";

	private String ip;
	private String user;
	private String pwd;
	private int port;
	private FTPClient ftpClient;


	public FtpUtil(String ip, String user, String pwd, int port) {
		this.ip = ip;
		this.user = user;
		this.pwd = pwd;
		this.port = port;
	}

	/**
	 * 上传文件到ftp服务器的对外调用方法
	 * @param fileList
	 * @return
	 * @throws IOException
	 */
	public static boolean uploadFile(List<File> fileList) throws IOException {
		FtpUtil ftpUtil = new FtpUtil(ftpIp,ftpUser,ftpPass,21);
		logger.info("开始连接ftp服务器");
		//保存到img文件夹中
		boolean result = ftpUtil.uploadFile(imgRemotePath,fileList);
		logger.info("结束上传,上传结果:{}",result);
		return  result;
	}

	/**
	 * 真正执行上传ftp服务器的逻辑
	 * @param remotePath
	 * @param fileList
	 * @return
	 * @throws IOException
	 */
	private boolean uploadFile(String remotePath,List<File> fileList) throws IOException {
		boolean uploaded = true;
		FileInputStream fis = null;
		//连接FTP服务器(创建ftpClient,并连接服务器)
		if (connectServer(this.ip,this.port,this.user,this.pwd)){
			try {
				//设置ftpClient参数
				//ftpClient.changeWorkingDirectory(remotePath);//切换到该目录
				createMultiDir(remotePath);//创建多级目录并进入该目录
				ftpClient.setBufferSize(1024);
				ftpClient.setControlEncoding("UTF-8");
				ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
				ftpClient.enterLocalPassiveMode();
				//遍历存储文件
				for (File fileItem : fileList) {
					fis = new FileInputStream(fileItem);
					ftpClient.storeFile(fileItem.getName(),fis);
				}
			} catch (IOException e) {
				logger.error("ftpClient上传文件异常",e);
				uploaded = false;
			}finally {
				fis.close();
				ftpClient.disconnect();
			}
		}
		return uploaded;
	}


	/**
	 * 创建多级目录
	 * @param multiDir 多级文件路径 格式"/a/b/c/"
	 * @return
	 * @throws IOException
	 */
	public boolean createMultiDir(String multiDir) throws IOException {
		boolean isSuccess = false;
		String[] dirs = multiDir.split("/");
		ftpClient.changeWorkingDirectory("/");
		for (int i = 1; dirs != null && i < dirs.length; i++) {
			logger.info("准备进入目录：{}",dirs[i]);
			if (!ftpClient.changeWorkingDirectory(dirs[i])) {
				if (ftpClient.makeDirectory(dirs[i])) {
					if (!ftpClient.changeWorkingDirectory(dirs[i])) {
						return false;
					}
				}
			}
		}
		isSuccess = true;
		return isSuccess;
	}




	/**
	 * 连接ftp服务器
	 * @param ip
	 * @param port
	 * @param user
	 * @param pwd
	 * @return
	 */
	private boolean connectServer(String ip,int port,String user,String pwd){
		boolean isSuccess = false;
		ftpClient = new FTPClient();
		try {
			ftpClient.connect(ip);
			isSuccess = ftpClient.login(user, pwd);
		} catch (IOException e) {
			logger.error("连接ftp服务器异常",e);
		}
		return isSuccess;
	}


	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public FTPClient getFtpClient() {
		return ftpClient;
	}

	public void setFtpClient(FTPClient ftpClient) {
		this.ftpClient = ftpClient;
	}
}
