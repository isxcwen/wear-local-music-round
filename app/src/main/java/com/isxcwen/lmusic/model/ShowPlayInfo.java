package com.isxcwen.lmusic.model;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.isxcwen.lmusic.BR;
import com.isxcwen.lmusic.utils.LogUtils;

public class ShowPlayInfo extends BaseObservable {
    private String musicName;
    private String singName;

    private boolean playBtn = true;
    private boolean stopBtn;
    private boolean pauseBtn = false;
    private boolean nextBtn = true;
    private boolean previousBtn = true;

    private boolean queueBtn = true;
    private boolean volumeBtn = true;

    private int position;
    private int duration;
    private int process = 0;

    private boolean isPlay = false;

    public void updateStatus(PlaybackStateCompat playbackStateCompat){
        if(playbackStateCompat == null){
            isPlay = false;
            setPlayBtn(true);
            setNextBtn(true);
            setPreviousBtn(true);
            setPosition(0);
        }else {
            long actions = playbackStateCompat.getActions();
            int state = playbackStateCompat.getState();
            isPlay = state == PlaybackStateCompat.STATE_PLAYING;
            if(!isPlay && isPermission(PlaybackStateCompat.ACTION_PLAY, actions)){
                setPlayBtn(true);
            }else {
                setPlayBtn(false);
            }

            if(isPlay && isPermission(PlaybackStateCompat.ACTION_PAUSE, actions)){
                setPauseBtn(true);
            }else {
                setPauseBtn(false);
            }

           /* if(isPermission(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS, actions)){
                setPreviousBtn(true);
            } else {
                setPreviousBtn(false);
            }

            if(isPermission(PlaybackStateCompat.ACTION_SKIP_TO_NEXT, actions)){
                setNextBtn(true);
            }else {
                setNextBtn(false);
            }


            if(isPermission(PlaybackStateCompat.ACTION_STOP, actions)){
                setStopBtn(true);
            }else {
                setStopBtn(false);
            }*/

            int position = (int)playbackStateCompat.getPosition();
            setPosition(position);
            int duration = (int)playbackStateCompat.getExtras().getLong("duration");
            setDuration(duration);
            setProcess(position == 0 ? 0 : position * 100 / duration);
        }
    }

    private boolean isPermission(long action, long actions){
        return (actions & action) == action;
    }

    public void updateMetaData(MediaMetadataCompat mediaMetadataCompat){
        String musicName, singName;
        if(mediaMetadataCompat == null){
            musicName = "未获取到本地音乐";
            singName = "";
        }else {
            musicName = mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            singName = mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        }
        //LogUtils.print("当前音乐：" + musicName);
        this.setMusicName(musicName);
        this.setSingName(singName);
    }
    @Bindable
    public int getProcess() {
        return process;
    }

    public void setProcess(int process) {
        if (process != this.process){
            this.process = process;
            notifyPropertyChanged(BR.process);
        }
    }
    @Bindable
    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        if (play != this.isPlay) {
            this.isPlay = play;
            notifyPropertyChanged(BR.play);
        }
    }

    @Bindable
    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        if(!musicName.equals(this.musicName)){
            this.musicName = musicName;
            notifyPropertyChanged(BR.musicName);
        }
    }
    @Bindable
    public String getSingName() {
        return singName;
    }

    public void setSingName(String singName) {
        if(!singName.equals(this.singName)) {
            this.singName = singName;
            notifyPropertyChanged(BR.singName);
        }
    }

    @Bindable
    public boolean isPlayBtn() {
        return playBtn;
    }

    public void setPlayBtn(boolean playBtn) {
        if(playBtn != this.playBtn) {
            this.playBtn = playBtn;
            notifyPropertyChanged(BR.playBtn);
        }
    }

    @Bindable
    public boolean isStopBtn() {
        return stopBtn;
    }

    public void setStopBtn(boolean stopBtn) {
        if(stopBtn != this.stopBtn) {
            this.stopBtn = stopBtn;
            notifyPropertyChanged(BR.stopBtn);
        }
    }

    @Bindable
    public boolean isPauseBtn() {
        return pauseBtn;
    }

    public void setPauseBtn(boolean pauseBtn) {
        if(pauseBtn != this.pauseBtn) {
            this.pauseBtn = pauseBtn;
            notifyPropertyChanged(BR.pauseBtn);
        }
    }

    @Bindable
    public boolean isNextBtn() {
        return nextBtn;
    }

    public void setNextBtn(boolean nextBtn) {
        if(nextBtn != this.nextBtn) {
            this.nextBtn = nextBtn;
            notifyPropertyChanged(BR.nextBtn);
        }
    }

    @Bindable
    public boolean isPreviousBtn() {
        return previousBtn;
    }

    public void setPreviousBtn(boolean previousBtn) {
        if(previousBtn != this.previousBtn) {
            this.previousBtn = previousBtn;
            notifyPropertyChanged(BR.previousBtn);
        }
    }

    @Bindable
    public boolean isQueueBtn() {
        return queueBtn;
    }

    public void setQueueBtn(boolean queueBtn) {
        if(queueBtn != this.queueBtn) {
            this.queueBtn = queueBtn;
            notifyPropertyChanged(BR.queueBtn);
        }
    }

    @Bindable
    public boolean isVolumeBtn() {
        return volumeBtn;
    }

    public void setVolumeBtn(boolean volumeBtn) {
        if(volumeBtn != this.volumeBtn) {
            this.volumeBtn = volumeBtn;
            notifyPropertyChanged(BR.volumeBtn);
        }
    }

    @Bindable
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if(position != this.position) {
            this.position = position;
            notifyPropertyChanged(BR.position);
        }
    }

    @Bindable
    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        if(duration != this.duration) {
            this.duration = duration;
            notifyPropertyChanged(BR.duration);
        }
    }
}
