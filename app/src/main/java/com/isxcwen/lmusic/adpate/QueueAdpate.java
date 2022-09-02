package com.isxcwen.lmusic.adpate;

import static com.isxcwen.lmusic.utils.ConstantsUtil.INDEX_NAME;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.isxcwen.lmusic.R;

import java.util.List;

public class QueueAdpate extends RecyclerView.Adapter<QueueAdpate.QueueViewHolder> {
    private final List<MediaBrowserCompat.MediaItem> mediaItems;
    private Activity activity;
    private MediaControllerCompat controllerCompat;

    public QueueAdpate(List<MediaBrowserCompat.MediaItem> mediaItems, Activity activity, MediaControllerCompat controllerCompat) {
        this.mediaItems = mediaItems;
        this.activity = activity;
        this.controllerCompat = controllerCompat;
    }

    @NonNull
    @Override
    public QueueAdpate.QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_list_item, parent, false);
        return new QueueViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueAdpate.QueueViewHolder holder, int position) {
        holder.bind(mediaItems.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mediaItems == null ? 0 : mediaItems.size();
    }


    protected class QueueViewHolder extends RecyclerView.ViewHolder {
        private View view;
        public QueueViewHolder(@NonNull View view) {
            super(view);
            this.view = view;
        }

        public void bind(MediaBrowserCompat.MediaItem mediaItem, int position) {
            MediaDescriptionCompat description = mediaItem.getDescription();

            TextView musicName = view.findViewById(R.id.music_name);
            musicName.setText(description.getTitle());

            TextView singName = view.findViewById(R.id.sing_name);
            singName.setText(description.getSubtitle());

            this.view.setOnClickListener(gengrateClickListener(description.getMediaId(), position));
        }
    }

    protected View.OnClickListener gengrateClickListener(String mediaId, int index){
        return view -> {
            Bundle bundle = new Bundle();
            bundle.putInt(INDEX_NAME, index);
            //view.onFinishTemporaryDetach();
            controllerCompat.getTransportControls().playFromMediaId(mediaId, bundle);
            activity.finish();
        };
    }
}
