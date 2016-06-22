/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (c) 2014 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.hoperun.download.vo;

import org.springframework.web.multipart.MultipartFile;

 /**
 * ClassName: UploadVO
 *
 * @description
 * @author xing_peng
 * @Date 2015-7-6
 * 
 */
public class UploadVO {

	private DownloadType type;
	
	private MultipartFile uploadFile;
	
	private String versionName;
	
	private String versionCode;

	public DownloadType getType() {
		return type;
	}

	public void setType(DownloadType type) {
		this.type = type;
	}

	public MultipartFile getUploadFile() {
		return uploadFile;
	}

	public void setUploadFile(MultipartFile uploadFile) {
		this.uploadFile = uploadFile;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public String getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(String versionCode) {
		this.versionCode = versionCode;
	}
	
}
