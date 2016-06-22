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

    public static final String BLUETOOTH_BOND_TOGGLE = "bluetooth_bond_toggle";

    public static final String SHARED_BLUETOOTH_BOND = "bluetooth_bond";

    public static final String SHARED_BLUETOOTH_BOND_ADDRESS = "bluetooth_bond_address";

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


    public static String getShardBondAddress(Context context) {
        SharedPreferences shared = context.getSharedPreferences(SHARED_BLUETOOTH_BOND, Context.MODE_PRIVATE);
        return shared.getString(SHARED_BLUETOOTH_BOND_ADDRESS, null);
    }

    public static void saveSharedBondAddress(Context context, String address) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_BLUETOOTH_BOND, Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putString(SHARED_BLUETOOTH_BOND_ADDRESS, address);
        editor.commit();
    }
}
