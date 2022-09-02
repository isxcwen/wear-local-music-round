package com.isxcwen.lmusic.utils;

import static com.isxcwen.lmusic.utils.ConstantsUtil.SPLIT_SING_POS;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MusicUtils {
    public static void readLocalMusicFile(String path, FileFilter fileFilter){
        List<File> scan = scan(new File(path), fileFilter, null);
    }

    private static List<File> scan(File file, FileFilter fileFilter, List<File> fileList){
        if(fileList == null){
            fileList = new LinkedList<>();
        }
        if(file.exists() &&  file.isDirectory()){
            File[] files = file.listFiles();
            if(files != null){
                for (File fileSub : files) {
                    scan(fileSub, fileFilter, fileList);
                }
            }
        }else {
            if(fileFilter.accept(file)){
                fileList.add(file);
            }
        }
        return fileList;
    }

    public static List<MediaMetadataCompat> readLocalMusicMediaItem(ContentResolver resolver) {
        //LogUtils.print("获取音乐列表");
        if (resolver == null) return null;
        Cursor cursor = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null);
            }
            if(cursor != null) {
                List<MediaMetadataCompat> mediaItems = new ArrayList<>(cursor.getCount());
                MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                while (cursor.moveToNext()){
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    //小于90s的不扫描
                    if (duration < 90000){
                        continue;
                    }
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    if(!isExists(path)){
                        continue;
                    }
                    builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album);
                    //builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, path);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, path);
                    builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
                    //西班牙语重音字母替换
                    /*title = getSpanishStr(title);
                    artist = getSpanishStr(artist).replaceAll("&", "/");
                    mediaItems.add(gengrateMediaItem(mediaId, title, artist, path, duration));*/
                    ////Log.d(TAG, "getLocalMusicMetadata: "+artist);
                    //LogUtils.print(title + " - " + artist);
                    mediaItems.add(builder.build());
                }
                return mediaItems;
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return new ArrayList<>();
    }

    private static MediaBrowserCompat.MediaItem gengrateMediaItem(String mediaId, CharSequence title, CharSequence artist, CharSequence path, long duration){
        MediaDescriptionCompat.Builder build = new MediaDescriptionCompat.Builder();
        build.setMediaId(mediaId)
                .setTitle(title)
                .setSubtitle(artist + SPLIT_SING_POS + duration)
                .setDescription(path);
        return new MediaBrowserCompat.MediaItem(build.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    public static MediaMetadataCompat mediaItem2MediaMetadata(MediaBrowserCompat.MediaItem mediaItem){
        MediaDescriptionCompat description = mediaItem.getDescription();
        String subtitle = (String) description.getSubtitle();
        String[] split = subtitle.split(SPLIT_SING_POS);
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaItem.getMediaId())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, (String)description.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, split[0])
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.valueOf(split[1]))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, (String) description.getDescription())
                .build();
    }

    private Bitmap getAlbumBitmap(String Path){
        if (Path.isEmpty()) return null;//返回默认的专辑封面
        if (!FileExists(Path)) return null; //找不到文件返回空
        if (!Path.contains(".mp3")) {
            Bitmap bitmap;
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(Path));
                bitmap = BitmapFactory.decodeStream(bis);
            }  catch (IOException e) {
                e.printStackTrace();
                //Log.d("AllSongSheetModel", "getAlbumBitmap: 本地图片转Bitmap失败");
                return null;
            }finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //Log.d("AllSongSheetModel", "getAlbumBitmap: 输出流关闭异常");
                    }
                }
            }
            ////Log.d("加载本地图片", "getAlbumBitmap: ");
            return bitmap;
        }else {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(Path);
            byte[] picture = metadataRetriever.getEmbeddedPicture();

            /*每次拿到专辑图片后，关闭MediaMetadataRetriever对象，等待GC器回收内存
             *以便下一次再重新引用（new），避免内存泄漏*/
            metadataRetriever.release();//SDK > 26 才有close，且close与release是一样的
            //返回默认的专辑封面
            return picture == null ? null :
                    BitmapFactory.decodeByteArray(picture, 0, picture.length);
        }
    }

    public static boolean FileExists(String targetFileAbsPath){
        try {
            File f = new File(targetFileAbsPath);
            if(!f.exists()) return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 将西班牙语重音乱码替换为UTF-8
     * */
    private static String getSpanishStr(String str) {
        if (str == null || TextUtils.isEmpty(str)) return str;
        return str.replaceAll("¨¢","á")
                .replaceAll("¨¦","é")
                .replaceAll("¨Ş","í")
                .replaceAll("¨ª","í")
                .replaceAll("¨®","ó")
                .replaceAll("¨²","ú");
    }

    public static int calculateProcess(long position, long duration){
        return (int) (position * 100 / duration);
    }

    public static boolean isExists(String path) {
        File file = new File(path);
        return file.exists();
    }
}


