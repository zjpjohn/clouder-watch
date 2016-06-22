package com.cms.android.wearable.service.common;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StorageUtils {
    private static final String TAG = "StorageUtils";
    private static File logFile;

    // 日志文件夹名称
    private final static String CLOUDWATCH_LOG_FILE = "cloudwatch_log_file";
    // 日志存放路径
    public static final String LOG_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "/cloudwatch_log_file/";

    public final static String SD_CARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator;

    public static boolean isSDExist() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else
            return false;
    }

    public static long getSDFreeSize() {
        // 取得SD卡文件路径
        File path = Environment.getExternalStorageDirectory();
        StatFs sf = new StatFs(path.getPath());
        // 获取单个数据块的大小(Byte)
        long blockSize = sf.getBlockSize();
        // 空闲的数据块的数量
        long freeBlocks = sf.getAvailableBlocks();
        // 返回SD卡空闲大小
        // return freeBlocks * blockSize; //单位Byte
        // return (freeBlocks * blockSize)/1024; //单位KB
        return (freeBlocks * blockSize) / 1024 / 1024; // 单位MB
    }

    public static long getSDAllSize() {
        // 取得SD卡文件路径
        File path = Environment.getExternalStorageDirectory();
        StatFs sf = new StatFs(path.getPath());
        // // 获取单个数据块的大小(Byte)
        // long blockSize = sf.getBlockSizeLong();
        // // 获取所有数据块数
        // long allBlocks = sf.getBlockCountLong();

        // 获取单个数据块的大小(Byte)
        long blockSize = sf.getBlockSize();
        // 获取所有数据块数
        long allBlocks = sf.getBlockCount();
        // 返回SD卡大小
        // return allBlocks * blockSize; //单位Byte
        // return (allBlocks * blockSize)/1024; //单位KB
        return (allBlocks * blockSize) / 1024 / 1024; // 单位MB
    }

    public static void isDirExist(String dir) {
        File file = new File(SD_CARD_ROOT + dir + File.separator);
        if (!file.exists())
            // 如果不存在则创建
            if (!file.mkdir()) {
                Log.w(TAG, "can not make dir");
            }

    }

    public static void delete(String fullPath) throws Exception {
        File file = new File(fullPath);
        if (file.exists()) {
            if (!file.delete()) {
                Log.w(TAG, "can not delete file " + file);
            }
        } else {
            throw new Exception("File is not existed");
        }
    }

    public static String createLogPath() throws Exception {
        String path;
        if (isSDExist()) {
            isDirExist(CLOUDWATCH_LOG_FILE);
            String fileName = "cloudwatch_phone_log.txt";
            path = LOG_FILE_PATH + fileName;
        } else {
            throw new Exception("No SD card exception");
        }
        return path;
    }

    public static void saveToSDCard(String content) throws Exception {
        FileOutputStream fos = null;
        try {
            content += getDate();
            // 获取sd卡的路径
            logFile = new File(createLogPath());
            if (!logFile.exists()) {
                boolean isSuccess = logFile.createNewFile();
                Log.d("Log", "logFile create isSuccess = " + isSuccess);
            }
            fos = new FileOutputStream(logFile, true);
            fos.write(content.getBytes());
            fos.flush();
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                LogTool.e("StorageUtils", Log.getStackTraceString(new Throwable()));
            }
        }
    }

    private static String getDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
        Date date = new Date();
        long time = date.getTime();
        String t1 = format.format(time);
        return t1;
    }

    public static File getFile() {
        return logFile;
    }
}
