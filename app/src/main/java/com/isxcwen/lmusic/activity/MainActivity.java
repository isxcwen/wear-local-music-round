package com.isxcwen.lmusic.activity;


import static com.isxcwen.lmusic.utils.ConstantsUtil.MusicService.PROCESS_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.isxcwen.lmusic.R;
import com.isxcwen.lmusic.compone.TaskComponet;
import com.isxcwen.lmusic.databinding.ActivityMainBinding;
import com.isxcwen.lmusic.model.ShowPlayInfo;
import com.isxcwen.lmusic.utils.ConstantsUtil;
import com.isxcwen.lmusic.utils.LogUtils;

public class MainActivity extends MediaBrowserActivity {
    private ShowPlayInfo showPlayInfo;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        showPlayInfo = new ShowPlayInfo();
        binding.setShowPlayInfo(showPlayInfo);
        musicNameRolling();
        setOnClickListener();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(showPlayInfo.isPlay()){
            TaskComponet.unLock("server");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        TaskComponet.lock("server");
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaBorwserServerIntent != null && !showPlayInfo.isPlay()){
            stopService(mediaBorwserServerIntent);
        }
    }

    @Override
    protected void connectSuccess() {
        super.connectSuccess();
        //LogUtils.print("链接MediaBrowserService成功", this);
        showPlayInfo.updateStatus(mediaControllerCompat.getPlaybackState());
        showPlayInfo.updateMetaData(mediaControllerCompat.getMetadata());
    }

    @Override
    protected MediaControllerCompat.Callback generateMediaControllerCallback() {
        return new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                super.onPlaybackStateChanged(state);
                if(showPlayInfo != null){
                    showPlayInfo.updateStatus(state);
                }
            }

            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                super.onMetadataChanged(metadata);
                if(showPlayInfo != null){
                    showPlayInfo.updateMetaData(metadata);
                }
            }

            @Override
            public void onSessionEvent(String event, Bundle extras) {
                super.onSessionEvent(event, extras);
                if(showPlayInfo.isPlay()
                        && ConstantsUtil.MusicService.PROCESS_UPDATE.equals(event)
                        && extras != null){
                    int process = extras.getInt(PROCESS_KEY, 0);
                    if(process > 0){
                        showPlayInfo.setProcess(process);
                    }
                }
            }
        };
    }

    @Override
    protected boolean needStartServer() {
        return true;
    }

    private void setOnClickListener(){
        //播放
        binding.play.setOnClickListener(v -> {
            //设置隐藏 暂停可见
            if(showPlayInfo.isPlay()){
                TaskComponet.lock("server");
                mediaControllerCompat.getTransportControls().pause();
            }else {
                TaskComponet.unLock("server");
                mediaControllerCompat.getTransportControls().play();
            }
        });
        //播放
        /*binding.pause.setOnClickListener(v -> {
            //设置隐藏 播放可见
            mediaControllerCompat.getTransportControls().pause();
        });*/
        //上一曲
        binding.previous.setOnClickListener(v -> {
            mediaControllerCompat.getTransportControls().skipToPrevious();
        });
        //下一曲
        binding.next.setOnClickListener(v -> {
            //设置隐藏 播放可见
            mediaControllerCompat.getTransportControls().skipToNext();
        });
        //音量
        binding.volume.setOnClickListener(v -> {
            //跳转音量
            startActivity(new Intent(getApplication(), VolumeActivity.class));
        });
        //音乐列表
        binding.queue.setOnClickListener(v -> {
            //跳转到列表
            startActivity(new Intent(getApplication(), MusicQueueActivity.class));
        });
    }

    private void musicNameRolling(){
        TextView textView = binding.currentMusicName;
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setSingleLine(true);
        textView.setSelected(true);
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
    }

    @Override
    protected boolean needAutoFinish() {
        return !showPlayInfo.isPlay();
    }
}