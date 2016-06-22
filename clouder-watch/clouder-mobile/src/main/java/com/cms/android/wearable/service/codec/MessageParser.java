/**
 * **************************************************************************
 * <p/>
 * HOPERUN PROPRIETARY INFORMATION
 * <p/>
 * The information contained herein is proprietary to HopeRun
 * and shall not be reproduced or disclosed in whole or in part
 * or used for any design or manufacture
 * without direct written authorization from HopeRun.
 * <p/>
 * Copyright (c) 2013 by HopeRun.  All rights reserved.
 * <p/>
 * ***************************************************************************
 */
package com.cms.android.wearable.service.codec;

import com.cms.android.wearable.service.common.FileUtil;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.impl.MappedInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * ClassName: LinkParser
 *
 * @author hu_wg
 * @description
 * @Date Jan 15, 2013
 */
public class MessageParser {

    private static final String TAG = "MessageParser";

    private final static String KEY_SPLIT = " ";
    public final static byte KEY_REQUEST_START = 64;// @
    public final static byte KEY_RESPONSE_START = 36;// $
    public final static byte KEY_END_0 = 0x0d;// \r
    public final static byte KEY_END_1 = 0x0a;// \n

    public static MappedInfo dataPack(MessageData dataObj, boolean isRequest) {
        String filepath = FileUtil.createFilePath(dataObj.getUUID().toString());
        byte[] uuid = dataObj.getUUID() == null ? new byte[]{} : dataObj.getUUID().getBytes(Charset.forName("utf-8"));
        byte[] deviceNo = dataObj.getDeviceNo() == null ? new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0} : dataObj.getDeviceNo().getBytes(Charset.forName("utf-8"));
        byte[] command = dataObj.getCommand();
        byte[] data = dataObj.getData();
        byte[] timeStamp = ByteUtil.getByteArrayByLong(dataObj.getTimeStamp(), 6);
        byte[] packageName = dataObj.getPackageName() == null ? new byte[]{} : dataObj.getPackageName().getBytes(Charset.forName("utf-8"));
        byte[] path = dataObj.getPath() == null ? new byte[]{} : dataObj.getPath().getBytes(Charset.forName("utf-8"));
        byte[] nodeId = dataObj.getNodeId() == null ? new byte[]{} : dataObj.getNodeId().getBytes(Charset.forName("utf-8"));
        byte[] buf = new byte[77 + data.length + packageName.length + path.length + nodeId.length];
        buf[0] = isRequest ? KEY_REQUEST_START : KEY_RESPONSE_START;
        buf[1] = isRequest ? KEY_REQUEST_START : KEY_RESPONSE_START;
        int totalLength = 77 + data.length + packageName.length + path.length + nodeId.length;
        buf[2] = (byte) (totalLength & 0xFF); // L1
        buf[3] = (byte) ((totalLength >> 8) & 0xFF); // L2
        buf[4] = (byte) ((totalLength >> 16) & 0xFF); // L3
        buf[5] = (byte) ((totalLength >> 24) & 0xFF); // L4

        RandomAccessFile randomAccessFile = null;
        FileChannel fc = null;
        try {
            randomAccessFile = new RandomAccessFile(filepath, "rw");
            fc = randomAccessFile.getChannel();
            // 注意，文件通道的可读可写要建立在文件流本身可读写的基础之上
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, totalLength);
            buffer.put(buf[0]);
            buffer.put(buf[1]);
            buffer.put(buf[2]);
            buffer.put(buf[3]);
            buffer.put(buf[4]);
            buffer.put(buf[5]);
            // copy uuid
            buffer.put(uuid);
            // System.arraycopy(uuid, 0, buf, 6, uuid.length);
            // copy deviceNo
            // System.arraycopy(deviceNo, 0, buf, 42, deviceNo.length);
            buffer.put(deviceNo);
            buffer.position(62);
            buffer.put(command[0]);// command type
            buffer.put(command[1]);// command sub type
            // buf[62] = command[0];
            // buf[63] = command[1];
            // copy timeStamp
            buffer.put(timeStamp);
            // System.arraycopy(timeStamp, 0, buf, 64, timeStamp.length);
            // copy packageName
            buffer.position(70);
            buffer.put((byte) packageName.length);// packageName length
            buffer.put(packageName);
            // System.arraycopy(packageName, 0, buf, 71, packageName.length);
            // copy path
            buffer.position(71 + packageName.length);
            buffer.put((byte) path.length);// path length
            // buf[71 + packageName.length] = (byte) path.length;
            buffer.put(path);
            // System.arraycopy(path, 0, buf, 72 + packageName.length, path.length);
            // copy nodeId
            buffer.position(72 + packageName.length + path.length);
            buffer.put((byte) nodeId.length);// nodeId
            // buf[72 + packageName.length + path.length] = (byte) nodeId.length;
            // length
            buffer.position(73 + packageName.length + path.length);
            buffer.put(nodeId);
            // System.arraycopy(nodeId, 0, buf, 73 + packageName.length +
            // path.length, nodeId.length);
            // copy data
            buffer.position(73 + packageName.length + path.length + nodeId.length);
            buffer.put(data);
            byte[] crc = CRCUtil.makeCrcToBytes(filepath);
            buffer.put(crc[1]);// L
            buffer.put(crc[0]);// H
            buffer.put(KEY_END_0);// \r
            buffer.put(KEY_END_1);// \n

            buffer.position(0);
            MappedInfo mappedInfo = new MappedInfo(filepath, buffer);
            return mappedInfo;
        } catch (FileNotFoundException e) {
            LogTool.e(TAG, "FileNotFoundException", e);
        } catch (IOException e) {
            LogTool.e(TAG, "IOException", e);
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

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

    // Header Length Device ID Type Data Checksum Tail
    // A B C D E F G H I J K L M
    // A Header, 2 bytes, defined as @@, ASCII code
    // B Length, 2 bytes, indicates the data length from Header to Tail
    // ([Header, Tail)), statistical length range is
    // A,B,C,D,E,F,G,H,I,K,L,L,M
    // C UUID, 36bytes
    // D Device ID (LinkII product ID), 20 bytes, ASCII bytes. If length < 20
    // bytes, filled up with ‘\0’.
    // E Type, 2 bytes, high byte represents to main identifier and low byte
    // represent to sub identifier.
    // F TimeStamp, 6 bytes
    // G PackageName length, 1 bytes
    // H PackageName random length.
    // I Path length, 1 bytes
    // J Path random length.
    // K Data, random length.
    // L Checksum, 2 bytes, calculate from Header to Data excluding Checksum and
    // Tail. Checksum algorithm is
    // CRC Checksum Algorithm (see appendix), calculating range is
    // A,B,C,D,E,F,G,H,I,J,K
    // M Tail, 2 bytes, defined as “\r\n”.
    public static MappedInfo dataPack(MessageData dataObj){
        return dataPack((MessageData) dataObj, true);
    }

    // Header Length Device ID Type Data Checksum Tail
    // A B C D E F G
    // A Header, 2 bytes, defined as $$, ASCII code.
    // B Length, 2 bytes, indicates the data length from Header to Tail
    // ([Header, Tail)), statistical length range is
    // A,B,C,D,E,F,G
    // C UUID, 36bytes
    // D Device ID (LinkII product ID), 20 bytes, ASCII bytes. If length < 20
    // bytes, filled up with ‘\0’.
    // E Type, 2 bytes, high byte represents to main identifier and low byte
    // represent to sub identifier.
    // F TimeStamp, 6 bytes
    // G PackageName length, 1 bytes
    // H PackageName random length.
    // I Path length, 1 bytes
    // J Path random length.
    // K Data, random length.
    // L Checksum, 2 bytes, calculate from Header to Data excluding Checksum and
    // Tail. Checksum algorithm is
    // CRC Checksum Algorithm (see appendix), calculation range is A,B,C,D,E.
    // M Tail, 2 bytes, defined as “\r\n”.
    // 24 24
    // 20 00
    // 77 6b 68 68 6e 6a 36 31 30 30 31 32 34 35 30 30 33 36 34
    // 00
    // 22 08
    // 14 10 0c 12
    // 0d 0a
    public static MessageData dataUnpack(RandomAccessFile randomAccessFile) {
        MessageData dataObj = null;

        try {
            FileChannel fc = randomAccessFile.getChannel();
            // 注意，文件通道的可读可写要建立在文件流本身可读写的基础之上
            long size = randomAccessFile.getChannel().size();
            LogTool.d(TAG, "totalLength = " + size);
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, size);

            dataObj = new MessageData();
            byte[] uuids = new byte[36];
            buffer.position(6);
            buffer.get(uuids, 0, uuids.length);
            // System.arraycopy(dataPack, 6, uuid, 0, 36);
            byte[] deviceNos = new byte[20];
            buffer.position(42);
            buffer.get(deviceNos, 0, deviceNos.length);
            // System.arraycopy(dataPack, 42, deviceNos, 0, 20);
            byte[] command = new byte[2];
            buffer.position(62);
            command[0] = buffer.get();
            command[1] = buffer.get();
            // command[0] = dataPack[62];
            // command[1] = dataPack[63];
            byte[] timeStamps = new byte[6];
            buffer.get(timeStamps, 0, timeStamps.length);
            // System.arraycopy(dataPack, 64, timeStamps, 0, 6);
            buffer.position(70);
            int packageLength = buffer.get();
            // int packageLength = dataPack[70];
            byte[] packgeNames = new byte[packageLength];
            buffer.get(packgeNames, 0, packageLength);
            // System.arraycopy(dataPack, 71, packgeNames, 0, packageLength);
            buffer.position(71 + packageLength);
            int pathLength = buffer.get();
            // int pathLength = dataPack[71 + packageLength];
            byte[] paths = new byte[pathLength];
            buffer.position(72 + packageLength);
            buffer.get(paths, 0, pathLength);
            // System.arraycopy(dataPack, 72 + packageLength, paths, 0,
            // pathLength);
            int nodeIdLength = buffer.get();
            // int nodeIdLength = dataPack[72 + packageLength + pathLength];
            byte[] nodeIds = new byte[nodeIdLength];
            buffer.position(73 + packageLength + pathLength);
            buffer.get(nodeIds, 0, nodeIdLength);
            // System.arraycopy(dataPack, 73 + packageLength + pathLength,
            // nodeIds, 0, nodeIdLength);
            byte[] data = new byte[(int) (size - packageLength - pathLength - nodeIdLength - 77)];
            buffer.position(73 + packageLength + pathLength + nodeIdLength);
            buffer.get(data, 0, (int) (size - packageLength - pathLength - nodeIdLength - 77));

            dataObj.setUuid(new String(uuids, Charset.forName("UTF-8")));
            dataObj.setCommand(command);
            dataObj.setData(data);
            dataObj.setDeviceNo(new String(deviceNos, Charset.forName("UTF-8")));
            long timeStamp = (0xffL & (long) timeStamps[0]) | (0xff00L & ((long) timeStamps[1] << 8))
                    | (0xff0000L & ((long) timeStamps[2] << 16)) | (0xff000000L & ((long) timeStamps[3] << 24))
                    | (0xff00000000L & ((long) timeStamps[4] << 32)) | (0xff0000000000L & ((long) timeStamps[5] << 40));
            dataObj.setTimeStamp(timeStamp);
            dataObj.setPackageName(new String(packgeNames, Charset.forName("UTF-8")));
            dataObj.setPath(new String(paths, Charset.forName("UTF-8")));
            dataObj.setNodeId(new String(nodeIds, Charset.forName("UTF-8")));

        } catch (Exception e) {
            LogTool.e("", e.getMessage(), e);
        }
        return dataObj;
    }

    // TODO
    public static boolean checkDataPack(byte[] dataPack) {
        // check is valid pack
        if (dataPack.length < 77) {
            // error data pack
            return false;
        }

        // check pack length
        byte l0 = dataPack[2];
        byte l1 = dataPack[3];
        byte l2 = dataPack[4];
        byte l3 = dataPack[5];
        int length = ((l3 << 24) & 0xFF000000) | ((l2 << 16) & 0xFF0000) | ((l1 << 8) & 0xFF00) | l0 & 0xFF;
        if (dataPack.length != length) {
            // error data pack length
            return false;
        }

        // check crc
        byte[] abcde = new byte[dataPack.length - 4];
        System.arraycopy(dataPack, 0, abcde, 0, dataPack.length - 4);
        byte crc0 = dataPack[dataPack.length - 4];
        byte crc1 = dataPack[dataPack.length - 3];
        byte[] newCrc = CRCUtil.makeCrcToBytes(abcde);
        if (newCrc[0] != crc1 || newCrc[1] != crc0) {
            // error crc
            return false;
        }

        return true;
    }

    public static String generateKey(byte[] command, byte[] data) {
        StringBuffer sb = new StringBuffer();
        for (byte b : command) {
            String strData = Integer.toHexString(b);
            strData = strData.length() == 1 ? "0" + strData : strData;
            sb.append(strData).append(KEY_SPLIT);
        }
        for (byte b : data) {
            String strData = Integer.toHexString(b);
            strData = strData.length() == 1 ? "0" + strData : strData;
            sb.append(strData).append(KEY_SPLIT);
        }
        return sb.toString();
    }
}
