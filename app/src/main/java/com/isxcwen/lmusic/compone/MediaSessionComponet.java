package com.isxcwen.lmusic.compone;

import static android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
import static android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL;
import static com.isxcwen.lmusic.utils.ConstantsUtil.MusicService.PROCESS_KEY;
import static com.isxcwen.lmusic.utils.ConstantsUtil.MusicService.PROCESS_UPDATE;
import static com.isxcwen.lmusic.utils.ConstantsUtil.SessionConstants.SP_PLAY_INFO;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.isxcwen.lmusic.model.CurrentPlayInfo;
import com.isxcwen.lmusic.reciver.MediaButtonReceiver;
import com.isxcwen.lmusic.server.MusicService;
import com.isxcwen.lmusic.utils.ConstantsUtil;
import com.isxcwen.lmusic.utils.LogUtils;
import com.isxcwen.lmusic.utils.MusicUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class MediaSessionComponet {
    private MediaSessionCompat mediaSession;

    //当前播放或显示的音乐信息
    private CurrentPlayInfo currentPlayInfo;
    private PlaybackStateCompat.Builder playbackStateCompatBuild;

    //音乐列表
    private List<MediaSessionCompat.QueueItem> queueItemList;
    private List<MediaMetadataCompat> mediaMetadataCompatList;
    private Map<String, MediaMetadataCompat> mediaId2MetadataCompat;
    private List<MediaBrowserCompat.MediaItem> mediaItem;

    private Bundle bundle;

    public MediaSessionComponet(Service service, MediaSessionCompat.Callback callback){
        bundle = new Bundle();
        initSession(service, callback);
    }

    public void initSession(Service service, MediaSessionCompat.Callback callback){
        //创建mediaSession
        ComponentName componentName = new ComponentName(service, MusicService.class);
        mediaSession = new MediaSessionCompat(service, service.getPackageName(), componentName, PendingIntent.getBroadcast(service, 1, new Intent(service, MediaButtonReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT));
        //开启控制类型
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        playbackStateCompatBuild = new PlaybackStateCompat.Builder();
        //TaskComponet.register(() -> {
            initMusic(service);
        //});

        //设置回调 通过控制器调用回调对应的方法
        mediaSession.setCallback(callback);
    }

    private void initMusic(Context context) {
        //获取音乐列表
        //初始化本地列表
        //mediaMetadataCompatList  mediaId2MetadataCompat  queueItems
        List<MediaSessionCompat.QueueItem> queueItems = initMusicList(context.getContentResolver());
        //设置队列
        mediaSession.setQueue(queueItems);

        //获取最后一次播放信息
        currentPlayInfo = initCurrentPlayInfo(context.getSharedPreferences(SP_PLAY_INFO, Context.MODE_PRIVATE));
        //设置状态
        mediaSession.setPlaybackState(currentPlayInfo.getPlaybackStateCompat());
        //设置播放元数据
        mediaSession.setMetadata(currentPlayInfo.getMediaMetadataCompat());
        mediaSession.setActive(true);
    }

    private List<MediaSessionCompat.QueueItem> initMusicList(ContentResolver contentResolver){
        mediaMetadataCompatList = MusicUtils.readLocalMusicMediaItem(contentResolver);
        mediaId2MetadataCompat = mediaMetadataCompatList.stream().collect(Collectors.toMap(mediaMetadataCompat -> mediaMetadataCompat.getDescription().getMediaId(), Function.identity(), (o1, o2) -> o1));
        mediaItem = mediaMetadataCompatList.stream().map(mediaMetadataCompat -> new MediaBrowserCompat.MediaItem(mediaMetadataCompat.getDescription(), FLAG_PLAYABLE)).collect(Collectors.toList());
        queueItemList = IntStream.range(0, mediaMetadataCompatList.size()).parallel()
                .mapToObj(index -> mediaMetadata2QueueItem(mediaMetadataCompatList.get(index), index))
                .collect(Collectors.toList());
        return queueItemList;
    }

    public MediaMetadataCompat getMediaMetadataCompatByMediaId(String mediaId){
        return mediaId2MetadataCompat.get(mediaId);
    }

    public MediaMetadataCompat getMediaMetadataCompatById(int id){
        if(id < 0 || id >= mediaMetadataCompatList.size()){
            return null;
        }
        return mediaMetadataCompatList.get(id);
    }

    private CurrentPlayInfo initCurrentPlayInfo(SharedPreferences sharedPreferences){
        long position = sharedPreferences.getLong("position", 0);
        float speed = sharedPreferences.getFloat("playbackSpeed", 1.0F);
        int shuffleMode = sharedPreferences.getInt("shuffMode", SHUFFLE_MODE_ALL);
        int index = sharedPreferences.getInt("index", 0);
        String mediaId = sharedPreferences.getString("mediaId", "");
        bundle.putInt("shuffMode", shuffleMode);
        playbackStateCompatBuild
                .setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_FAST_FORWARD)
                .setExtras(bundle);
        MediaMetadataCompat mediaMetadataCompat = null;
        //判断有没有音乐列表 判断索引有没有超出边界  判断是不是同一首
        if ((mediaMetadataCompatList != null && mediaMetadataCompatList.size() != 0)){
            if(index < mediaMetadataCompatList.size() && mediaId.equals(mediaMetadataCompatList.get(index).getDescription().getMediaId())) {
                mediaMetadataCompat = mediaMetadataCompatList.get(index);
                bundle.putLong("duration", mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
                //LogUtils.print("获取到上次保存的音乐信息");
            }else {
                mediaMetadataCompat = mediaMetadataCompatList.get(0);
                bundle.putLong("duration", mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
                //LogUtils.print("索引不存在置为0");
                index = 0;
                position = 0;
                //LogUtils.print("音乐信息本地不存在，可能被删除, 将数据置为空");
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.commit();
            }
        } else {
            index = 0;
            position = 0;
            //LogUtils.print("音乐信息本地不存在，可能被删除, 将数据置为空");
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.clear();
            edit.commit();
        }
        playbackStateCompatBuild.setState(PlaybackStateCompat.STATE_NONE, position, speed);
        playbackStateCompatBuild.setActiveQueueItemId(index);
        CurrentPlayInfo currentPlayInfo = new CurrentPlayInfo();
        currentPlayInfo.setPlaybackStateCompat(playbackStateCompatBuild.build());
        currentPlayInfo.setMediaMetadataCompat(mediaMetadataCompat);
        return currentPlayInfo;
    }

    public void trySaveLastPlayInfo(Context context, long position){
        if(position == -1 || currentPlayInfo == null){
            return;
        }
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SP_PLAY_INFO, Context.MODE_PRIVATE);
            //LogUtils.print("存储播放信息");
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString("mediaId", currentPlayInfo.getMediaMetadataCompat().getDescription().getMediaId());
            try {
                edit.putLong("position", position);
            } catch (Exception e) {
            }
            edit.putInt("index", (int)currentPlayInfo.getPlaybackStateCompat().getActiveQueueItemId());
            edit.putInt("shuffMode", SHUFFLE_MODE_ALL);
            edit.putFloat("playbackSpeed", (int)currentPlayInfo.getPlaybackStateCompat().getPlaybackSpeed());
            edit.commit();
        } catch (Exception e) {
            //LogUtils.print("最后一次播放保存失败");
        }
    }

    private MediaSessionCompat.QueueItem mediaMetadata2QueueItem(MediaMetadataCompat mediaMetadataCompat, int index){
        return new MediaSessionCompat.QueueItem(mediaMetadataCompat.getDescription(), index);
    }

    public MediaSessionCompat.Token getSessionToken() {
        return mediaSession.getSessionToken();
    }

    public MediaControllerCompat getController() {
        return mediaSession.getController();
    }

    public List<MediaBrowserCompat.MediaItem> getMediaItem() {
        return this.mediaItem;
    }

    public boolean isPlaying(){
        if(currentPlayInfo == null){
            return false;
        }
        return currentPlayInfo.getPlaybackStateCompat().getState() == PlaybackStateCompat.STATE_PLAYING;
    }

    public CurrentPlayInfo getCurrentPlayInfo() {
        return currentPlayInfo;
    }

    public MediaMetadataCompat getMediaMetadataCompat() {
        if(currentPlayInfo != null){
            return currentPlayInfo.getMediaMetadataCompat();
        }else {
            return null;
        }
    }

    public PlaybackStateCompat getPlaybackStateCompat() {
        if(currentPlayInfo != null) {
            return currentPlayInfo.getPlaybackStateCompat();
        }else {
            return null;
        }
    }

    public int getTargetId(boolean isNext) {
        int size = mediaMetadataCompatList.size();
        if(size == 0){
            return -1;
        }
        long activeQueueItemId = currentPlayInfo.getPlaybackStateCompat().getActiveQueueItemId();
        int targetIndex;
        if(isNext){
            if(activeQueueItemId >= size - 1){
                targetIndex = 0;
            }else {
                targetIndex = (int)activeQueueItemId + 1;
            }
        }else {
            if(activeQueueItemId - 1 < 0){
                targetIndex = size - 1;
            }else {
                targetIndex = (int)activeQueueItemId - 1;
            }
        }
        return targetIndex;
    }

    public void notifyPlayInfoChange(int status){
        PlaybackStateCompat playbackStateCompat = currentPlayInfo.getPlaybackStateCompat();
        notifyPlayInfoChange(status, (int)playbackStateCompat.getPosition(), (int)playbackStateCompat.getActiveQueueItemId());
    }

    public void notifyPlayInfoChange(int status, int position){
        PlaybackStateCompat playbackStateCompat = currentPlayInfo.getPlaybackStateCompat();
        notifyPlayInfoChange(status, position, (int)playbackStateCompat.getActiveQueueItemId());
    }
    public void notifyPlayInfoChange(int status, int position, int index){
        PlaybackStateCompat playbackStateCompat = currentPlayInfo.getPlaybackStateCompat();
        if(index == (int)playbackStateCompat.getActiveQueueItemId()){
            //歌曲没变
        }else {
            //歌曲改变
            MediaMetadataCompat mediaMetadataCompat = mediaMetadataCompatList.get(index);
            currentPlayInfo.setMediaMetadataCompat(mediaMetadataCompat);
            playbackStateCompatBuild.setActiveQueueItemId(index);
        }
        playbackStateCompat = playbackStateCompatBuild.setState(status, position, playbackStateCompat.getPlaybackSpeed()).build();
        currentPlayInfo.setPlaybackStateCompat(playbackStateCompat);
        playbackStateCompat.getExtras().putLong("duration", currentPlayInfo.getMediaMetadataCompat().getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        //LogUtils.print("状态：" + status);
        //LogUtils.print("歌名：" + currentPlayInfo.getMusicName());
        mediaSession.setPlaybackState(currentPlayInfo.getPlaybackStateCompat());
        mediaSession.setMetadata(currentPlayInfo.getMediaMetadataCompat());
    }

    public void sendProcessBar(int position){
        long duration = currentPlayInfo.getDuration();
        bundle.putInt(PROCESS_KEY, MusicUtils.calculateProcess(position, duration));
        mediaSession.sendSessionEvent(PROCESS_UPDATE, bundle);
    }

    public void destory(Service service) {
        try {
            //mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setActive(boolean active) {
        mediaSession.setActive(active);
    }
}
