package com.isxcwen.lmusic.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.Display;
import android.view.Gravity;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.isxcwen.lmusic.compone.TaskComponet;
import com.isxcwen.lmusic.server.MusicService;
import com.isxcwen.lmusic.utils.ConstantsUtil;
import com.isxcwen.lmusic.utils.LogUtils;

public abstract class MediaBrowserActivity extends Activity {
    protected MediaBrowserCompat mediaBrowser;
    protected MediaControllerCompat mediaControllerCompat;
    protected MediaControllerCompat.Callback controllerCallback;
    protected DisplayManager displayManager;
    protected Intent mediaBorwserServerIntent;

    protected volatile boolean registedAutoFinish = false;
    protected MediaBrowserActivity() {
        System.out.println();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //LogUtils.print("onCreate", this);
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        checkpermissionAndInitMusicService();
    }

    @Override
    protected void onStart() {
        //LogUtils.print("onStart", this);
        super.onStart();
    }

    @Override
    protected void onResume() {
        //LogUtils.print("onResume", this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        //LogUtils.print("onPause", this);
        super.onPause();
        if(!isRegistedAutoFinish()) {
            setRegistedAutoFinish(true);
            TaskComponet.registerDelay(() -> {
                try {
                    if (!isScreenOn() && needAutoFinish()) {
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setRegistedAutoFinish(false);
            }, ConstantsUtil.AUTO_FINISH_TIME);
        }
    }

    @Override
    protected void onStop() {
        //LogUtils.print("onStop", this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaBrowser != null && mediaBrowser.isConnected()){
            try {
                mediaBrowser.unsubscribe(mediaBrowser.getRoot());
                mediaBrowser.disconnect();
                //mediaBrowser = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(mediaControllerCompat !=null && controllerCallback != null){
            mediaControllerCompat.unregisterCallback(controllerCallback);
        }
        mediaControllerCompat = null;
        displayManager = null;
        mediaBorwserServerIntent = null;
        System.gc();
    }

    protected void checkpermissionAndInitMusicService() {
        if (needApplayPermission()) {
            //LogUtils.print("%s 申请权限", this);
            //请求权限后回调链接
            requestPermissions(ConstantsUtil.PERMISSIONS, 1);
        } else {
            //直接链接
            initMusicService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initMusicService();
        }
    }

    private boolean needApplayPermission() {
        for (String permission : ConstantsUtil.PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    private void initMusicService() {
        //LogUtils.print("%s 初始化 mediaBrowser", this);
        initMediaBrowser();
        //todo 是否需要启动
        if (needStartServer()) {
            mediaBorwserServerIntent = new Intent(getApplication(), MusicService.class);
            //startService(mediaBorwserServerIntent);
            startForegroundService(mediaBorwserServerIntent);
        }
        if (!mediaBrowser.isConnected()) {
            //LogUtils.print("准备链接MediaBrowserService", this);
            mediaBrowser.connect();
        }
    }

    private void initMediaBrowser() {
        mediaBrowser = new MediaBrowserCompat(getApplication(),
                new ComponentName(getApplication(), MusicService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        super.onConnected();
                        //LogUtils.print("%s 链接MediaBrowserServer成功", MediaBrowserActivity.this);
                        initController();

                        controllerCallback = generateMediaControllerCallback();
                        if (controllerCallback != null) {
                            mediaControllerCompat.registerCallback(controllerCallback);
                        }
                        //需要先取消注册在注册  bug
                        MediaBrowserCompat.SubscriptionCallback subscriptionCallback = generateMediaBrowserCompatSubscriptionCallback();
                        if (subscriptionCallback != null) {
                            mediaBrowser.unsubscribe(mediaBrowser.getRoot());
                            mediaBrowser.subscribe(mediaBrowser.getRoot(), subscriptionCallback);
                        }
                        connectSuccess();
                    }

                    @Override
                    public void onConnectionSuspended() {
                        super.onConnectionSuspended();
                    }

                    @Override
                    public void onConnectionFailed() {
                        super.onConnectionFailed();
                    }
                },
                null);
    }

    private void initController() {
        try {
            //获取控制器
            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
            mediaControllerCompat = new MediaControllerCompat(getApplication(), token);
            //保存控制器
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new RuntimeException("启动失败");
        }
    }

    protected void connectSuccess() {
    }

    protected MediaBrowserCompat.SubscriptionCallback generateMediaBrowserCompatSubscriptionCallback() {
        return null;
    }

    protected MediaControllerCompat.Callback generateMediaControllerCallback() {
        return null;
    }

    protected abstract boolean needStartServer();

    public boolean isScreenOn(){
        if(displayManager == null){
            return true;
        }
        Display[] displays = displayManager.getDisplays();
        for (Display display : displays) {
            if (display.getState () == Display.STATE_ON
                    || display.getState () == Display.STATE_UNKNOWN) {
                return true;
            }
        }
        return false;
    }

    protected boolean needAutoFinish(){
        return true;
    }


    public boolean isRegistedAutoFinish() {
        return registedAutoFinish;
    }

    public void setRegistedAutoFinish(boolean registedAutoFinish) {
        this.registedAutoFinish = registedAutoFinish;
    }
}
