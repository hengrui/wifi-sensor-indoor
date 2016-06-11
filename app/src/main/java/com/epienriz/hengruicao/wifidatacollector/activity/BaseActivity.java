package com.epienriz.hengruicao.wifidatacollector.activity;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Created by hengruicao on 6/8/16.
 * Part of code from https://github.com/lasclocker/WiFi-and-Sensors-Indoor-Positioning
 */
public class BaseActivity extends AppCompatActivity{
    long lastClickTime     = 0;
    final long backSpaceTimeIntervalMilliSecond = 1000;

    @Override
    public void onBackPressed() {
		/**
		 * Backspace: when press back button, call this method.
		 */
        if (lastClickTime <= 0) {
            Toast.makeText(this, "再按一次后退键退出应用", Toast.LENGTH_SHORT).show();
            lastClickTime = System.currentTimeMillis();
        } else {
            long currentClickTime = System.currentTimeMillis();
            if (currentClickTime - lastClickTime < backSpaceTimeIntervalMilliSecond) {
                finish();
                // kill the process of the APP.
                android.os.Process.killProcess(android.os.Process.myPid());
            } else {
                Toast.makeText(this, "再按一次后退键退出应用", Toast.LENGTH_SHORT).show();
                lastClickTime = currentClickTime;
            }
        }
    }

    public static void displayToast(String str, Context context){
		/**
		 * make a toast.
		 */
        Toast.makeText(context, str, Toast.LENGTH_LONG).show();
    }

    public String getUserIdentifier() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set screen keep on always
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
		/*
		 * when the APP is destroyed, clear the flag of "keep screen on".
		 */
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }
}
