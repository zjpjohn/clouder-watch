package com.cms.android.wearable;

import java.util.Arrays;

import android.net.Uri;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;

public class Asset implements SafeParcelable {
	public static final Creator<Asset> CREATOR = new AssetCreator();
	protected byte[] data;
	protected String digest;
	protected ParcelFileDescriptor fd;
	protected Uri uri;
	final int versionCode;

	protected Asset(int versionCode, byte[] data, String digest, ParcelFileDescriptor fd, Uri uri) {
		this.versionCode = versionCode;
		this.data = data;
		this.fd = fd;
		this.digest = digest;
		this.uri = uri;
	}

	private Asset(Parcel in) {
		int size = in.readInt();
		if (size > 0) {
			this.data = new byte[size];
			in.readByteArray(this.data);
		} else {
			this.data = new byte[0];
		}
		this.digest = in.readString();
		this.fd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
		this.uri = in.readParcelable(Uri.class.getClassLoader());
		this.versionCode = in.readInt();
	}

	protected static Asset createFromRef(String digest) {
		if (digest == null)
			throw new IllegalArgumentException("Asset digest is null!");
		return new Asset(1, null, digest, null, null);
	}

	public static Asset createFromBytes(byte[] data) {
		if (data == null)
			throw new IllegalArgumentException("Asset data is null!");
		return new Asset(1, data, null, null, null);
	}

	protected static Asset createFromFd(ParcelFileDescriptor fd) {
		if (fd == null)
			throw new IllegalArgumentException("Asset fd is null!");
		return new Asset(1, null, null, fd, null);
	}

	public int describeContents() {
		return 0;
	}

	public boolean equals(Object obj) {
		boolean bool = this == obj;
		if (!bool) {
			bool = obj instanceof Asset;
			if (bool) {
				Asset asset = (Asset) obj;
				bool = (equals(this.versionCode, asset.versionCode))
						&& (equals(this.digest, asset.digest))
						&& (equals(this.uri, asset.uri) && (equals(this.fd, asset.fd)) && (equals(this.data, asset.data)));
			}
		}
		return bool;
	}

	private boolean equals(Object obj1, Object obj2) {
		return obj1 == obj2 || obj1.equals(obj2);
	}

	public byte[] getData() {
		return this.data;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public String getDigest() {
		return this.digest;
	}

	public void setFd(ParcelFileDescriptor pfd) {
		this.fd = pfd;
	}

	public ParcelFileDescriptor getFd() {
		return this.fd;
	}

	public Uri getUri() {
		return this.uri;
	}

	public int getVersionCode() {
		return this.versionCode;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public int hashCode() {
		Object[] array = new Object[4];
		array[0] = this.data;
		// array[1] = this.fd;
		array[2] = this.digest;
		array[3] = this.uri;
		return Arrays.hashCode(array);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Asset[@");
		sb.append(Integer.toHexString(hashCode()));
		if (this.data != null) {
			sb.append(", size=");
			sb.append(this.data.length);
		}
		if (this.fd != null) {
			sb.append(", fd=");
			sb.append(this.fd);
		}
		if (this.digest != null) {
			sb.append(", digest=");
			sb.append(this.digest);
		}
		if (this.uri != null) {
			sb.append(", uri=");
			sb.append(this.uri);
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeInt(this.data == null ? 0 : this.data.length);
		if (this.data != null && this.data.length != 0) {
			dest.writeByteArray(this.data);
		}
		dest.writeString(this.digest);

		dest.writeParcelable(this.fd, flag);
		dest.writeParcelable(this.uri, flag);
		dest.writeInt(this.versionCode);
	}

	public static class AssetCreator implements Creator<Asset> {

		public Asset createFromParcel(Parcel in) {
			return new Asset(in);
		}

		public Asset[] newArray(int size) {
			return new Asset[size];
		}
	}
}