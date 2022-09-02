package com.isxcwen.lmusic.activity;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.isxcwen.lmusic.compone.AudioComponet;
import com.isxcwen.lmusic.compone.TaskComponet;
import com.isxcwen.lmusic.databinding.ActivityVolumeBinding;
import com.isxcwen.lmusic.view.VerticalSeekBar;

public class VolumeActivity extends Activity {
    private ActivityVolumeBinding binding;
    private AudioComponet audioComponet;
    private volatile boolean autoClose = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVolumeBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        audioComponet = new AudioComponet(this);
        int process = audioComponet.currentProcess();
        binding.volumeShow.setText(String.valueOf(process));
        binding.volumeSeekBar.setProgress(process);

        TaskComponet.registerDelay(this::closeActivity, 2000L);

        binding.volumeSeekBar.setOnProgressUpdateListener(gengrateListener());
    }

    private VerticalSeekBar.OnProgressUpdateListener gengrateListener(){
        return new VerticalSeekBar.OnProgressUpdateListener() {
            @Override
            public void onStartTrackingTouch(VerticalSeekBar seekBar) {
                autoClose = false;
            }

            @Override
            public void onProgressUpdate(VerticalSeekBar seekBar, float progress, boolean fromUser) {
                int p = (int) progress;
                binding.volumeShow.setText(String.valueOf(p));
                audioComponet.updateVolume(VolumeActivity.this, p);
            }

            @Override
            public void onStopTrackingTouch(VerticalSeekBar seekBar) {
                autoClose = true;
                TaskComponet.registerDelay(VolumeActivity.this::closeActivity, 1000L);
            }
        };
    }

    private void closeActivity(){
        if(autoClose){
            try {
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}