package com.clouder.watch.mobile.utils;

/**
 * Created by yang_shoulai on 7/28/2015.
 */
public class StringUtils {

    public static boolean isEmpty(String res) {
        return res == null || res.trim().length() == 0;
    }


    public static boolean isNotEmpty(String res) {
        return !isEmpty(res);
    }

    /**
     * 判断蓝牙地址是否有效
     *
     * @param address
     * @return
     */
    public static boolean isValidBluetoothAddress(String address) {
        if (address == null || address.length() != 17) {
            return false;
        }
        for (int i = 0; i < 17; i++) {
            char c = address.charAt(i);
            switch (i % 3) {
                case 0:
                case 1:
                    if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                        // hex character, OK
                        break;
                    }
                    return false;
                case 2:
                    if (c == ':') {
                        break;  // OK
                    }
                    return false;
            }
        }
        return true;
    }

    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }
}
