package org.andcreator.andview.uilt;

import android.os.Environment;

public class OtherUtil {

    public static String getSDLogPath(){
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }else {
            return "/storage/emulated/0";
        }

    }
}
