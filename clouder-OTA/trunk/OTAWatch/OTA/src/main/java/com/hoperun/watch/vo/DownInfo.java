package com.hoperun.watch.vo;

import java.io.Serializable;

import com.hoperun.watch.download.IDownload;
import com.hoperun.watch.vo.enums.EDownloadStatus;

public class DownInfo implements IDownload,Serializable {

	private static final long serialVersionUID = 1L;

	private EDownloadStatus status;

	private long totalSize;

	private long downloadCount;

	private int progress;

	private String saveFilePath;

	@Override
	public EDownloadStatus getDownloadStatus() {
		return status;
	}

	public void setDownloadStatus(EDownloadStatus status) {
		this.status = status;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public long getDownloadCount() {
		return downloadCount;
	}

	public void setDownloadCount(long downloadCount) {
		this.downloadCount = downloadCount;
	}

	@Override
	public int getDownloadProgress() {
		return progress;
	}

	public void setDownloadProgress(int progress) {
		this.progress = progress;
	}

	public void setSaveFilePath(String saveFilePath) {
		this.saveFilePath = saveFilePath;
	}

	@Override
	public String getSaveFilePath() {
		return this.saveFilePath;
	}

	@Override
	public String toString() {
		return "DownInfo [status=" + status + ", totalSize=" + totalSize + ", downloadCount=" + downloadCount
				+ ", progress=" + progress + "]";
	}

}
