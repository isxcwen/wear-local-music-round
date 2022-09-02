package com.isxcwen.lmusic.compone;

import static android.media.AudioManager.AUDIOFOCUS_REQUEST_FAILED;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.isxcwen.lmusic.utils.LogUtils;

import java.util.Arrays;

public class AudioComponet {
    private AudioManager audioManager;
    //焦点状态 是否获取
    private boolean focusSattus = false;
    private AudioFocusRequest audioFocusRequest;

    //播放控制器
    private MediaControllerCompat mediaController;

    //切换到扬声器
    private BecomingNoisyReceiver becomingNoisyReceiver;
    private boolean registedBecoming = false;
    private IntentFilter becomingIntent;

    //设备改变回调
    //private boolean registedAd2pListener = false;
    //private boolean pauseBuAd2pRemove = false;
    //private AudioDeviceCallback audioDeviceCallback;


    //当前音量
    private int currentvolume;
    //最大音量
    private int maxVolume;
    //最小音量
    private int minVolume;
    //固定音量设备
    private boolean volumeFixed;

    public AudioComponet(Activity activity){
        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentvolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        minVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        volumeFixed = audioManager.isVolumeFixed();
    }

    public AudioComponet(Service service, MediaControllerCompat controllerCompat) {
        this.audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        this.mediaController = controllerCompat;
        audioFocusRequest = initAudioFocusRequest();
        //audioDeviceCallback = gengrateCallback();
    }

    public boolean updateVolume(Activity activity, float progress){
        int v = progressVolume(progress);
        if(volumeFixed) return false;
        if(activity != null){
            activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, AudioManager.FLAG_PLAY_SOUND);
        currentvolume = v;
        return true;
    }

    private boolean updateVolume(Activity activity, int volume){
        if(volumeFixed) return false;
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
        currentvolume = volume;
        return true;
    }

    public int currentProcess(){
        return volumeProgress(currentvolume);
    }

    private int volumeProgress(int volume){
        //声音转化为百分比
        return (volume * 100) / maxVolume;
    }

    private int progressVolume(float progress){
        //百分比转化为声音
        return (int)(progress * maxVolume) / 100;
    }

    public void destory(Service service){
        try {
            if(audioManager != null){
                abandonAudioFocusRequest();
                /*if(audioDeviceCallback != null){
                    audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
                }*/
                unRegisterBecomingNoisyReceiver(service);
            }
        }finally {
            //LogUtils.print("释放AudioCompone");
        }
    }

    public boolean checkDeviceVolume(Context context){
        //是否有输出设备形式
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            return false;
        }
        //检查扬声器,检查蓝牙耳机
        boolean SPEAKER = audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        //LogUtils.print("扬声器：" + SPEAKER);
        boolean BLUETOOTH = audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
        //LogUtils.print("蓝牙耳机：" + BLUETOOTH);
        if(!SPEAKER && !BLUETOOTH){
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("EXTRA_CONNECTION_ONLY", true)
                    .putExtra("EXTRA_CLOSE_ON_CONNECT", true)
                    .putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1);
            context.startActivity(intent);
            return false;
        }
        return true;
    }

    private boolean audioOutputAvailable(int type){
        //是否有对应设备
        return Arrays.stream(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)).anyMatch(audioDeviceInfo -> audioDeviceInfo.getType() == type);
    }

    /*public void registerAudioDeviceCallback(){
        //蓝牙耳机
        if(!registedAd2pListener && audioDeviceCallback != null){
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
            registedAd2pListener = true;
        }
    }

    public void unRegisterAudioDeviceCallback(){
        //蓝牙耳机
        if(registedAd2pListener && audioDeviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
            registedAd2pListener = false;
        }
    }

    private AudioDeviceCallback gengrateCallback(){
        return new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                boolean isAd2p = Arrays.stream(addedDevices).map(audioDeviceInfo -> audioDeviceInfo.getType()).anyMatch(type -> type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
                if (isAd2p) {
                    if (pauseBuAd2pRemove && mediaController.getPlaybackState().getState() != PlaybackStateCompat.STATE_PLAYING) {
                        //LogUtils.print("恢复移除耳机暂停播放");
                        mediaController.getTransportControls().play();
                        pauseBuAd2pRemove = false;
                    }
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                super.onAudioDevicesRemoved(removedDevices);
                boolean isAd2p = Arrays.stream(removedDevices).map(audioDeviceInfo -> audioDeviceInfo.getType()).anyMatch(type -> type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
                if (isAd2p) {
                    if(mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                        //LogUtils.print("蓝牙移除暂停");
                        mediaController.getTransportControls().pause();
                        pauseBuAd2pRemove = true;
                    }
                }
            }
        };
    }*/

    public boolean requestAudioFocus(){
        if(audioFocusRequest != null && !focusSattus){
            focusSattus = audioManager.requestAudioFocus(audioFocusRequest) !=
                    AUDIOFOCUS_REQUEST_FAILED;
            //LogUtils.print("申请声音焦点" + focusSattus);
        }
        return focusSattus;
    }

    public void abandonAudioFocusRequest(){
        if(audioFocusRequest != null && focusSattus) {
             if(audioManager.abandonAudioFocusRequest(audioFocusRequest) != AUDIOFOCUS_REQUEST_FAILED){
                 focusSattus = false;
             }
            //LogUtils.print("释放声音焦点" + focusSattus);
        }
    }

    private AudioFocusRequest initAudioFocusRequest() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            /**
             *AUDIOFOCUS_GAIN = 1;//想要长期占有焦点，失去焦点者stop播放和释放（原有声音焦点者失去声音焦点）
             *AUDIOFOCUS_GAIN_TRANSIENT = 2;//想要短暂占有焦点，失去焦点者pause播放（原有声音焦点者继续持有音焦点），比如语音、比如电话
             *AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;//想要短暂占有焦点，失去焦点者可以继续播放但是音量需要调低，比如导航
             *AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;//想要短暂占有焦点，但希望失去焦点者不要有声音播放，比如电话
             */
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    // 可让您的应用异步处理焦点请求。设置此标记后，
                    // 在焦点锁定时发出的请求会返回 AUDIOFOCUS_REQUEST_DELAYED。
                    // 当锁定音频焦点的情况不再存在时（例如当通话结束时），
                    // 系统会批准待处理的焦点请求，并调用 onAudioFocusChange() 来通知您的应用。
                    .setAcceptsDelayedFocusGain(true)
                    //播放通知铃声时自动降低音量，true则回调音频焦点更改回调，可在回调里暂停音乐
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(this::onAudioFocusChange).build();
            return audioFocusRequest;
        }
        return null;
    }

    private void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mediaController.getTransportControls().play();
                //Log.d("MusicService", "获得焦点");
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //Log.d("MusicService", "失去焦点");
                mediaController.getTransportControls().stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //Log.d("MusicService", "短暂失去焦点");
                mediaController.getTransportControls().pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                /*//Log.d("MusicService", "短暂失去焦点可以压低声音播放");
                mediaController.getTransportControls().pause();*/
                break;
            default:
        }
    }

    public void registerBecomingNoisyReceiver(Service service){
        if(!registedBecoming){
            registedBecoming = true;
            initBecomeReceiver();
            initBecomingIntent();
            service.registerReceiver(becomingNoisyReceiver, becomingIntent);
        }
    }

    public void unRegisterBecomingNoisyReceiver(Service service){
        if(registedBecoming){
            registedBecoming = false;
            initBecomeReceiver();
            service.unregisterReceiver(becomingNoisyReceiver);
        }
    }

    public void initBecomeReceiver(){
        if(becomingNoisyReceiver == null){
            becomingNoisyReceiver = new BecomingNoisyReceiver();
        }
    }

    public void initBecomingIntent(){
        if(becomingIntent == null){
            becomingIntent = new IntentFilter();
            becomingIntent.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        }
    }


    private class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mediaController.getTransportControls().pause();
            }
        }
    }
}
