package com.isxcwen.lmusic.utils;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class LogUtils {
    private static final String TAG = "L-Music";
    public static void print(String content){
        Log.d(TAG, content);
    }

    public static void printFormat(String format, Object... o){
        Log.d(TAG, String.format(format, o));
    }

    public static void print(String str, Context context){
        Log.d(TAG, context.getClass().getSimpleName() + "  " + str);
    }


    public static void showToast(Context context, String str, int time){
        Toast toast = Toast.makeText(context, "获取中...", time);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();
    }
}
