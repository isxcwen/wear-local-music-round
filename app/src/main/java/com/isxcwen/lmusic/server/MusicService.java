package com.isxcwen.lmusic.server;

import static android.widget.Toast.LENGTH_SHORT;
import static com.isxcwen.lmusic.utils.ConstantsUtil.INDEX_NAME;
import static com.isxcwen.lmusic.utils.ConstantsUtil.MusicService.*;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.service.media.MediaBrowserService;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import com.isxcwen.lmusic.compone.AudioComponet;
import com.isxcwen.lmusic.compone.MediaPlayerComponet;
import com.isxcwen.lmusic.compone.TaskComponet;
import com.isxcwen.lmusic.compone.MediaSessionComponet;
import com.isxcwen.lmusic.compone.WearNotifyComponet;
import com.isxcwen.lmusic.model.CurrentPlayInfo;
import com.isxcwen.lmusic.player.BasePlayer;
import com.isxcwen.lmusic.utils.LogUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MusicService extends MediaBrowserServiceCompat {
    private AudioComponet audioComponet;
    private MediaSessionComponet mediaSessionComponet;
    private WearNotifyComponet wearNotifyComponet;
    private BasePlayer basePlayer;

    @Override
    public IBinder onBind(Intent intent) {
        //LogUtils.print("onBind" + intent.getPackage());
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //LogUtils.print("onUnbind" + intent.getPackage());
        return super.onUnbind(intent);
    }

    @Override
    public void notifyChildrenChanged(@NonNull String parentId) {
        super.notifyChildrenChanged(parentId);
        //LogUtils.print("notifyChildrenChanged" + parentId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //LogUtils.print("onCreate", this);
        //创建mediaSession
        LogUtils.showToast(this, "获取音乐中...", LENGTH_SHORT);
        mediaSessionComponet = new MediaSessionComponet(this, new MySessionCallback());
        MediaControllerCompat controller = mediaSessionComponet.getController();
        //设置session才会生效
        setSessionToken(mediaSessionComponet.getSessionToken());

        //初始化播放器
        basePlayer = new MediaPlayerComponet();
        basePlayer.setController(controller);

        //设置声音管理
        audioComponet = new AudioComponet(this, controller);

        wearNotifyComponet = new WearNotifyComponet(this, mediaSessionComponet.getSessionToken(), mediaSessionComponet.getCurrentPlayInfo());
        //初始化进度条task
        TaskComponet.lock("server");
        TaskComponet.registerScheduler(() -> {
            try {
                TaskComponet.lock("server");
                if(mediaSessionComponet != null && mediaSessionComponet.isPlaying()){
                    mediaSessionComponet.sendProcessBar(basePlayer.getPosition(mediaSessionComponet.getMediaMetadataCompat()));
                }
            } finally {
                TaskComponet.unLock("server");
            }
        }, 300L , 500L);

        //Log.d("MusicService", "create MusicService success");
    }

    @Override
    public void onDestroy() {
        //LogUtils.print("onDestroy", this);
        super.onDestroy();
        destory();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        destory();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(clientPackageName, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.detach();
        result.sendResult(mediaSessionComponet.getMediaItem());
    }

    private class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            super.onPlay();
            if (playMusic(player -> {
                //更新播放状态 进度置为0
                mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PLAYING);
                //LogUtils.print("MusicService 开启通知");
                wearNotifyComponet.showForegroundNotify(MusicService.this, mediaSessionComponet.getCurrentPlayInfo());
                audioComponet.registerBecomingNoisyReceiver(MusicService.this);
            })) {
                //成功
            }else{
                //失败
                //LogUtils.print("暂停失败", MusicService.this);
                LogUtils.showToast(MusicService.this, "播放失败，下一首", LENGTH_SHORT);
                mediaSessionComponet.getController().getTransportControls().skipToNext();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            MediaMetadataCompat mediaMetadataCompat = mediaSessionComponet.getMediaMetadataCompatByMediaId(mediaId);
            int index = extras.getInt(INDEX_NAME);
            if (playMusic(mediaMetadataCompat, player -> {
                //更新播放数据
                mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PLAYING, 0, index);
                //LogUtils.print("MusicService 开启通知");
                wearNotifyComponet.showForegroundNotify(MusicService.this, mediaSessionComponet.getCurrentPlayInfo());
                audioComponet.registerBecomingNoisyReceiver(MusicService.this);
            })) {
                //成功
            }else{
                //失败
                //LogUtils.print("暂停失败", MusicService.this);
                LogUtils.showToast(MusicService.this, "播放失败，下一首", LENGTH_SHORT);
                mediaSessionComponet.getController().getTransportControls().skipToNext();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if(pauseMusic()){
                //保存音乐进度
                int position = basePlayer.getPosition(mediaSessionComponet.getMediaMetadataCompat());
                mediaSessionComponet.trySaveLastPlayInfo(MusicService.this, position);
                //更新状态
                mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PAUSED, position);
                wearNotifyComponet.cancleForegroundNotify(MusicService.this);
                audioComponet.unRegisterBecomingNoisyReceiver(MusicService.this);
            }else {
                //LogUtils.print("暂停失败", MusicService.this);
            }

        }

        @Override
        public void onStop() {
            super.onStop();
            stopSelf();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            playChange(true);
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            playChange(false);
        }
    }

    private void playChange(boolean isNext) {
        int targetId = mediaSessionComponet.getTargetId(isNext);
        MediaMetadataCompat mediaMetadataCompat = mediaSessionComponet.getMediaMetadataCompatById(targetId);
        //是否需要播放
        if (mediaSessionComponet.isPlaying()) {
            if (playMusic(mediaMetadataCompat, player -> {
                //更新播放状态 进度置为0
                mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PLAYING, 0, targetId);
                mediaSessionComponet.trySaveLastPlayInfo(MusicService.this, 0);
                //LogUtils.print("MusicService 更新通知");
                wearNotifyComponet.showForegroundNotify(MusicService.this, mediaSessionComponet.getCurrentPlayInfo());
            })) {

            }
        } else {
            //更新播放状态 进度置为0
            mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PAUSED, 0, targetId);
            mediaSessionComponet.trySaveLastPlayInfo(MusicService.this, 0);
            //LogUtils.print("MusicService 更新通知");
            //wearNotifyComponet.showNotify(MusicService.this, mediaSessionComponet.getCurrentPlayInfo());
        }

    }

    private boolean playMusic(Consumer<BasePlayer> callback) {
        PlaybackStateCompat playbackStateCompat = mediaSessionComponet.getPlaybackStateCompat();
        MediaMetadataCompat mediaMetadataCompat = mediaSessionComponet.getMediaMetadataCompat();
        if(playbackStateCompat == null || mediaMetadataCompat==null){
            return false;
        }
        long poistion = playbackStateCompat.getPosition();
        return playMusic(mediaMetadataCompat, poistion == 0 ? null : poistion, callback);
    }

    private boolean playMusic(MediaMetadataCompat mediaMetadataCompat, Consumer<BasePlayer> callback) {
        return playMusic(mediaMetadataCompat, null, callback);
    }

    private boolean playMusic(MediaMetadataCompat mediaMetadataCompat, Long position, Consumer<BasePlayer> callback) {
        if (mediaMetadataCompat != null) {
            //检查设备 申请焦点;
            if (audioComponet.checkDeviceVolume(this) && audioComponet.requestAudioFocus()) {
                if (position == null ? basePlayer.play(mediaMetadataCompat, callback) : basePlayer.play(mediaMetadataCompat, position, callback)) {
                    //LogUtils.print("MusicService 开启服务");
                    //startService(new Intent(getApplication(), MusicService.class));
                    //LogUtils.print("MusicService 注册设备变更回调");
                    //audioComponet.registerAudioDeviceCallback();
                    //audioComponet.registerBecomingNoisyReceiver(this);
                    //mediaSessionComponet.setActive(true);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pauseMusic(){
        return basePlayer.pause(mediaSessionComponet.getCurrentPlayInfo().getMediaMetadataCompat());
    }

    private void destory(){
        //LogUtils.print("onDestroy", this);
        try {
            if(mediaSessionComponet != null){
                mediaSessionComponet.trySaveLastPlayInfo(this, basePlayer.getPosition(mediaSessionComponet.getMediaMetadataCompat()));
                mediaSessionComponet.destory(this);
                mediaSessionComponet = null;
            }

            if(basePlayer != null){
                basePlayer.destory();
                basePlayer = null;
            }

            //释放焦点
            if(audioComponet != null){
                audioComponet.destory(this);
                audioComponet = null;
            }

            if(wearNotifyComponet != null){
                wearNotifyComponet.destory(this);
                wearNotifyComponet = null;
            }

            TaskComponet.destory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
