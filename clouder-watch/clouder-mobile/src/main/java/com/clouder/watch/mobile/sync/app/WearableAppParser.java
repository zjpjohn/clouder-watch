package com.clouder.watch.mobile.sync.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 手表应用解析器
 * 手机端打包android wear应用的步骤：
 * 1、签名拷贝Android Wear应用apk至手机应用的res/raw目录下，apk重命名为 wearable_app.apk
 * <p/>
 * 2、在手机端app目录res/xml/wearable_app_desc.xml文件，里面包含Android Wear应用的版本和路径信息
 * Example:
 * <wearableApp package="wearable.app.package.name">
 * <versionCode>1</versionCode>
 * <versionName>1.0</versionName>
 * <rawPathResId>wearable_app</rawPathResId>
 * </wearableApp>
 * <p/>
 * 3、在手机端app的AndroidManifest.xml的<application>标签下添加<meta-data>节点用于引用wearable_app_desc.xml描述文件
 * Example:
 * <meta-data android:name="com.google.android.wearable.beta.app" android:resource="@xml/wearable_app_desc"/>
 * <p/>
 * 4、签名编译手机端app
 * <p/>
 * 将手表端应用从手机端app中解压出来的步骤：
 * <p/>
 * 1、遍历得到所有的已安装应用
 * 2、遍历所有已安装应用，解析得到包含name为"com.google.android.wearable.beta.app"的<meta-data></meta-data>节点的app
 * 3、解析wearable_app_desc文件，取回手表端应用的信息
 * 4、在res/raw文件夹下取回 wearable_app.apk
 * <p/>
 * <p/>
 * <p/>
 * Created by yang_shoulai on 8/28/2015.
 */
public class WearableAppParser {

    private static final String TAG = "WearableAppParser";

    public static final String KEY_WEARABLE_META_DATA = "com.google.android.wearable.beta.app";

    public static final String TAG_WEARABLE_APP = "wearableApp";

    public static final String TAG_WEARABLE_ATTR_PKG = "package";

    public static final String TAG_VERSION_CODE = "versionCode";

    public static final String TAG_VERSION_NAME = "versionName";

    public static final String TAG_APP_NAME_ID = "rawPathResId";


    private Context mContext;

    private PackageManager mPackageManager;


    public WearableAppParser(Context context) {

        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }


    /**
     * 获取所有已安装应用列表
     *
     * @return
     */
    private List<PackageInfo> loadAllInstalledApps() {

        return this.mPackageManager.getInstalledPackages(0);
    }

    /**
     * 从所有的已安装应用中解析得到所有可能的wearable app
     *
     * @return
     */
    private List<WearableApp> loadAllAppsContainsWearable(List<PackageInfo> list) {
        Log.d(TAG, "Found total " + (list == null ? 0 : list.size()) + "app(s) installed in this device.");
        List<WearableApp> wearableList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            for (PackageInfo info : list) {
                WearableApp wearableApp = loadWearableApp(info.packageName);
                if (wearableApp != null) {
                    wearableList.add(wearableApp);
                }
            }
        }
        Log.d(TAG, "Load " + wearableList.size() + " wearable app in this device.");
        return wearableList;
    }


    /**
     * 解析wearable_app_desc文件取回wearable app的详细信息
     */
    private List<WearableApp> loadDetailWearableAppInfo(List<WearableApp> list) {
        Iterator<WearableApp> iterator = list.iterator();
        while (iterator.hasNext()) {
            WearableApp app = iterator.next();
            WearableApp wearableApp = loadDetailWearableAppInfo(app);
            if (wearableApp.wearableAppName == null || wearableApp.wearableAppName.trim().length() == 0) {
                iterator.remove();
            }
        }
        return list;
    }

    /**
     * 解析出所有的Wearable app信息
     *
     * @return
     */
    public List<WearableApp> parse() {
        List<PackageInfo> allApps = loadAllInstalledApps();
        List<WearableApp> allWearableApps = loadAllAppsContainsWearable(allApps);
        return loadDetailWearableAppInfo(allWearableApps);
    }


    /**
     * 取回apk, apk的位置总是在res/raw文件夹中
     * <p/>
     * apk大小需要注意
     *
     * @param mobileAppPackage
     * @param wearableAppName
     * @return
     */
    public byte[] loadWearableApk(String mobileAppPackage, String wearableAppName) {
        InputStream inputStream = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream outstream = null;
        try {
            Context otherContext = mContext.createPackageContext(mobileAppPackage, Context.CONTEXT_IGNORE_SECURITY);
            int id = otherContext.getResources().getIdentifier(wearableAppName, "raw", mobileAppPackage);
            inputStream = otherContext.getResources().openRawResource(id);
            bis = new BufferedInputStream(inputStream);
            outstream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            return outstream.toByteArray();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "PackageManager.NameNotFoundException", e);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resources.NotFoundException", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outstream != null) {
                try {
                    outstream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public WearableApp loadWearableApp(String mobilePackageName) {
        try {
            ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(mobilePackageName, PackageManager.GET_META_DATA);
            if (applicationInfo.metaData != null) {
                Object obj = applicationInfo.metaData.get(KEY_WEARABLE_META_DATA);
                if (obj != null && obj instanceof Integer) {
                    WearableApp wearableApp = new WearableApp();
                    wearableApp.descXmlId = (Integer) obj;
                    wearableApp.mobileAppPackage = mobilePackageName;
                    return wearableApp;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "PackageManager.NameNotFoundException", e);
        }
        return null;
    }

    public WearableApp loadDetailWearableAppInfo(WearableApp wearableApp) {
        try {
            Context context = this.mContext.createPackageContext(wearableApp.mobileAppPackage, Context.CONTEXT_IGNORE_SECURITY);
            XmlResourceParser xmlResourceParser = context.getResources().getXml(wearableApp.descXmlId);
            int eventType = xmlResourceParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d(TAG, "Document Parse Start!");
                } else if (eventType == XmlPullParser.START_TAG) {
                    String tagName = xmlResourceParser.getName();
                    if (TAG_WEARABLE_APP.equals(tagName)) {
                        int count = xmlResourceParser.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            String name = xmlResourceParser.getAttributeName(i);
                            if (name.equals(TAG_WEARABLE_ATTR_PKG)) {
                                String value = xmlResourceParser.getAttributeValue(i);
                                wearableApp.wearablePackage = value;
                            }
                        }
                    } else if (TAG_VERSION_CODE.equals(tagName)) {
                        wearableApp.versionCode = xmlResourceParser.nextText();
                    } else if (TAG_VERSION_NAME.equals(tagName)) {
                        wearableApp.versionName = xmlResourceParser.nextText();
                    } else if (TAG_APP_NAME_ID.equals(tagName)) {
                        wearableApp.wearableAppName = xmlResourceParser.nextText();
                    }
                }
                eventType = xmlResourceParser.next();
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "PackageManager.NameNotFoundException", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParserException", e);
        } catch (IOException e) {
            Log.e(TAG, "XmlPullParserException", e);
        }

        return wearableApp;
    }


    public static class WearableApp {

        public String mobileAppPackage; //手机端应用的包名

        public int descXmlId; //手表应用的xml说明文件id

        public String wearablePackage; //手表应用的包名

        public String wearableAppName; //手表应用在手机工程中的资源名称

        public String versionCode; //手表应用的版本号

        public String versionName;//手表应用的版本名称

    }

}
