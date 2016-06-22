package com.hoperun.watch.download;

import com.hoperun.watch.vo.enums.EDownloadStatus;

public interface IDownload {
	
	long getDownloadCount();

	int getDownloadProgress();

	long getTotalSize();

	EDownloadStatus getDownloadStatus();
	
	String getSaveFilePath();

}
