package com.hoperun.watch.vo;

import android.os.Parcel;
import android.os.Parcelable;

public class AppInfo implements Parcelable {

	private String fileName;

	private String packageName;

	private String versionName;

	private int versionCode;

	private String checksum;

	private long fileLength;

	public AppInfo() {
		super();
	}

	private AppInfo(Parcel in) {
		fileName = in.readString();
		packageName = in.readString();
		versionName = in.readString();
		versionCode = in.readInt();
		checksum = in.readString();
		fileLength = in.readLong();
	}

	public static final Parcelable.Creator<AppInfo> CREATOR = new Parcelable.Creator<AppInfo>() {
		public AppInfo createFromParcel(Parcel in) {
			return new AppInfo(in);
		}

		public AppInfo[] newArray(int size) {
			return new AppInfo[size];
		}
	};

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public long getFileLength() {
		return fileLength;
	}

	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}

	@Override
	public String toString() {
		return "AppInfo [fileName=" + fileName + ", packageName=" + packageName + ", versionName=" + versionName
				+ ", versionCode=" + versionCode + ", checksum=" + checksum + ", fileLength=" + fileLength + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(fileName);
		dest.writeString(packageName);
		dest.writeString(versionName);
		dest.writeInt(versionCode);
		dest.writeString(checksum);
		dest.writeLong(fileLength);
	}

}
