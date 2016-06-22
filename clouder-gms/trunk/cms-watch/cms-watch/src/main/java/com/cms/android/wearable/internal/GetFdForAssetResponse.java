package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;

public class GetFdForAssetResponse implements SafeParcelable {
	public static final Creator<GetFdForAssetResponse> CREATOR = new GetFdForAssetResponseCreator();
	public final ParcelFileDescriptor fd;
	public final int status;
	public final int version;

	public GetFdForAssetResponse(int version, int status, ParcelFileDescriptor fd) {
		this.version = version;
		this.status = status;
		this.fd = fd;
	}

	private GetFdForAssetResponse(Parcel in) {
		this.fd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
		this.status = in.readInt();
		this.version = in.readInt();
	}

	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeParcelable(this.fd, flag);
		dest.writeInt(status);
		dest.writeInt(version);
	}

	public static class GetFdForAssetResponseCreator implements Creator<GetFdForAssetResponse> {

		public GetFdForAssetResponse createFromParcel(Parcel in) {
			return new GetFdForAssetResponse(in);
		}

		public GetFdForAssetResponse[] newArray(int size) {
			return new GetFdForAssetResponse[size];
		}
	}
}