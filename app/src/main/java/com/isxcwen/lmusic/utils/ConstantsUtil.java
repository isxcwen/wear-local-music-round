package com.isxcwen.lmusic.utils;

import android.Manifest;

public class ConstantsUtil {
    public static class NotifyConstants{
        public static final String CHANNEL_ID = "com.isxcwen.lmusic-channel";
        public static final String PLAY_PERFIX = "正在播放";
        public static final String PAUSE_PERFIX = "暂停中";
        public static final String CHANNEL_NAME = "本地音乐";
        public static final int NOTIFY_ID = 1;
    }

    public static class MusicService{
        public static final String PROCESS_UPDATE = "PROCESS_UPDATE";
        public static final String PROCESS_KEY = "PROCESS_KEY";
    }

    public static final String SPLIT_SING_POS = "=duration=";
    public static final String INDEX_NAME = "index";
    public static final String PLAY_PROCESS = "process";

    public static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.DISABLE_KEYGUARD
    };

    public static final int AUTO_FINISH_TIME = 10 * 1000;

    public static class SessionConstants{
        public static final String SP_PLAY_INFO = "Play_INFO";
    }
}
