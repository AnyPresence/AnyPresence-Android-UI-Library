package com.anypresence.library;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

/**
 * A class for loading an image from a URL into an ImageView.
 * */
public class BitmapTask extends AsyncTask<Void, Void, Bitmap> {
    private static final LruCache<String, Bitmap> LOADED_BITMAPS = new LruCache<String, Bitmap>(4 * 1024 * 1024);

    private final Context mContext;
    private final ImageView mImageView;
    private final String mURL;

    public BitmapTask(Context context, ImageView imageView, String url) {
        this.mContext = context.getApplicationContext();
        this.mImageView = imageView;
        this.mURL = url;
    }

    @Override
    protected Bitmap doInBackground(Void... args) {
        return getImageBitmap(mURL);
    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bitmap = null;

        if(url != null && !"".equals(url)) {
            try {
                URL aURL = new URL(url);
                URLConnection conn = aURL.openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                bitmap = BitmapFactory.decodeStream(bis);
                bis.close();
                is.close();

                cacheBitmap(mContext, bitmap, url);
            }
            catch(IOException e) {
                Log.e(AnyPresenceActivity.TAG, "Error getting bitmap from url " + url, e);
            }
        }

        return bitmap;
    }

    private File getCacheFile(Context context, String url) {
        if(url != null && !"".equals(url)) {
            try {
                return new File(context.getCacheDir() + File.separator + URLEncoder.encode(url, "UTF-8"));
            }
            catch(UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Save bitmap to cache dir for quicker loading
     * */
    private void cacheBitmap(Context context, Bitmap bitmap, String url) {
        if(url != null && !"".equals(url)) {

            Log.d(AnyPresenceActivity.TAG, "Saving bitmap to memory.");
            if(bitmap != null) persist(bitmap);

            File cache = getCacheFile(context, url);
            Log.d(AnyPresenceActivity.TAG, "Got cache file at " + cache);

            if(!cache.isDirectory()) {
                Log.d(AnyPresenceActivity.TAG, "Saving bitmap to disk now.");

                cache.delete();

                try {
                    FileOutputStream fos = new FileOutputStream(cache);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.close();
                    Log.d(AnyPresenceActivity.TAG, "Bitmap saved to disk.");
                }
                catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
                catch(NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Load bitmap from cache dir
     * */
    protected Bitmap loadCache() {
        Context context = mContext;
        String url = mURL;
        if(url != null && !"".equals(url)) {
            File cache = getCacheFile(context, url);

            if(!cache.isDirectory()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(cache.toString(), options);

                return bitmap;
            }
            else {
                return null;
            }
        }
        return null;
    }

    protected void persist(Bitmap bitmap) {
        LOADED_BITMAPS.put(mURL, bitmap);
    }

    private Bitmap loadMemCache(String url) {
        if(url != null && !"".equals(url)) {
            Log.d(AnyPresenceActivity.TAG, "Grabbing bitmap from memory with key: " + url);
            Bitmap bitmap = LOADED_BITMAPS.get(url);
            if(bitmap != null) {
                return bitmap;
            }
            else {
                Log.d(AnyPresenceActivity.TAG, "But there was no bitmap in memory");
            }
        }
        return null;
    }

    @SuppressLint("NewApi")
    public void executeAsync(Void... args) {
        Bitmap bitmap = loadMemCache(mURL);
        if(bitmap != null) {
            mImageView.setImageBitmap(bitmap);
        }
        else {
            if(android.os.Build.VERSION.SDK_INT < 11) {
                execute(args);
            }
            else {
                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
            }
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Bitmap bitmap = loadCache();
        if(bitmap != null) {
            mImageView.setImageBitmap(bitmap);

            Log.d(AnyPresenceActivity.TAG, "Saving bitmap to memory.");
            persist(bitmap);
        }
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);

        if(result != null) mImageView.setImageBitmap(result);
    }
}
