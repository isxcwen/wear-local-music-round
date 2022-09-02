package com.isxcwen.lmusic.compone;

import static androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.wear.ongoing.OngoingActivity;
import androidx.wear.ongoing.Status;

import com.isxcwen.lmusic.activity.MainActivity;
import com.isxcwen.lmusic.R;
import com.isxcwen.lmusic.utils.ConstantsUtil;
import com.isxcwen.lmusic.model.CurrentPlayInfo;
import static com.isxcwen.lmusic.utils.ConstantsUtil.NotifyConstants.*;

public class WearNotifyComponet {
    private MediaSessionCompat.Token token;
    private NotificationManager notificationManager;
    private NotificationChannel notificationChannel;
    private OngoingActivity ongoingActivity;
    private NotificationCompat.Builder builder;

    public WearNotifyComponet(Service service, MediaSessionCompat.Token token, CurrentPlayInfo currentPlayInfo) {
        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        this.token = token;
        showForegroundNotify(service, currentPlayInfo);
        cancleForegroundNotify(service);
    }

    public void cancleNotify(){
        //获取通知管理器
        notificationManager.cancel(1);
        //Log.d("WearNotifyCompone", "cancle norify");
    }

    public void destory(Service service){
        try {
            notificationManager.deleteNotificationChannel(CHANNEL_ID);
            cancleNotify();
        } catch (Exception e) {
            //e.printStackTrace();
        }finally {
            token = null;
            ongoingActivity = null;
            builder = null;
            notificationChannel = null;
            notificationManager = null;
        }
    }

    public void cancleForegroundNotify(Service service){
        service.stopForeground(true);
    }
    public void showForegroundNotify(Service service , CurrentPlayInfo currentPlayInfo) {
        //Log.d("WearNotifyCompone", "create norify");
        //构建通知
        if(builder == null){
            initNotifyBuild(service);
        }

        String musicName = currentPlayInfo.getMusicName();
        String singName = currentPlayInfo.getSingName();
        boolean play = currentPlayInfo.isPlay();
        if(currentPlayInfo == null){
            musicName = "获取失败";
            singName = "";
        }
        /*builder.setContentTitle(musicName)
                .setContentText(singName);*/
        //ffeaed
        if(ongoingActivity == null){
            //wear独特的持续通知
            initOngoingActivity(service, play, musicName);
        }else {
            Status ongoingActivityStatus = ongoingActivityStatus(play, musicName);
            ongoingActivity.update(service, ongoingActivityStatus);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //创建channel
            if(notificationChannel == null){
                notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH));
            }
        }
        Notification build = builder.build();
        notificationManager.notify(1, build);
        service.startForeground(1, build);
    }

    private void initNotifyBuild(Service service){
        androidx.media.app.NotificationCompat.MediaStyle style = new androidx.media.app.NotificationCompat.MediaStyle();
        style.setMediaSession(token);
        builder = new NotificationCompat.Builder(service, CHANNEL_ID)
                .setContentIntent(PendingIntent.getActivity(service, 1, new Intent(service, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .setStyle(style)
                .setCategory(CATEGORY_TRANSPORT)
                .setSmallIcon(R.drawable.ico_notify)
                .setOngoing(true);
    }

    private void initOngoingActivity(Service service , boolean isPlay, String musicName){
        Status ongoingActivityStatus = ongoingActivityStatus(isPlay, musicName);
        ongoingActivity = new OngoingActivity.Builder(service, NOTIFY_ID, builder)
                //.setTouchIntent(PendingIntent.getActivity(service, 1, new Intent(service, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .setStatus(ongoingActivityStatus)
                //.setCategory(CATEGORY_TRANSPORT)
                .build();
        ongoingActivity.apply(service);
    }

    private static Status ongoingActivityStatus(boolean isPlay, String musicName){
        //CharSequence playingMusicName = currentPlayInfo == null ? "unknow" : currentPlayInfo.getMusicName();
        Status ongoingActivityStatus = new Status.Builder()
                // Sets the text used across various surfaces.
                //.addTemplate(ConstantsUtil.NotifyConstants.SHOW_PERFIX + playingMusicName)
                //.addTemplate((isPlay ? ConstantsUtil.NotifyConstants.PLAY_PERFIX : ConstantsUtil.NotifyConstants.PAUSE_PERFIX) + " - " + musicName)
                .addTemplate(musicName)
                .build();
        return ongoingActivityStatus;
    }
}
