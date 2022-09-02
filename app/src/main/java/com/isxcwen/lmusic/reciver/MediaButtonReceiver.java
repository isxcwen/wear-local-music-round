package com.isxcwen.lmusic.reciver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.media.MediaBrowserServiceCompat;

import com.isxcwen.lmusic.server.MusicService;

import java.util.List;

public class MediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())
                || !intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
            Log.d(TAG, "Ignore unsupported intent: " + intent);
            return;
        }
        context = context.getApplicationContext();
        context.startForegroundService(new Intent(context, MusicService.class));
        MediaButtonReceiver.MediaButtonConnectionCallback connectionCallback =
                new MediaButtonReceiver.MediaButtonConnectionCallback(context, intent);
        MediaBrowserCompat mediaBrowser = new MediaBrowserCompat(context,
                new ComponentName(context, MusicService.class), connectionCallback, null);
        connectionCallback.setMediaBrowser(mediaBrowser);
        mediaBrowser.connect();
        return;
    }

    private static class MediaButtonConnectionCallback extends
            MediaBrowserCompat.ConnectionCallback {
        private final Context mContext;
        private final Intent mIntent;

        private MediaBrowserCompat mMediaBrowser;

        MediaButtonConnectionCallback(Context context, Intent intent) {
            mContext = context;
            mIntent = intent;
        }

        void setMediaBrowser(MediaBrowserCompat mediaBrowser) {
            mMediaBrowser = mediaBrowser;
        }

        @Override
        public void onConnected() {
            try {
                MediaControllerCompat mediaController = new MediaControllerCompat(mContext,
                        mMediaBrowser.getSessionToken());
                KeyEvent ke = mIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                mediaController.dispatchMediaButtonEvent(ke);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to create a media controller", e);
            }
            finish();
        }

        @Override
        public void onConnectionSuspended() {
            finish();
        }

        @Override
        public void onConnectionFailed() {
            finish();
        }

        private void finish() {
            mMediaBrowser.disconnect();
        }
    };
}
