package com.hoperun.watch.download;

public interface IDownloadEventCallback {

	void onDownloading(IDownload download);

	void onDownloadError(IDownload download);

	void onDownloadCompeleted(IDownload download);

	void onDownloadCanceled(IDownload download);
}
