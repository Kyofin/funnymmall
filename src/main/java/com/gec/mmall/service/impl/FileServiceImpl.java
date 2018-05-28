package com.gec.mmall.service.impl;

import com.gec.mmall.service.IFileService;
import com.gec.mmall.util.FtpUtil;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("iFileService")
public class FileServiceImpl implements IFileService {

	private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

	/**
	 * 将springMVC接收的文件上传到文件服务器
	 * @param multipartFile
	 * @param path
	 * @return
	 */
	@Override
	public String upload(MultipartFile multipartFile,String path){
		//检查用户是否已经添加上传文件
		if (multipartFile.getSize() == 0){
			return null;
		}
		//原始文件名
		String fileName = multipartFile.getOriginalFilename();
		//扩展名 abc.jpg
		String fileExtensionName = fileName.substring(fileName.lastIndexOf(".")+1);
		String uploadFileName = UUID.randomUUID().toString()+"."+fileExtensionName;
		logger.info("开始上传文件,上传的文件名{},上传的路径{},新文件名{}",fileName,path,uploadFileName );

		File fileDir = new File(path);
		if (!fileDir.exists()){
			//赋予写的权限
			fileDir.setWritable(true);
			//根据path创建（多级）目录
			fileDir.mkdirs();
		}
		//创建目标文件对象
		File targetFile = new File(path,uploadFileName);

		try {
			//将springMVC接收到的文件转换成目标文件,保存到项目发布的目录下的指定文件夹中
			multipartFile.transferTo(targetFile);//文件上传到tomcat已完成！

			//将targetFile上传到我们的FTP服务器上
			FtpUtil.uploadFile(Lists.newArrayList(targetFile));//已经上传到ftp服务器！

			//上传完成之后,删除tomcat的upload文件下面的文件
			targetFile.delete();
		} catch (IOException e) {
			logger.error("上传文件异常",e);
			return  null;
		}
		return  targetFile.getName();
	}
}
