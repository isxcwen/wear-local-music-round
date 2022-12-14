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
        //??????mediaSession
        LogUtils.showToast(getApplication(), "???????????????...", LENGTH_SHORT);
        mediaSessionComponet = new MediaSessionComponet(getApplication(), new MySessionCallback());
        MediaControllerCompat controller = mediaSessionComponet.getController();
        //??????session????????????
        setSessionToken(mediaSessionComponet.getSessionToken());

        //??????????????????
        basePlayer = new MediaPlayerComponet();
        basePlayer.setController(controller);

        //??????????????????
        audioComponet = new AudioComponet(getApplication(), controller);

        wearNotifyComponet = new WearNotifyComponet(this, mediaSessionComponet.getSessionToken(), mediaSessionComponet.getCurrentPlayInfo());
        //??????????????????task
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
                //?????????????????? ????????????0
                mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PLAYING);
                //LogUtils.print("MusicService ????????????");
                wearNotifyComponet.showForegroundNotify(MusicService.this, mediaSessionComponet.getCurrentPlayInfo());
                audioComponet.registerBecomingNoisyReceiver(getApplication());
            })) {
                //??????
            }else{
                //??????
                //LogUtils.print("????????????", MusicService.this);
                LogUtils.showToast(getApplication(), "????????????????????????", LENGTH_SHORT);
                mediaSessionComponet.getController().getTransportControls().skipToNext();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            MediaMetadataCompat mediaMetadataCompat = mediaSessionComponet.getMediaMetadataCompatByMediaId(mediaId);
            int index = extras.getInt(INDEX_NAME);
            if (playMusic(mediaMetadataCompat, player -> {
                //??????????????????
                mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PLAYING, 0, index);
                //LogUtils.print("MusicService ????????????");
                wearNotifyComponet.showForegroundNotify(MusicService.this, mediaSessionComponet.getCurrentPlayInfo());
                audioComponet.registerBecomingNoisyReceiver(getApplication());
            })) {
                //??????
            }else{
                //??????
                //LogUtils.print("????????????", MusicService.this);
                LogUtils.showToast(getApplication(), "????????????????????????", LENGTH_SHORT);
                mediaSessionComponet.getController().getTransportControls().skipToNext();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if(pauseMusic()){
                //??????????????????
                int position = basePlayer.getPosition(mediaSessionComponet.getMediaMetadataCompat());
                mediaSessionComponet.trySaveLastPlayInfo(getApplication(), position);
                //????????????
                mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PAUSED, position);
                wearNotifyComponet.cancleForegroundNotify(MusicService.this);
                audioComponet.unRegisterBecomingNoisyReceiver(getApplication());
            }else {
                //LogUtils.print("????????????", MusicService.this);
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
        //??????????????????
        if (mediaSessionComponet.isPlaying()) {
            if (playMusic(mediaMetadataCompat, player -> {
                //?????????????????? ????????????0
                mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PLAYING, 0, targetId);
                mediaSessionComponet.trySaveLastPlayInfo(getApplication(), 0);
                //LogUtils.print("MusicService ????????????");
                wearNotifyComponet.showForegroundNotify(MusicService.this, mediaSessionComponet.getCurrentPlayInfo());
            })) {

            }
        } else {
            //?????????????????? ????????????0
            mediaSessionComponet.notifyPlayInfoChange(PlaybackStateCompat.STATE_PAUSED, 0, targetId);
            mediaSessionComponet.trySaveLastPlayInfo(getApplication(), 0);
            //LogUtils.print("MusicService ????????????");
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
            //???????????? ????????????;
            if (audioComponet.checkDeviceVolume(getApplication()) && audioComponet.requestAudioFocus()) {
                if (position == null ? basePlayer.play(mediaMetadataCompat, callback) : basePlayer.play(mediaMetadataCompat, position, callback)) {
                    //LogUtils.print("MusicService ????????????");
                    //startService(new Intent(getApplication(), MusicService.class));
                    //LogUtils.print("MusicService ????????????????????????");
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
                mediaSessionComponet.trySaveLastPlayInfo(getApplication(), basePlayer.getPosition(mediaSessionComponet.getMediaMetadataCompat()));
                mediaSessionComponet.destory(this);
                mediaSessionComponet = null;
            }

            if(basePlayer != null){
                basePlayer.destory();
                basePlayer = null;
            }

            //????????????
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
