package com.example.testasset;

import java.util.Arrays;

import android.net.Uri;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

public class Asset implements SafeParcelable {
	public static final Parcelable.Creator<Asset> CREATOR = new AssetCreator();
	protected byte[] data;
	protected String digest;
	protected ParcelFileDescriptor fd;
	protected Uri uri;
	final int versionCode;

	public Asset(int versionCode, byte[] data, String digest, ParcelFileDescriptor fd, Uri uri) {
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

	public static Asset createFromRef(String digest) {
		if (digest == null)
			throw new IllegalArgumentException("Asset digest is null!");
		return new Asset(1, null, digest, null, null);
	}

	public static Asset createFromBytes(byte[] data) {
		if (data == null)
			throw new IllegalArgumentException("Asset data is null!");
		return new Asset(1, data, null, null, null);
	}

	public static Asset createFromFd(ParcelFileDescriptor fd) {
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
				bool = (equals(this.versionCode, asset.versionCode)) && (equals(this.digest, asset.digest))
						&& (equals(this.uri, asset.uri));
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

	public int hashCode() {
		Object[] array = new Object[4];
		array[0] = this.data;
		// array[1] = this.fd;
		array[2] = this.digest;
		array[3] = this.uri;
		return Arrays.hashCode(array);
	}

	public String toString() {
		StringBuilder localStringBuilder = new StringBuilder();
		localStringBuilder.append("Asset[@");
		localStringBuilder.append(Integer.toHexString(hashCode()));
		if (this.data != null) {
			localStringBuilder.append(", size=");
			localStringBuilder.append(this.data.length);
		}
		if (this.fd != null) {
			localStringBuilder.append(", fd=");
			localStringBuilder.append(this.fd);
		}
		if (this.digest != null) {
			localStringBuilder.append(", digest=");
			localStringBuilder.append(this.digest);
		}
		if (this.uri != null) {
			localStringBuilder.append(", uri=");
			localStringBuilder.append(this.uri);
		}
		localStringBuilder.append("]");
		return localStringBuilder.toString();
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

	public static class AssetCreator implements Parcelable.Creator<Asset> {

		public Asset createFromParcel(Parcel in) {
			return new Asset(in);
		}

		public Asset[] newArray(int size) {
			return new Asset[size];
		}
	}
}