package com.cms.android.wearable.service.common;

import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class Utils {

    private static final String TAG = "Utils";

    // public static final String BLUETOOTH_BOND_NAME = "bluetooth_bond_name";

    public static final String BLUETOOTH_BOND_TOGGLE = "bluetooth_bond_toggle";

    public static final String BLUETOOTH_BOND_ADDRESS = "bluetooth_bond_address";

    public static final String SHARED_BLUETOOTH_BOND = "bluetooth_bond";

    public static final String SHARED_BLUETOOTH_DISCONNECT = "bluetooth_bond_disconnect";

    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            LogTool.e(TAG, "found service size = " + (resolveInfo == null ? 0 : resolveInfo.size()));
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    // /**
    // * save bluetooth bond name
    // *
    // * @param name
    // */
    // public static void saveSharedBondName(Context context, String name) {
    // SharedPreferences sharedPreferences =
    // context.getSharedPreferences(SHARED_BLUETOOTH_BOND,
    // Context.MODE_PRIVATE);
    // Editor editor = sharedPreferences.edit();
    // editor.putString(BLUETOOTH_BOND_NAME, name);
    // editor.commit();
    // }

    /**
     * save bluetooth bond address
     *
     * @param name
     */
    public static void saveSharedBondAddress(Context context, String address) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_BLUETOOTH_BOND, Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putString(BLUETOOTH_BOND_ADDRESS, address);
        editor.commit();
    }

    /**
     * get bluetooth bond name
     *
     * @return
     */
    // public static String getShardBondName(Context context) {
    // SharedPreferences shared =
    // context.getSharedPreferences(SHARED_BLUETOOTH_BOND,
    // Context.MODE_PRIVATE);
    // return shared.getString(BLUETOOTH_BOND_NAME, "");
    // }

    /**
     * get bluetooth bond address
     *
     * @return
     */
    public static String getShardBondAddress(Context context) {
        SharedPreferences shared = context.getSharedPreferences(SHARED_BLUETOOTH_BOND, Context.MODE_PRIVATE);
        return shared.getString(BLUETOOTH_BOND_ADDRESS, "");
    }

    /**
     * save bluetooth bond toggle
     *
     * @param name
     */
    public static void saveSharedBondToggle(Context context, boolean toggle) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_BLUETOOTH_BOND, Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(BLUETOOTH_BOND_TOGGLE, toggle);
        editor.commit();
    }

    /**
     * get bluetooth bond toggle
     *
     * @return
     */
    public static boolean getShardBondToggle(Context context) {
        SharedPreferences shared = context.getSharedPreferences(SHARED_BLUETOOTH_BOND, Context.MODE_PRIVATE);
        return shared.getBoolean(BLUETOOTH_BOND_TOGGLE, true);
    }


    /**
     * 当前配对的设备是否已经和其他设备配对
     *
     * @param context
     * @return
     */
    public static boolean getShardBondDisconnect(Context context) {
        SharedPreferences shared = context.getSharedPreferences(SHARED_BLUETOOTH_BOND, Context.MODE_PRIVATE);
        return shared.getBoolean(SHARED_BLUETOOTH_DISCONNECT, false);
    }

    public static void setShardBondDisconnect(Context context, boolean disconnect) {
        SharedPreferences shared = context.getSharedPreferences(SHARED_BLUETOOTH_BOND, Context.MODE_PRIVATE);
        Editor editor = shared.edit();
        editor.putBoolean(SHARED_BLUETOOTH_DISCONNECT, disconnect);
        editor.commit();
    }
}
