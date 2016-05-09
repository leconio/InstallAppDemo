package cn.liucl.installappdemo;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2016/5/9.
 */
public class FileUtils {

    private static File getSDPath() {
        File storageDirectory = Environment.getExternalStorageDirectory();
        File file = new File(storageDirectory, "appinstall");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static String getApk(Context context) {
        File file = new File(getSDPath(), "app.apk");
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = context.getAssets().open("app-debug.apk");
            os = new FileOutputStream(file);
            int len;
            byte[] b = new byte[1024];
            while ((len = is.read(b)) != -1) {
                os.write(b, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file.getAbsolutePath();
    }

}
