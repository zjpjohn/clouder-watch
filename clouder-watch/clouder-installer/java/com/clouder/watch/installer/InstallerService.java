package com.clouder.watch.installer;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.lang.Exception;

/**
 * Created by yang_shoulai on 9/7/2015.
 */
public class InstallerService extends IntentService {


    private static final String TAG = "InstallerService";

    public InstallerService(String name) {
        super(TAG);
    }

    public InstallerService() {
        this(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");
        boolean pairedDeviceChange = intent.getBooleanExtra("paired_device_change", false);
        Log.i(TAG, "pairedDeviceChange ? " + pairedDeviceChange);
        if (!pairedDeviceChange) {
            String filePath = intent.getStringExtra("apk_file_path");
            String packageName = intent.getStringExtra("package");
            boolean install = intent.getBooleanExtra("install", true);
            Log.e(TAG, "filePath = " + filePath + ", packageName = " + packageName + ", install = " + install);
            if (install) {
                if ((filePath == null || filePath.trim().length() == 0)) {
                    Log.e(TAG, "install app failed!");
                    return;
                } else {
                    try {
                        installApkBackground(filePath, packageName);

                    } catch (Exception e) {
                        Log.e(TAG, "install app failed!", e);
                    }
                }

            } else {
                try {
                    uninstallApkDefaul(packageName);
                } catch (Exception e) {
                    Log.e(TAG, "uninstall app failed!", e);
                }

            }
        } else {
            String str = getInstalledPackages();
            Log.d(TAG, "Installed Packages = " + str);
            if (str != null && !"".equals(str)) {
                String[] array = str.split(",");
                if (array.length > 0) {
                    for (String pkg : array) {
                        uninstallApkDefaul(pkg);
                    }
                }
            }
            clearInstalledPackages();
        }
    }

    /**
     * install apk background
     *
     * @param fileAbsolutePath
     * @param packageName
     */
    private void installApkBackground(String fileAbsolutePath, String packageName) {
        Log.i(TAG, "Install apk [" + fileAbsolutePath + "] background!");
        File file = new File(fileAbsolutePath);
        if (!file.exists()) {
            return;
        }

        int installFlags = 0;
        Uri packageUri = Uri.fromFile(new File(fileAbsolutePath));
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            if (pi != null) {
                installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("debug", "NameNotFoundException--" + e.getMessage());
        }
        ClouderPackageInstallObserver observer = new ClouderPackageInstallObserver();
        pm.installPackage(packageUri, observer, installFlags, packageName);
    }


    /**
     * uninstall apk
     *
     * @param packageName
     */
    public void uninstallApkDefaul(String packageName) {
        PackageManager pm = getPackageManager();
        ClouderPackageDeleteObserver observer = new ClouderPackageDeleteObserver();
        pm.deletePackage(packageName, observer, 0);
    }

    private class ClouderPackageInstallObserver extends IPackageInstallObserver.Stub {
        @Override
        public void packageInstalled(String packageName, int returnCode)
                throws RemoteException {
            if (returnCode == 1) {
                Log.d(TAG, "apk [" + packageName + "] installed success");
                saveInstalledPackage(packageName);
            } else {
                Log.e(TAG, "apk [" + packageName + "] install failed!");
            }
        }
    }


    private class ClouderPackageDeleteObserver extends IPackageDeleteObserver.Stub {
        @Override
        public void packageDeleted(String packageName, int returnCode) {
            if (returnCode == 1) {
                Log.d(TAG, "apk [" + packageName + "] uninstall success");
            } else {
                Log.e(TAG, "apk [" + packageName + "] uninstall failed!");
            }
        }

    }

    private SharedPreferences getClouderInstallerSharedPreferences() {
        return this.getSharedPreferences("CLOUDER_INSTALLER", Context.MODE_PRIVATE);
    }

    private void saveInstalledPackage(String packageName) {
        SharedPreferences sharedPreferences = getClouderInstallerSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String packages = getInstalledPackages();
        if (packages == null) {
            editor.putString("installed_packages", packageName);
        } else {
            boolean has = false;
            String[] array = packages.split(",");
            if (array.length > 0) {
                for (String pkg : array) {
                    if (pkg.equals(packageName)) {
                        has = true;
                        break;
                    }
                }
            }
            if (!has) {
                editor.putString("installed_packages", packages + "," + packageName);
            }
        }
        editor.commit();
    }

    private String getInstalledPackages() {
        SharedPreferences sharedPreferences = getClouderInstallerSharedPreferences();
        String str = sharedPreferences.getString("installed_packages", null);
        return str;
    }

    private void clearInstalledPackages() {
        SharedPreferences sharedPreferences = getClouderInstallerSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("installed_packages", null);
        editor.commit();
    }
}