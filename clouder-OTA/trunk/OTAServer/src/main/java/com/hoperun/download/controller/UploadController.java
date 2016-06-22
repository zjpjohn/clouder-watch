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
package com.hoperun.download.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.hoperun.download.util.SysProperties;
import com.hoperun.download.vo.UploadVO;

/**
 * ClassName: UploadController
 *
 * @description
 * @author xing_peng
 * @Date 2015-7-6
 * 
 */

@Controller
public class UploadController {

	private final Logger log = Logger.getLogger(UploadController.class);

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public String uploading() {
		return "upload";
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String upload(HttpServletRequest req, @Valid UploadVO uploadVO) {

		String type = uploadVO.getType().toString();

		SysProperties.writeSystemProperties(type + ".versionCode", uploadVO.getVersionCode());
		SysProperties.writeSystemProperties(type + ".versionName", uploadVO.getVersionName());

		InputStream ins = null;
		OutputStream os = null;

		log.info("upload file : " + SysProperties.getProperty(type + ".fileName"));

		try {
			ins = uploadVO.getUploadFile().getInputStream();

			String installFileName = SysProperties.getProperty(type + ".fileName");
			String dir = SysProperties.getProperty(type + ".downLoadPath");
			os = new FileOutputStream(dir + installFileName);

			int needSize = (int) uploadVO.getUploadFile().getSize();
			log.info("file size : " + needSize);
			
			byte[] b = new byte[1024];
			while (needSize > 0) {
				int len = ins.read(b);
				if (needSize < b.length) {
					os.write(b, 0, needSize);
				} else {
					os.write(b, 0, len);
					if (len < b.length) {
						break;
					}
				}
				needSize -= b.length;
			}


		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ins.close();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return "redirect:upload";
	}
}
