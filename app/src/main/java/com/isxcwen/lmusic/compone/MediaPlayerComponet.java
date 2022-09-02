package com.isxcwen.lmusic.compone;

import android.media.MediaPlayer;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;

import com.isxcwen.lmusic.player.BasePlayer;
import com.isxcwen.lmusic.utils.LogUtils;

import java.io.IOException;
import java.util.function.Consumer;

public class MediaPlayerComponet implements BasePlayer {
    //播放器
    private MediaPlayer mediaPlayer;
    private MediaControllerCompat controller;
    private MediaMetadataCompat mediaMetadataCompat;

    public MediaPlayerComponet(){
        //initMediaPlayer();
    }

    @Override
    public void setController(MediaControllerCompat controller) {
        this.controller = controller;
    }

    @Override
    public boolean play(MediaMetadataCompat mediaMetadataCompat, Consumer<BasePlayer> callback) {
        if(mediaPlayer == null){
            initMediaPlayer();
        }
        /*if(isOne(mediaMetadataCompat)){
            if(!mediaPlayer.isPlaying()){
                mediaPlayer.start();
            }
            return true;
        }*/
        return play(mediaMetadataCompat, new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                play(mediaPlayer);
                if(callback != null) {
                    callback.accept(MediaPlayerComponet.this);
                }
            }
        });
    }

    @Override
    public boolean play(MediaMetadataCompat mediaMetadataCompat, long position, Consumer<BasePlayer> callback) {
        return play(mediaMetadataCompat, new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                playSeek(mp, position);
                if(callback != null) {
                    callback.accept(MediaPlayerComponet.this);
                }
            }
        });
    }

    private boolean play(MediaMetadataCompat mediaMetadataCompat, MediaPlayer.OnPreparedListener listener){
        try {
            if(mediaPlayer == null){
                initMediaPlayer();
            }
            MediaDescriptionCompat description = mediaMetadataCompat.getDescription();
            //LogUtils.print("mediaPlayer 播放" + description.getTitle());
            //mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.setOnPreparedListener(listener);
            mediaPlayer.setDataSource("file://" + description.getDescription());
            this.mediaMetadataCompat = mediaMetadataCompat;
            mediaPlayer.prepareAsync();
            return true;
        } catch (IOException e) {
            //LogUtils.print("mediaPlayer 播放失败" + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean pause(MediaMetadataCompat mediaMetadataCompat) {
        if(mediaPlayer == null){
            return true;
        }
        //LogUtils.print("mediaPlayer 暂停");
        if(isOne(mediaMetadataCompat)){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                return true;
            }
        }
        //LogUtils.print("mediaPlayer 暂停失败");
        mediaPlayer.pause();
        return true;
    }

    @Override
    public int getPosition(MediaMetadataCompat mediaMetadataCompat) {
        if(mediaPlayer == null || !isOne(mediaMetadataCompat)){
            return -1;
        }
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void destory() {
        //LogUtils.print("mediaPlayer开始销毁");
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void play(MediaPlayer mp){
        playSeek(mp, 0);
    }

    private void playSeek(MediaPlayer mp, long position){
        if(position < 0){
            position = 0;
            //LogUtils.printFormat("mediaPlayer seek %s", position);
        }
        mp.seekTo((int) position);
        //LogUtils.print("mediaPlayer开始播放");
        mp.start();
    }

    private boolean isOne(MediaMetadataCompat mediaMetadataCompat){
        if(this.mediaMetadataCompat == null){
            return false;
        }
        return this.mediaMetadataCompat == mediaMetadataCompat;
    }

    private void initMediaPlayer() {
        //LogUtils.print("mediaPlayer初始化");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            //LogUtils.print("mediaPlayer播放完成，下一曲");
            controller.getTransportControls().skipToNext();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            //LogUtils.printFormat("mediaPlayer出错了，错误码 %s, extra %s", what, extra);
            return true;
        });
    }
}
