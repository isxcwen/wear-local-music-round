package com.isxcwen.lmusic.model;

import android.content.pm.PackageManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class CurrentPlayInfo {
    private MediaMetadataCompat mediaMetadataCompat;
    private PlaybackStateCompat playbackStateCompat;


    public MediaMetadataCompat getMediaMetadataCompat() {
        return mediaMetadataCompat;
    }

    public void setMediaMetadataCompat(MediaMetadataCompat mediaMetadataCompat) {
        this.mediaMetadataCompat = mediaMetadataCompat;
    }

    public PlaybackStateCompat getPlaybackStateCompat() {
        return playbackStateCompat;
    }

    public void setPlaybackStateCompat(PlaybackStateCompat playbackStateCompat) {
        this.playbackStateCompat = playbackStateCompat;
    }


    public String getMediaId(){
        return mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
    }

    public String getMusicName(){
        return mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
    }

    public String getSingName(){
        return mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
    }

    public boolean isPlay(){
        if(playbackStateCompat == null){
            return false;
        }
        return playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING;
    }

    public long getDuration() {
        return mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
    }

    public int getProcess(long currentPos) {
        long aLong = mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        return (int) (currentPos * 100 / aLong);
    }

    public String getPath() {
        return mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
    }
}
