package com.cms.android.common.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.cms.android.common.api.Api;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Releasable;
import com.cms.android.common.api.Result;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;

public class MobvoiApi {

	private static final String TAG = "MobvoiApi";

	public static void release(Result result) {

		if (result instanceof Releasable) {
			try {
				((Releasable) result).release();
				return;
			} catch (RuntimeException exception) {
				Log.e("MobvoiApi", "release " + result + " failed.", exception);
			}
		}
	}

	public static abstract class AbstractPendingResult<R extends Result> implements PendingResult<R> {

		private ResultCallback<R> mCallback;

		private boolean mConsumed = false;

		private MobvoiApi.ResultHandler<R> mHandler;

		private boolean mIsCanceled;

		private boolean mIsDone;

		private CountDownLatch mLatch = new CountDownLatch(1);

		private final Object mLock = new Object();

		private R mResult;

		private R consume() {
			Log.i(TAG, "consume...");
			synchronized (this.mLock) {
				Assert.isTrue(!this.mConsumed, "Result has already been consumed.");
				Assert.isTrue(isReady(), "Result is not ready.");
				R result = this.mResult;
				clear();
				return result;
			}
		}

		private void create(R result) {
			Log.i(TAG, "create result");
			this.mResult = result;
			this.mLatch.countDown();
			if (this.mCallback != null) {
				Log.i(TAG, "create this.mCallback != null");
				this.mHandler.release();
				if (!this.mIsCanceled)
					this.mHandler.call(this.mCallback, consume());
			} else {
				Log.i(TAG, "create this.mCallback == null");
			}
		}

		static <R extends Result> void execute(AbstractPendingResult<R> abstractPendingResult) {
			abstractPendingResult.timeout();
		}

		private void interrupt() {
			synchronized (this.mLock) {
				if (!isReady()) {
					// 12为中断状态位
					setResult(create(new Status(12)));
					this.mIsDone = true;
				}
				return;
			}
		}

		private void timeout() {
			Log.i(TAG, "handle timeout");
			synchronized (this.mLock) {
				if (!isReady()) {
					Log.i(TAG, "set result");
					// 13为超时标志位
					setResult(create(new Status(13)));
					this.mIsDone = true;
				}
				return;
			}
		}

		@Override
		public final R await() {
			return await(5L, TimeUnit.MINUTES);
		}

		@Override
		public final R await(long timeout, TimeUnit timeUnit) {
//			Assert.isTrue(Looper.myLooper() != Looper.getMainLooper(), "await must not be on the UI thread");
//			Assert.isTrue(!this.mConsumed, "Result has already been consumed");
			Log.i(TAG, "await timeout = " + timeout);
			try {
				if (!this.mLatch.await(timeout, timeUnit)) {
					Log.i(TAG, "timeout");
					timeout();
				}
				Log.i(TAG, "await after timeout = " + timeout);
			} catch (InterruptedException exception) {
				interrupt();
				Log.e(TAG, "interrupt");
			}
			Log.i(TAG, "await consume");
			Assert.isTrue(isReady(), "Result is not ready.");
			return consume();
		}

		protected void clear() {
			this.mConsumed = true;
			this.mResult = null;
			this.mCallback = null;
		}

		protected abstract R create(Status status);

		public boolean isCanceled() {
			synchronized (this.mLock) {
				return this.mIsCanceled;
			}
		}

		public final boolean isReady() {
			return this.mLatch.getCount() == 0;
		}

		protected void setHandler(MobvoiApi.ResultHandler<R> resultHandler) {
			this.mHandler = resultHandler;
		}

		public final void setResult(R r) {
			synchronized (this.mLock) {
				if ((!this.mIsDone) && (!this.mIsCanceled)) {
					Assert.isTrue(!isReady(), "Results have already been set");
					Assert.isTrue(!this.mConsumed, "Result has already been consumed");
					create(r);
				} else {
					MobvoiApi.release(r);
				}
			}
		}

		public final void setResultCallback(ResultCallback<R> resultCallback) {
			Assert.isTrue(!this.mConsumed, "Result has already been consumed.");
			synchronized (this.mLock) {
				if ((!isCanceled()) && (isReady()))
					this.mHandler.call(resultCallback, consume());
				this.mCallback = resultCallback;
			}
		}
	}

	public static abstract class ApiResult<R extends Result, A extends Api.Connection> extends
			MobvoiApi.AbstractPendingResult<R> implements MobvoiApiClientImpl.Connection<A> {

		private final Api.Key<A> mKey;

		private MobvoiApiClientImpl.OnClearListener mListener;

		protected ApiResult(Api.Key<A> key) {
			this.mKey = key;
		}

		private void dump(RemoteException exception) {
			setStatus(new Status(8, exception.getLocalizedMessage(), null));
		}

		protected void clear() {
			super.clear();
			if (this.mListener != null) {
				this.mListener.onClear(this);
				this.mListener = null;
			}
		}

		protected abstract void connect(A a) throws RemoteException;

		public Api.Key<A> getKey() {
			return this.mKey;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void setConnection(A a) throws DeadObjectException {
			Looper looper = a.getLooper();
			Log.d(TAG, "Looper[setConnection] hashCode = " + looper.hashCode() + " thread id = "
					+ looper.getThread().getId() + " name = " + looper.getThread().getName());
			Looper myLooper = Looper.myLooper();
			Log.d(TAG, myLooper == null ? "当前线程Looper为空" : (myLooper.getThread().getId() + " " + myLooper.getThread()
					.getName()));
			if (myLooper == null) {
				Looper.prepare();
			}
			setHandler(new MobvoiApi.ResultHandler(a.getLooper()));
			try {
				Log.i(TAG, "setConnection中调用connect方法");
				connect(a);
				return;
			} catch (DeadObjectException deadObjectException) {
				Log.e(TAG, "dump deadObjectException");
				dump(deadObjectException);
			} catch (RemoteException remoteException) {
				Log.e(TAG, "dump remoteException");
				dump(remoteException);
			}
		}

		public final void setStatus(Status status) {
			Log.e(TAG, "setStatus >> " + status);
			setResult(create(status));
			// TODO 这边没明白什么意思
			// if (!status.isSuccess())
			// for (boolean bool = true;; bool = false) {
			// Assert.notEmpty(!status.isSuccess(),
			// "Failed result must not be success");
			//
			// return;
			// }
		}
	}

	public static class ResultHandler<R extends Result> extends Handler {

		public ResultHandler() {
			this(Looper.getMainLooper());
		}

		public ResultHandler(Looper looper) {
			super();
		}

		public boolean call(ResultCallback<R> resultCallback, R r) {
			Log.i(TAG, "ResultHandler call");
			return sendMessage(obtainMessage(1, new Pair<ResultCallback<R>, R>(resultCallback, r)));
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void handleMessage(Message message) {
			switch (message.what) {
			case 1:
				// 将Result正常返回
				Log.i(TAG, "handleMessage 1");
				Pair pair = (Pair) message.obj;
				onResult((ResultCallback<R>) pair.first, (R) pair.second);
				break;
			case 2:
				Log.i(TAG, "handleMessage 2 release");
				MobvoiApi.release((Result) message.obj);
				break;
			case 3:
			case 4:
				Log.i(TAG, "handleMessage 3 4");
				MobvoiApi.AbstractPendingResult.execute((MobvoiApi.AbstractPendingResult) message.obj);
			default:
				Log.i("MobvoiApi", "discard a message, message = " + message);
				break;
			}
		}

		protected void onResult(ResultCallback<R> resultCallback, R r) {
			try {
				resultCallback.onResult(r);
				return;
			} catch (RuntimeException runtimeException) {
				MobvoiApi.release(r);
			}		}

		public void release() {
			removeMessages(2);
		}
	}
}