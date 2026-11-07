package com.zola.app;

import android.content.Context;
import java.io.File;

final class MediaCache {
    private MediaCache() {}

    static synchronized long size(Context context) {
        File dir = new File(context.getApplicationContext().getFilesDir(), "media");
        File[] files = dir.listFiles();
        long total = 0;
        if (files != null) {
            for (File file : files) {
                total += file.length();
            }
        }
        return total;
    }

    static synchronized void clear(Context context) {
        File dir = new File(context.getApplicationContext().getFilesDir(), "media");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }
}
