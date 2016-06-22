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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hoperun.download.util.JsonHelper;
import com.hoperun.download.util.MD5;
import com.hoperun.download.util.SysProperties;
import com.hoperun.download.vo.RequestVO;
import com.hoperun.download.vo.VersionVO;

/**
 * ClassName: VersionController
 *
 * @description
 * @author xing_peng
 * @Date 2015-6-29
 * 
 */

@Controller
public class VersionController {

	private final Logger log = Logger.getLogger(VersionController.class);

	static {
		SysProperties.init();
	}

	@RequestMapping(value = "/versionDownLoad", method = RequestMethod.POST)
	public void downNewInstallPacket(HttpServletResponse response, HttpServletRequest req,
			@RequestBody RequestVO request) {

		RandomAccessFile raFile = null;
		OutputStream os = null;

		try {

			log.info("versionDownLoad");

			if (request.getType() != null) {

				String type = request.getType().toString();
				log.info("request type : " + type);
				String installFileName = SysProperties.getProperty(type + ".fileName");
				String dir = SysProperties.getProperty(type + ".downLoadPath");

				log.info("downLoad file : " + installFileName);
				raFile = new RandomAccessFile(dir + installFileName, "r");

				response.setContentLength((int) raFile.length());
				response.setCharacterEncoding("utf-8");
				response.setContentType("multipart/form-data");
				response.setHeader("Content-Disposition", "attachment;fileName=" + installFileName);

				String range = req.getHeader("RANGE");
				log.info("range: " + range);

				int start = 0, end = 0;
				if (null != range && range.startsWith("bytes=")) {
					String[] values = range.split("=")[1].split("-");
					start = Integer.parseInt(values[0]);
					if (values.length == 2) {
						end = Integer.parseInt(values[1]);
					} else {
						end = (int) raFile.length() - 1;
					}
				}
				int requestSize = 0;
				if (end != 0 && end > start) {
					requestSize = end - start + 1;
					response.setContentLength(requestSize);
				} else {
					requestSize = Integer.MAX_VALUE;
				}

				os = response.getOutputStream();
				byte[] b = new byte[1024];

				int needSize = requestSize;
				if (start <= end) {
					raFile.seek(start);
					while (needSize > 0) {
						int len = raFile.read(b);
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
				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				raFile.close();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@RequestMapping(value = "/queryVersionNo", method = RequestMethod.POST)
	@ResponseBody
	public String queryVersionNo(HttpServletRequest req, @RequestBody RequestVO request) {

		VersionVO vo = new VersionVO();
		log.info("queryVersionNo");

		if (request.getType() != null) {

			String type = request.getType().toString();
			log.info("request type : " + type);
			String installFileName = SysProperties.getProperty(type + ".fileName");
			String dir = SysProperties.getProperty(type + ".downLoadPath");

			vo.setChecksum(MD5.md5sum(dir + installFileName));
			vo.setPackageName(SysProperties.getProperty(type + ".packageName"));
			vo.setFileName(SysProperties.getProperty(type + ".fileName"));
			vo.setVersionName(SysProperties.getProperty(type + ".versionName"));
			vo.setVersionCode(Integer.valueOf(SysProperties.getProperty(type + ".versionCode")));

			File file = new File(dir + installFileName);
			vo.setFileLength(file.length());

			log.info("Version : " + JsonHelper.toJson(vo));
		}

		return JsonHelper.toJson(vo);
	}

}
