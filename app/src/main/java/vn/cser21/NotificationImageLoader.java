package vn.cser21;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.bumptech.glide.Glide;

import java.util.concurrent.ExecutionException;

public final class NotificationImageLoader {

    private NotificationImageLoader() {
    }

    public static Bitmap loadLargeIcon(Context context, String url) {
        int sizePx = dpToPx(context, 64);
        return loadBitmap(context, url, sizePx, sizePx);
    }

    public static Bitmap loadBigPicture(Context context, String url) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int widthPx = Math.max(displayMetrics.widthPixels, dpToPx(context, 240));
        int heightPx = dpToPx(context, 180);
        return loadBitmap(context, url, widthPx, heightPx);
    }

    private static Bitmap loadBitmap(Context context, String url, int widthPx, int heightPx) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        try {
            return Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(url)
                    .submit(widthPx, heightPx)
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
