package com.cms.android.wearable.service.codec;

/*****************************************************************************
 * HOPERUN PROPRIETARY INFORMATION
 * <p/>
 * The information contained herein is proprietary to HopeRun
 * and shall not be reproduced or disclosed in whole or in part
 * or used for any design or manufacture
 * without direct written authorization from HopeRun.
 * <p/>
 * Copyright (c) 2013 by HopeRun.  All rights reserved.
 *****************************************************************************/

import android.os.Environment;
import android.util.Log;

import com.cms.android.wearable.service.common.LogTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.UUID;

/*****************************************************************************
 * HOPERUN PROPRIETARY INFORMATION
 * <p/>
 * The information contained herein is proprietary to HopeRun
 * and shall not be reproduced or disclosed in whole or in part
 * or used for any design or manufacture
 * without direct written authorization from HopeRun.
 * <p/>
 * Copyright (c) 2013 by HopeRun.  All rights reserved.
 *****************************************************************************/

/**
 * ClassName: LinkParser
 *
 * @author hu_wg
 * @description
 * @Date Jan 15, 2013
 */
public class TransportParser {

    private static final String TAG = "TransportDataParser";

    public final static byte REQUEST_PARSER_TYPE = 1;// 表示该报文为请求类型
    public final static byte RESPONSE_PARSER_TYPE = 2;// 表示该报文为返回类型

    public final static byte KEY_REQUEST_START = 38;// &
    public final static byte KEY_END_0 = 0x0d;// \r
    public final static byte KEY_END_1 = 0x0a;// \n

    public static byte[] dataPack(TransportData data) throws IOException {
        byte[] id = data.getId().getBytes();
        byte[] uuid = data.getUuid().getBytes();
        byte protocolType = data.getProtocolType();
        long contentLength = data.getContentLength();

        MappedByteBuffer buffer = data.getMappedInfo().getBuffer();
        int index = data.getIndex();
        int readLength = data.getReadLength();
        long count = data.getCount();
        int packIndex = data.getPackIndex();

        byte[] content = new byte[readLength];
        buffer.position(index);
        buffer.get(content, 0, readLength);
        // LogTool.e(TAG, "content = " + Arrays.toString(content));
        byte[] buf = new byte[2 + 4 + 1 + id.length + uuid.length + 1 + 4 + 4 + 4 + content.length + 2 + 2];
        buf[0] = KEY_REQUEST_START;
        buf[1] = KEY_REQUEST_START;
        // start 2bytes + length 4 bytes + request/response type 1byte + uuid
        // 36bytes + count 4bytes + index
        // 4bytes + content length + crc 2bytes + end 2bytes
        int length = 2 + 4 + 1 + id.length + uuid.length + 1 + 4 + 4 + 4 + content.length + 2 + 2;
        buf[2] = (byte) (length & 0xFF); // L1
        buf[3] = (byte) ((length >> 8) & 0xFF); // L2
        buf[4] = (byte) ((length >> 16) & 0xFF); // L3
        buf[5] = (byte) ((length >> 24) & 0xFF); // L4

        buf[6] = REQUEST_PARSER_TYPE;

        // 7-42 id 36位
        System.arraycopy(id, 0, buf, 7, id.length);

        // 43-78 uuid 36位
        System.arraycopy(uuid, 0, buf, 43, uuid.length);

        // 79 protocol type
        buf[79] = protocolType;

        // 80-83 package length
        buf[80] = (byte) (contentLength & 0xFF); // L1
        buf[81] = (byte) ((contentLength >> 8) & 0xFF); // L2
        buf[82] = (byte) ((contentLength >> 16) & 0xFF); // L3
        buf[83] = (byte) ((contentLength >> 24) & 0xFF); // L4

        // count
        buf[84] = (byte) (count & 0xFF); // L1
        buf[85] = (byte) ((count >> 8) & 0xFF); // L2
        buf[86] = (byte) ((count >> 16) & 0xFF); // L3
        buf[87] = (byte) ((count >> 24) & 0xFF); // L4

        // index
        buf[88] = (byte) (packIndex & 0xFF); // L1
        buf[89] = (byte) ((packIndex >> 8) & 0xFF); // L2
        buf[90] = (byte) ((packIndex >> 16) & 0xFF); // L3
        buf[91] = (byte) ((packIndex >> 24) & 0xFF); // L4

        // content
        System.arraycopy(content, 0, buf, 92, content.length);
        byte[] tmp = new byte[2 + 4 + 1 + id.length + uuid.length + 1 + 4 + 4 + 4 + content.length];
        System.arraycopy(buf, 0, tmp, 0, 2 + 4 + 1 + id.length + uuid.length + 1 + 4 + 4 + 4 + content.length);
        byte[] crc = CRCUtil.makeCrcToBytes(tmp);
        LogTool.e(TAG, "[dataPack] uuid = " + data.getUuid() + " crc = " + Arrays.toString(crc));
        buf[2 + 4 + 1 + id.length + uuid.length + 1 + 4 + 4 + 4 + content.length] = crc[1]; // L
        buf[2 + 4 + 1 + id.length + uuid.length + 1 + 4 + 4 + 4 + content.length + 1] = crc[0]; // H
        buf[2 + 4 + 1 + id.length + uuid.length + 1 + 4 + 4 + 4 + content.length + 2] = KEY_END_0; // \r
        buf[2 + 4 + 1 + id.length + uuid.length + 1 + 4 + 4 + 4 + content.length + 3] = KEY_END_1; // \n

        return buf;
    }

    public static TransportData dataUnpack(byte[] dataPack) {
        TransportData dataObj = null;


        // check data start and end when read byte[] from io.
        if (checkDataPack(dataPack)) {
            dataObj = new TransportData();

            // byte[] lengths = new byte[4];
            // System.arraycopy(dataPack, 2, lengths, 0, 4);
            // int l0 = lengths[0] < 0 ? lengths[0] + 256 : lengths[0];
            // int l1 = lengths[1] < 0 ? lengths[1] + 256 : lengths[1];
            // int l2 = lengths[2] < 0 ? lengths[2] + 256 : lengths[2];
            // int l3 = lengths[3] < 0 ? lengths[3] + 256 : lengths[3];
            // int length = ((l3 << 24) & 0xFF000000) | ((l2 << 16) &
            // 0xFF0000) | ((l1 << 8) & 0xFF00) | l0 & 0xFF;

            // 7-42 id 36位
            byte[] ids = new byte[36];
            System.arraycopy(dataPack, 7, ids, 0, 36);

            // 43-78 uuid 36位
            byte[] uuids = new byte[36];
            System.arraycopy(dataPack, 43, uuids, 0, 36);

            // 79 protocol type
            byte protocolType = dataPack[79];

            // 80-83 package length
            byte[] pls = new byte[4];
            System.arraycopy(dataPack, 80, pls, 0, 4);
            int pl0 = pls[0] < 0 ? pls[0] + 256 : pls[0];
            int pl1 = pls[1] < 0 ? pls[1] + 256 : pls[1];
            int pl2 = pls[2] < 0 ? pls[2] + 256 : pls[2];
            int pl3 = pls[3] < 0 ? pls[3] + 256 : pls[3];
            int packageLength = ((pl3 << 24) & 0xFF000000) | ((pl2 << 16) & 0xFF0000) | ((pl1 << 8) & 0xFF00) | pl0
                    & 0xFF;

            // 84-87 count
            byte[] counts = new byte[4];
            System.arraycopy(dataPack, 84, counts, 0, 4);
            int c0 = counts[0] < 0 ? counts[0] + 256 : counts[0];
            int c1 = counts[1] < 0 ? counts[1] + 256 : counts[1];
            int c2 = counts[2] < 0 ? counts[2] + 256 : counts[2];
            int c3 = counts[3] < 0 ? counts[3] + 256 : counts[3];
            int count = ((c3 << 24) & 0xFF000000) | ((c2 << 16) & 0xFF0000) | ((c1 << 8) & 0xFF00) | c0 & 0xFF;

            // 88-91 index
            byte[] indexs = new byte[4];
            System.arraycopy(dataPack, 88, indexs, 0, 4);
            int i0 = indexs[0] < 0 ? indexs[0] + 256 : indexs[0];
            int i1 = indexs[1] < 0 ? indexs[1] + 256 : indexs[1];
            int i2 = indexs[2] < 0 ? indexs[2] + 256 : indexs[2];
            int i3 = indexs[3] < 0 ? indexs[3] + 256 : indexs[3];
            int index = ((i3 << 24) & 0xFF000000) | ((i2 << 16) & 0xFF0000) | ((i1 << 8) & 0xFF00) | i0 & 0xFF;

            // content
            int subcontentLength = dataPack.length - 2 - 4 - 1 - ids.length - uuids.length - 1 - 4 - 4 - 4 - 2 - 2;
            byte[] content = new byte[subcontentLength];
            if (subcontentLength > 0) {
                System.arraycopy(dataPack, 2 + 4 + 1 + ids.length + uuids.length + 1 + 4 + 4 + 4, content, 0,
                        subcontentLength);
            }
            dataObj.setId(new String(ids));
            String uuid = new String(uuids);
            dataObj.setUuid(uuid);
            dataObj.setProtocolType(protocolType);
            dataObj.setContentLength(packageLength);
            dataObj.setCount(count);
            dataObj.setIndex(index);

            // check crc
            byte[] abcde = new byte[dataPack.length - 4];
            System.arraycopy(dataPack, 0, abcde, 0, dataPack.length - 4);
            byte[] newCrc = CRCUtil.makeCrcToBytes(abcde);
            LogTool.e(TAG, "[dataUnPack] uuid = " + dataObj.getUuid() + " crc = " + Arrays.toString(newCrc));

            String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "//cloudwatchcache";
            File baseDir = new File(basePath);
            if (!baseDir.exists()) {
                if (!baseDir.mkdir()) {
                    Log.w(TAG, "can not make dirs");
                }
            }
            String filepath = basePath + "//" + dataObj.getUuid();
            LogTool.d(TAG, " basePath = " + basePath + " filepath = " + filepath);
            RandomAccessFile randomAccessFile = null;
            try {
                randomAccessFile = new RandomAccessFile(filepath, "rw");
                FileChannel fc = randomAccessFile.getChannel();
                // 注意，文件通道的可读可写要建立在文件流本身可读写的基础之上
                MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, dataObj.getContentLength());
                int position = 4196 * index;
                buffer.position(position);
                LogTool.e(TAG, "allcontentLength = " + dataObj.getContentLength() + " position = " + position + " "
                        + " contentLength = " + subcontentLength + " content length = " + content.length);
                buffer.put(content, 0, subcontentLength);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
        } else {
            // System.out.println("dataUnpack datat is invalid.");
            LogTool.e(TAG, "dataUnpack data is invalid.");
        }
        return dataObj;
    }

    public static boolean checkDataPack(byte[] dataPack) {
        // check is valid pack
        // if (dataPack.length < 2 + 4 + 1 + 36 + 36 + 1 + 4 + 4 + 4 + 2 + 2) {
        // // error data pack
        // return false;
        // }
        // 开头2bytes+长度4bytes+request/response+crc2bytes+结尾2bytes
        if (dataPack.length < 2 + 4 + 1 + 2 + 2) {
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
}
