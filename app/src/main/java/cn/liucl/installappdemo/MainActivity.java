package cn.liucl.installappdemo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void installWithRoot(View view) {
        new Thread() {
            @Override
            public void run() {
                final String apk = FileUtils.getApk(MainActivity.this);
                InstallUtils.installSilent(apk);
            }
        }.start();
    }

    public void uninstallWithRoot(View view) {
        final String packageName = "cn.liucl.videocapture";
        new Thread() {
            @Override
            public void run() {
                InstallUtils.uninstallSilent(packageName, false);
            }
        }.start();
    }


    public void installWithoutRoot(View view) {
        if (!hasEnv()) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent,0);
        } else {
            startInstall();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0  && hasEnv()) {
            startInstall();
        }
    }

    private void startInstall() {
        new Thread() {
            @Override
            public void run() {
                final String apk = FileUtils.getApk(MainActivity.this);
                String appName = apk.substring(apk.lastIndexOf("/") + 1, apk.lastIndexOf("."));
                Log.i(TAG, "run: " + appName);
                MyAccessibilityService.addInstalledWhitelList(InstallUtils.getAppNameByReflection(MainActivity.this,apk));
                InstallUtils.installNormal(MainActivity.this,apk);
            }
        }.start();
    }

    public void uninstallWithoutRoot(View view) {
        new Thread(){
            @Override
            public void run() {
                String packageName = "cn.liucl.videocapture";
                MyAccessibilityService.addUnInstalledWhitelList(InstallUtils.getAppNameByPackage(MainActivity.this,packageName));
                InstallUtils.uninstallNormal(MainActivity.this,packageName);
            }
        }.start();
    }

    private boolean hasEnv() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ServiceUtils.isAccessibilitySettingsOn(MainActivity.this);
    }
}
