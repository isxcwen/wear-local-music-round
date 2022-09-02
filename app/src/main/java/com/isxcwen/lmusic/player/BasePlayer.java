package com.isxcwen.lmusic.player;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;

import java.util.function.Consumer;

public interface BasePlayer {
    void setController(MediaControllerCompat controller);

    boolean play(MediaMetadataCompat mediaMetadataCompat, Consumer<BasePlayer> callback);

    boolean play(MediaMetadataCompat mediaMetadataCompat, long position, Consumer<BasePlayer> callback);

    boolean pause(MediaMetadataCompat mediaMetadataCompat);

    int getPosition(MediaMetadataCompat mediaMetadataCompat);

    void destory();
}
