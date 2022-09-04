package com.isxcwen.lmusic.activity;

import androidx.annotation.NonNull;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewConfigurationCompat;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.isxcwen.lmusic.R;
import com.isxcwen.lmusic.adpate.QueueAdpate;
import com.isxcwen.lmusic.compone.TaskComponet;
import com.isxcwen.lmusic.utils.ConstantsUtil;
import com.isxcwen.lmusic.utils.LogUtils;

import java.util.List;

public class MusicQueueActivity extends MediaBrowserActivity {
    private WearableRecyclerView wearableRecyclerView;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_queue);
        //圆形音乐列表
        wearableRecyclerView = findViewById(R.id.circle_menu);
        wearableRecyclerView.setOnGenericMotionListener(genericMotionListener());
        textView = findViewById(R.id.no_music);
        initWearableRecyclerView();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected MediaBrowserCompat.SubscriptionCallback generateMediaBrowserCompatSubscriptionCallback() {
        return new MediaBrowserCompat.SubscriptionCallback(){
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
                super.onChildrenLoaded(parentId, children);
                //LogUtils.print("获取音乐列表", MusicQueueActivity.this);
                //设置List适配器
                if(children == null || children.isEmpty()){
                    //textView.setVisibility(View.VISIBLE);
                }else {
                    wearableRecyclerView.setAdapter(new QueueAdpate(children, MusicQueueActivity.this, mediaControllerCompat));
                    wearableRecyclerView.setVisibility(View.VISIBLE);
                    wearableRecyclerView.requestFocus();
                }
            }
        };
    }

    private void initWearableRecyclerView(){
        //如果想要自适应手表表盘请使用WearableLinearLayoutManager，如果不需要适配表盘可以使用LinearLayoutManager
        wearableRecyclerView.setLayoutManager(new WearableLinearLayoutManager(getApplication()));
        //第一个列表项和最后一个列表项在屏幕上垂直居中对齐
        wearableRecyclerView.setEdgeItemsCenteringEnabled(true);
        //是否可以使用圆形滚动手势
        wearableRecyclerView.setCircularScrollingGestureEnabled(false);
        //靠近屏幕边缘的虚拟“屏幕边框”（在此区域内能够识别出手势）的宽度
        //wearableRecyclerView.setBezelFraction(1f);
        //用户的手指必须旋转多少度才能滚过一个屏幕高度
        wearableRecyclerView.setScrollDegreesPerScreen(45f);
    }

    private View.OnGenericMotionListener genericMotionListener(){
        return (v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_SCROLL &&
                    ev.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)
            ) {
                // Don't forget the negation here
                float delta = -ev.getAxisValue(MotionEventCompat.AXIS_SCROLL) *
                        ViewConfigurationCompat.getScaledVerticalScrollFactor(
                                ViewConfiguration.get(getApplication()), getApplication()
                        );

                // Swap these axes to scroll horizontally instead
                v.scrollBy(0, Math.round(delta));

                return true;
            }
            return false;
        };
    }

    @Override
    protected boolean needStartServer() {
        return false;
    }
}