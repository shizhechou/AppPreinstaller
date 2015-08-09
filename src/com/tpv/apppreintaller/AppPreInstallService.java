
package com.tpv.apppreintaller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

//TODO item
/*
 * 1. use xml parser to check file copy source and destination
 * 2. check file is the same
 * 3. log controller use sharePreference?
 * 
 */
public class AppPreInstallService extends Service {
    private static final String LOG_TAG = "AppPreInstaller";

    private static final String ADOBE_READER_APK_DIR = "/sdcard/Download/AdobeReader.apk";
    private static final String ADOBE_PACKAGE_NAME = "com.adobe.reader";

    private static final int INSTALL_APK_FAIL_OTHER = 2;

    private static final String sPreferencesName = "com.tpv.apppreinstaller_preferences";
    private SharedPreferences mFlagPreferences = null;
    private SharedPreferences.Editor preferenceEditor = null;
    private static final String KEY_ADOBE_APP_INSTALLED_FLAG = "pref_adobe_app_installed_key";

    Thread mInstallAdobeApkThread = new Thread(new Runnable() {

        @Override
        public void run() {
            installPMApk(ADOBE_PACKAGE_NAME, ADOBE_READER_APK_DIR);
        }

    });

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constant.INSTALL_SUCCEEDED:

                    if (mFlagPreferences != null && preferenceEditor != null) {
                        if (msg.obj.equals(ADOBE_PACKAGE_NAME)) {
                            Log.i(LOG_TAG, "install " + ADOBE_PACKAGE_NAME
                                    + " apk successfully");
                            preferenceEditor.putBoolean(KEY_ADOBE_APP_INSTALLED_FLAG, true);
                            preferenceEditor.apply();
                        }
                    }

                    break;

                case Constant.INSTALL_FAILED_ALREADY_EXISTS:
                    Log.i(LOG_TAG, "apk has been installed before");

                    break;
                // case INSTALL_APK_FAIL:
                case Constant.INSTALL_FAILED_INVALID_APK:
                    Log.i(LOG_TAG, "install apk failed");

                    break;

                case Constant.INSTALL_FAILED_INSUFFICIENT_STORAGE:
                    Log.i(LOG_TAG, "install apk don't have enough storage space.");

                    break;

                case INSTALL_APK_FAIL_OTHER:
                    Log.i(LOG_TAG, "install apk failed caused by other reason");

                    break;

                default:
                    Log.i(LOG_TAG, "msg.what : " + msg.what);
                    stopSelf();
                    break;
            }

        }
    };

    private void installPMApk(String package_name, String apk_dir) {

        PackageInstallObserver observer = new PackageInstallObserver();
        if (mFlagPreferences == null) {
            // return INSTALL_APK_FAIL_OTHER;
            observer.packageInstalled(package_name, INSTALL_APK_FAIL_OTHER);
            return;
        }
        // Check apk install as before or not.
        boolean installedFlag = false;
        if (package_name.equals(ADOBE_PACKAGE_NAME)) {
            installedFlag = mFlagPreferences.getBoolean(KEY_ADOBE_APP_INSTALLED_FLAG, false);
        }

        if (installedFlag) {
            // return PackageManager.INSTALL_FAILED_ALREADY_EXISTS;
            observer.packageInstalled(package_name, Constant.INSTALL_FAILED_ALREADY_EXISTS);
            return;
        }

        int installFlags = 0;
        Uri mPackageURI = Uri.fromFile(new File(apk_dir));

        PackageManager pm = getPackageManager();

        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(package_name, PackageManager.GET_UNINSTALLED_PACKAGES);

            if (pi != null) {
                installFlags |= Constant.INSTALL_REPLACE_EXISTING;
            }
        } catch (NameNotFoundException e1) {
            e1.printStackTrace();
        }
        if ((installFlags & Constant.INSTALL_REPLACE_EXISTING) != 0) {
            Log.w(LOG_TAG, "Replacing existing package : " + package_name);

            // pm.installExistingPackage(package_name);
            try {
                Method installExistingPackage = pm.getClass().getMethod("installExistingPackage",
                        String.class);
                installExistingPackage.invoke(pm, package_name);

                observer.packageInstalled(package_name,
                        Constant.INSTALL_SUCCEEDED);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            Log.w(LOG_TAG, "install package:" + package_name);
            // pm.installPackage(mPackageURI, observer, installFlags,
            // package_name);
            try {
                Method installPackage = pm.getClass().getMethod("installPackage",
                        Uri.class, IPackageInstallObserver.class, int.class, String.class);
                Object[] args = new Object[] {
                        mPackageURI, observer, installFlags, package_name
                };

                installPackage.invoke(pm, args);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String packageName, int returnCode) {
            Log.i(LOG_TAG, "install complete, return code:" + returnCode);

            Message msg = new Message();
            msg.obj = packageName;
            msg.what = returnCode;
            mHandler.sendMessage(msg);

        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFlagPreferences = getSharedPreferences(sPreferencesName, Context.MODE_WORLD_READABLE);
        preferenceEditor = mFlagPreferences.edit();
        mInstallAdobeApkThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

}
