package cn.liucl.installappdemo;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Administrator on 2016/5/9.
 */
public class ServiceUtils {

    public static final String SERVICEUTILS = ServiceUtils.class.getSimpleName();

    /**
     * 判断AccessibilityService服务是否开启
     *
     * @param mContext
     * @return
     */
    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = "cn.liucl.installappdemo/cn.liucl.installappdemo.MyAccessibilityService";
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.i(SERVICEUTILS, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.i(SERVICEUTILS, "Error finding setting, default accessibility to not found: "
                    + e);
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.i(SERVICEUTILS, "***ACCESSIBILIY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();

                    Log.i(SERVICEUTILS, "-------------- > accessabilityService :: " + accessabilityService);
                    if (accessabilityService.equalsIgnoreCase(service)) {
                        Log.i(SERVICEUTILS, "We've found the correct setting - accessibility is switched on!");
                        accessibilityFound = true;
                    }
                }
            }
        } else {
            Log.i(SERVICEUTILS, "***ACCESSIBILIY IS DISABLED***");
        }

        return accessibilityFound;
    }
    
}
