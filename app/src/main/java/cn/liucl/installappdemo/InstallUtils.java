package cn.liucl.installappdemo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Administrator on 2016/5/9.
 */
public class InstallUtils {

    public static final String TAG = InstallUtils.class.getSimpleName();
    public static final int APP_INSTALL_AUTO = 0;
    public static final int APP_INSTALL_INTERNAL = 1;
    public static final int APP_INSTALL_EXTERNAL = 2;

    ///////////////////////////////////////////////////////////////////////////
    // Root
    ///////////////////////////////////////////////////////////////////////////

    public static void installSilent(String filePath){
        installSilent(filePath,"-r " + getInstallLocationParams());
    }

    /**
     * root静默安装
     * @param filePath
     * @param pmParams
     */
    private static void installSilent(String filePath, String pmParams){
        if (filePath == null || filePath.length() == 0) {
            Log.i(TAG, "installSilent: error path");
            return;
        }

        File file = new File(filePath);
        if (file.length() <= 0 || !file.exists() || !file.isFile()) {
            Log.i(TAG, "installSilent:  error file");
            return;
        }
        //LD_LIBRARY_PATH 指定链接库位置 指定安装命令
        String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm install " +
                (pmParams == null ? "" : pmParams) +
                " " +
                filePath.replace(" ", "\\ ");
        //以root模式执行
        ShellUtils.CommandResult result = ShellUtils.execCommand(command, true, true);
        if (result.successMsg != null
                && (result.successMsg.contains("Success") || result.successMsg.contains("success"))) {
            Log.i(TAG, "installSilent: success");
        }
    }

    /**
     * root 静默卸载
     * @param packageName
     * @param isKeepData
     */
    public static void uninstallSilent(String packageName, boolean isKeepData){
        if (packageName == null) {
            Log.i(TAG, "uninstallSilent: error package");
            return;
        }
        String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm uninstall" +
                (isKeepData ? " -k " : " ") +
                packageName.replace(" ", "\\ ");
        ShellUtils.CommandResult result = ShellUtils.execCommand(command, true, true);
        if (result.successMsg != null
                && (result.successMsg.contains("Success") || result.successMsg.contains("success"))) {
            Log.i(TAG, "uninstallSilent: success");
        }
    }

    private static String getInstallLocationParams() {
        int location = getInstallLocation();
        switch (location) {
            case APP_INSTALL_INTERNAL:
                return "-f";
            case APP_INSTALL_EXTERNAL:
                return "-s";
            default:
                break;
        }
        return "";
    }

    public static int getInstallLocation() {
        ShellUtils.CommandResult commandResult = ShellUtils.execCommand("LD_LIBRARY_PATH=/vendor/lib:/system/lib pm get-install-location",
                false, true);
        if (commandResult.result == 0 && commandResult.successMsg != null && commandResult.successMsg.length() > 0) {
            try {
                int location = Integer.parseInt(commandResult.successMsg.substring(0, 1));
                switch (location) {
                    case APP_INSTALL_INTERNAL:
                        return APP_INSTALL_INTERNAL;
                    case APP_INSTALL_EXTERNAL:
                        return APP_INSTALL_EXTERNAL;
                    default:
                        break;
                }
            } catch (NumberFormatException e) {
                Log.d(TAG, "pm get-install-location error!!!  NumberFormatException :", e);
            }
        }
        return APP_INSTALL_AUTO;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 非Root
    ///////////////////////////////////////////////////////////////////////////


    public static boolean installNormal(Context context, String filePath) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        File file = new File(filePath);
        if (file == null || !file.exists() || !file.isFile() || file.length() <= 0) {
            return false;
        }

        i.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        return true;
    }

    public static boolean uninstallNormal(Context context, String packageName) {
        if (packageName == null || packageName.length() == 0) {
            return false;
        }

        Intent i = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        return true;
    }


    /**
     * 通过反射机制获取应用名称，通过其他方式有可能只得到包名
     * 只适应5.0以下版本
     *
     * @param ctx
     * @param apkPath
     * @return
     */
    public static String getAppNameByReflection(Context ctx, String apkPath) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {//|| !apkPath.toLowerCase().endsWith(".apk")
            return null;
        }
        String PATH_AssetManager = "android.content.res.AssetManager";
        try {
            Object pkgParserPkg = getPackage(apkPath);
            // 从返回的对象得到名为"applicationInfo"的字段对象
            if (pkgParserPkg == null) {
                return null;
            }
            Field appInfoFld = pkgParserPkg.getClass().getDeclaredField(
                    "applicationInfo");
            // 从对象"pkgParserPkg"得到字段"appInfoFld"的值
            if (appInfoFld.get(pkgParserPkg) == null) {
                return null;
            }
            ApplicationInfo info = (ApplicationInfo) appInfoFld.get(pkgParserPkg);

            // 反射得到assetMagCls对象并实例化,无参
            Class<?> assetMagCls = Class.forName(PATH_AssetManager);
            Object assetMag = assetMagCls.newInstance();
            // 从assetMagCls类得到addAssetPath方法
            Class[] typeArgs = new Class[1];
            typeArgs[0] = String.class;
            Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod(
                    "addAssetPath", typeArgs);
            Object[] valueArgs = new Object[1];
            valueArgs[0] = apkPath;
            // 执行assetMag_addAssetPathMtd方法
            assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);

            // 得到Resources对象并实例化,有参数
            Resources res = ctx.getResources();
            typeArgs = new Class[3];
            typeArgs[0] = assetMag.getClass();
            typeArgs[1] = res.getDisplayMetrics().getClass();
            typeArgs[2] = res.getConfiguration().getClass();
            Constructor resCt = Resources.class
                    .getConstructor(typeArgs);
            valueArgs = new Object[3];
            valueArgs[0] = assetMag;
            valueArgs[1] = res.getDisplayMetrics();
            valueArgs[2] = res.getConfiguration();
            res = (Resources) resCt.newInstance(valueArgs);

            PackageManager pm = ctx.getPackageManager();
            // 读取apk文件的信息
            if (info == null) {
                return null;
            }
            String appName;
            if (info.labelRes != 0) {
                appName = (String) res.getText(info.labelRes);
            } else {
                appName = info.loadLabel(pm).toString();
                if (TextUtils.isEmpty(appName)) {
                    appName = apkFile.getName();
                }
            }

            return appName;
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
        return null;
    }

    /**
     * 通过反射获取Package对象，用来获取ApplicationInfo对象
     * @param apkPath
     * @return
     * @throws Exception
     */
    private static Object getPackage(String apkPath) throws Exception {

        String PATH_PackageParser = "android.content.pm.PackageParser";

        Constructor<?> packageParserConstructor = null;
        Method parsePackageMethod = null;
        Object packageParser = null;
        Class<?>[] parsePackageTypeArgs = null;
        Object[] parsePackageValueArgs = null;

        Class<?> pkgParserCls = Class.forName(PATH_PackageParser);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            packageParserConstructor = pkgParserCls.getConstructor();//PackageParser构造器
            packageParser = packageParserConstructor.newInstance();//PackageParser对象实例
            parsePackageTypeArgs = new Class<?>[]{File.class, int.class};
            parsePackageValueArgs = new Object[]{new File(apkPath), 0};//parsePackage方法参数
        } else {
            Class<?>[] paserTypeArgs = {String.class};
            packageParserConstructor = pkgParserCls.getConstructor(paserTypeArgs);//PackageParser构造器
            Object[] paserValueArgs = {apkPath};
            packageParser = packageParserConstructor.newInstance(paserValueArgs);//PackageParser对象实例

            parsePackageTypeArgs = new Class<?>[]{File.class, String.class,
                    DisplayMetrics.class, int.class};
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            parsePackageValueArgs = new Object[]{new File(apkPath), apkPath, metrics, 0};//parsePackage方法参数

        }
        parsePackageMethod = pkgParserCls.getDeclaredMethod("parsePackage", parsePackageTypeArgs);
        // 执行pkgParser_parsePackageMtd方法并返回
        return parsePackageMethod.invoke(packageParser, parsePackageValueArgs);
    }

    public static String getAppNameByPackage(Context context,String packageName) {
        String appName = null;
        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
        for(int i=0;i<packages.size();i++) {
            PackageInfo packageInfo = packages.get(i);
            if (packageInfo.packageName.equals(packageName)) {
                ApplicationInfo info = packageInfo.applicationInfo;
                appName = info.loadLabel(context.getPackageManager()).toString();
            }
        }
        Log.i(TAG, "getAppNameByPackage: " + appName);
        return appName;
    }
}
