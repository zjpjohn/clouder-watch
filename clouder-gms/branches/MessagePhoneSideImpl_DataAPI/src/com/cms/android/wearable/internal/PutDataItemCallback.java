package com.cms.android.wearable.internal;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.FutureTask;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.cms.android.common.api.Status;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.api.impl.DataApiImpl.DataItemResultImpl;

public class PutDataItemCallback extends WearableCallback {

	private List<FutureTask<Boolean>> futureTaskList;

	private WearableResult<DataApi.DataItemResult> result;

	private List<String> list;

	PutDataItemCallback(Context context, WearableResult<DataApi.DataItemResult> result,
			List<FutureTask<Boolean>> futureTaskList, List<String> list) {
		super(context);
		this.result = result;
		this.futureTaskList = futureTaskList;
		this.list = list;
	}

	@Override
	public void setPutDataRsp(PutDataResponse response) throws RemoteException {
		Log.d("WearableAdapter", "receive put data response, status = " + response.status + ", dataItem = "
				+ response.dataItem);
		DataItemResultImpl result = new DataItemResultImpl(new Status(response.status), response.dataItem);
		this.result.setResult(result);
		if (response.status != 0) {
			// 若不为成功
			Iterator<FutureTask<Boolean>> iterator = this.futureTaskList.iterator();
			while (iterator.hasNext()) {
				FutureTask<Boolean> futureTask = (FutureTask<Boolean>) iterator.next();
				futureTask.cancel(true);
			}
		}
	}

	@Override
	public void setAssetRsp() throws RemoteException {
		Log.e("spencer", "setAssetRsp callback");
		if (list != null && !list.isEmpty()) {
			for (String filepath : list) {
				File file = new File(filepath);
				if (file.exists()) {
					boolean isDel = file.delete();
					Log.e("spencer", "删除文件 " + filepath + " isDel = " + isDel);
				}
			}
		}
	}
}
