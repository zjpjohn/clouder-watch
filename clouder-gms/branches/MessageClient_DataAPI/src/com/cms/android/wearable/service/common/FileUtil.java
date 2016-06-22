package com.cms.android.wearable.service.common;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

import com.cms.android.wearable.service.common.LogTool;

public class FileUtil {

	private static final String TAG = "FileUtil";

	public static final String CLOUD_CACHE_DIRECTORY = "cloudwatchcache";

	/**
	 * 缓存最长存留时间
	 */
	public static final int MAX_CACHE_TIME = 24 * 60 * 60 * 1000;

	public static final String CLOUD_BASE_CACHE = Environment.getExternalStorageDirectory().getAbsolutePath() + "//"
			+ CLOUD_CACHE_DIRECTORY;

	public static String createFilePath(String name) {
		File baseDir = new File(CLOUD_BASE_CACHE);
		if (!baseDir.exists()) {
			baseDir.mkdir();
		}
		return CLOUD_BASE_CACHE + "//" + name;
	}

	public static MappedByteBuffer createBuffer(String name, long size) {
		RandomAccessFile randomAccessFile = null;
		try {
			File baseDir = new File(CLOUD_BASE_CACHE);
			if (!baseDir.exists()) {
				baseDir.mkdir();
			}
			String filepath = CLOUD_BASE_CACHE + "//" + name;
			LogTool.d(TAG, " basePath = " + CLOUD_BASE_CACHE + " filepath = " + filepath);
			randomAccessFile = new RandomAccessFile(filepath, "rw");
			FileChannel fc = randomAccessFile.getChannel();
			// 注意，文件通道的可读可写要建立在文件流本身可读写的基础之上
			MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, size);
			return buffer;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (randomAccessFile != null) {
				try {
					randomAccessFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;

	}

	public static void deletCacheFile() {
		File deleteDirectory = new File(CLOUD_BASE_CACHE);
		LogTool.d(TAG, "deleteDirectory->" + deleteDirectory);
		if (deleteDirectory.isDirectory()) {
			File[] files = deleteDirectory.listFiles();
			if (files != null) {
				for (File file : files) {
					String name = file.getName();
					long length = file.length();
					if ((new Date().getTime() - file.lastModified()) > MAX_CACHE_TIME) {
						boolean isSuccess = file.delete();
						LogTool.i(TAG, "delete file name = " + name + " length = " + length + " isSuccess = " + isSuccess);
					}
				}
			}
		} else {
			LogTool.d(TAG, "not a directory");
		}
	}

	public static void deleteCacheFile(String filename) {
		File baseDir = new File(CLOUD_BASE_CACHE);
		if (!baseDir.exists()) {
			baseDir.mkdir();
		}
		String filepath = CLOUD_BASE_CACHE + "//" + filename;
		LogTool.d(TAG, " basePath = " + CLOUD_BASE_CACHE + " filepath = " + filepath);
		File file = new File(filepath);
		if (file.exists()) {
			boolean isSuccess = file.delete();
			LogTool.i(TAG, "delete file name = " + filename + " isSuccess = " + isSuccess);
		}
	}
}