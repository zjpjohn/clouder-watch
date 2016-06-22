package com.hoperun.watch.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.hoperun.watch.download.IDownloadEventCallback;
import com.hoperun.watch.vo.AppInfo;
import com.hoperun.watch.vo.DownInfo;
import com.hoperun.watch.vo.enums.EDownloadStatus;
import com.hoperun.watch.vo.enums.EServiceType;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class HttpUtils {

	private static final String TAG = "";

	private static final int TIMEOUT = 30 * 1000;

	// private String BASE_URL =
	// "http://10.20.71.47:8080/telem-fm-data-webservice/dataservice/api";

	private String BASE_URL = "http://10.20.71.53:8080/download";

	private static String HTTP_HEAD_CONTENT_TYPE = "CONTENT-TYPE";

	private static String HTTP_HEAD_CONTENT_TYPE_VALUE = "application/json; charset=utf-8";

	private volatile boolean canceled = false;

	/**
	 * 当前下载信息
	 */
	private DownInfo downInfo;

	/**
	 * 当前下载回调
	 */
	private IDownloadEventCallback callback;

	public String send(EServiceType serviceType, String request) throws Exception {
		String url = BASE_URL + serviceType.getPath();
		String response = null;
		try {
			HttpResponse res = getHttpClient().execute(getHttpPost(url, request));
			int statusCode = res.getStatusLine().getStatusCode();
			Log.d(TAG, String.format("Http statusCode = %s", statusCode));
			if (statusCode == 200) {
				response = EntityUtils.toString(res.getEntity(), "UTF-8");
			} else {
				throw new Exception("NetworkException");
			}
		} catch (Exception e) {
			Log.d(TAG, "Http send exception.", e);
			throw new Exception(e);
		}
		return response;
	}
	
	public AppInfo queryApkVersionNo() throws Exception {
		String response = send(EServiceType.QUERY_VERSION_NO,"{\"type\":\"apk\"}");
//		String response = send(EServiceType.QUERY_VERSION_NO,"");
		return new Gson().fromJson(response, AppInfo.class);
	}
	
	public AppInfo queryFirmwareVersionNo() throws Exception {
		String response = send(EServiceType.QUERY_VERSION_NO,"{\"type\":\"firmware\"}");
		Log.d(TAG, String.format("response is %s.", response));
		return new Gson().fromJson(response, AppInfo.class);
	}

	public void download(EServiceType serviceType, String request, long position,long totalSize, String filePath,
			IDownloadEventCallback callback) throws Exception {
		if (callback == null) {
			Log.w(TAG, "[download] IDownloadEventCallback can't be null.");
			return;
		}

		canceled = false;

		// 下载信息对象
		this.downInfo = new DownInfo();
		this.callback = callback;		

		String url = BASE_URL + serviceType.getPath();
		HttpClient client = getHttpClient();
		HttpPost post = getHttpPost(url, request);
		post.addHeader("RANGE", "bytes=" + position + "-");

		InputStream is = null;
		FileOutputStream fos = null;
		try {
			HttpResponse res = client.execute(post);
			int statusCode = res.getStatusLine().getStatusCode();
			Log.d(TAG, String.format("Http statusCode = %s", statusCode));
			if (statusCode == 200) {
				is = res.getEntity().getContent();
				Header[] headers = res.getAllHeaders();

				for (int i = 0; i < headers.length; i++) {
					Log.d(TAG, "name:" + headers[i].getName() + " value:" + headers[i].getValue());
				}
				
				Log.d(TAG, "totalSize is " + totalSize);

				Header dispoHeader = res.getFirstHeader("Content-Disposition");
				Log.d(TAG, "dispoHeader is " + dispoHeader);

				fos = new FileOutputStream(new File(filePath),true);
				byte[] buffer = new byte[1024];
				int ch = 0;
				long count = position;
				int progress = 0;

				while (!canceled && (ch = is.read(buffer)) != -1) {
					// 将内缓存中的内容写入文件中
					fos.write(buffer, 0, ch);
					fos.flush();

					count += ch;
					progress = (int) (((float) count / totalSize) * 100);
					// 修改下载信息
					downInfo.setTotalSize(totalSize);
					downInfo.setDownloadStatus(EDownloadStatus.DOWNLOADING);
					downInfo.setDownloadCount(count);
					downInfo.setDownloadProgress(progress);
					callback.onDownloading(downInfo);
				}
				// 1.完全下载完成，如此则下载长度==获取的长度值
				// 2.中途取消,则下载长度<获取的长度值
				Log.e(TAG, "write length is " + count + " position:" + position + " totalSize:" + totalSize
						+ " file length:" + new File(filePath).length());
				if (count == totalSize) {
					downInfo.setDownloadStatus(EDownloadStatus.COMPLETED);
					downInfo.setSaveFilePath(filePath);
					callback.onDownloadCompeleted(downInfo);
				}else{
					Log.d(TAG, "count != totalSize");
				}
			} else {
				throw new Exception(String.format("NetworkException status code is %s.", statusCode));
			}
		} catch (Exception e) {
			downInfo.setDownloadStatus(EDownloadStatus.ERROR);
			callback.onDownloadError(downInfo);
			Log.e(TAG, "Http send exception.", e);
			throw new Exception(e);
		} finally {
			Log.d(TAG, "finally close is and fos.");
			try {
				if (is != null) {
					is.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Exception ex) {
				Log.e(TAG, "error in close stream", ex);
			}
		}
	}

	/**
	 * 取消下载
	 */
	public void cancelDownload() {
		if (!canceled) {
			this.canceled = true;
			if (this.downInfo != null && this.callback != null) {
				this.downInfo.setDownloadStatus(EDownloadStatus.CANCELED);
				this.callback.onDownloadCanceled(this.downInfo);
			}
		}
	}

	protected HttpClient getHttpClient() {
		HttpClient client = new DefaultHttpClient();
		HttpParams httpParams = client.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT);
		return client;
	}

	protected HttpPost getHttpPost(String url, String contextString) {
		HttpPost post = new HttpPost(url);
		post.setHeader(HTTP_HEAD_CONTENT_TYPE, HTTP_HEAD_CONTENT_TYPE_VALUE);

		try {
			HttpEntity entity = new ByteArrayEntity(contextString.getBytes("UTF-8"));
			post.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
			Log.d(TAG, "Http getHttpPost exception.", e);
		}
		return post;
	}

}
