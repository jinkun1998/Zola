package com.zola.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
final class Storage {
    static final String PICTURES = "pictures";
    static final String RECORDS = "records";
    static final String ROOT = "Zola";
    static final String VIDEOS = "videos";

    private Storage() {
    }

    static boolean canWriteRoot(Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return Build.VERSION.SDK_INT != 29 && context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == 0;
    }

    static String base(Context context) {
        return canWriteRoot(context) ? ROOT : Environment.DIRECTORY_DOWNLOADS + "/Zola";
    }

    static String kindOf(String str, String str2) {
        String lowerCase = "";
        String lowerCase2 = str == null ? "" : str.toLowerCase(Locale.ROOT);
        if (lowerCase2.isEmpty() || lowerCase2.equals("application/octet-stream")) {
            if (str2 != null) {
                lowerCase = str2.toLowerCase(Locale.ROOT);
            }
            String strGuessContentTypeFromName = URLConnection.guessContentTypeFromName(lowerCase);
            if (strGuessContentTypeFromName != null) {
                lowerCase2 = strGuessContentTypeFromName;
            }
        }
        if (lowerCase2.startsWith("image/")) {
            return PICTURES;
        }
        if (lowerCase2.startsWith("video/")) {
            return VIDEOS;
        }
        return null;
    }

    static String save(Context context, String str, String str2, String str3, InputStream inputStream) throws IOException {
        if (str3 == null || str3.isEmpty()) {
            str3 = "application/octet-stream";
        }
        if (canWriteRoot(context)) {
            File file = new File(new File(Environment.getExternalStorageDirectory(), ROOT), str);
            if (!file.exists() && !file.mkdirs()) {
                throw new IOException("mkdirs " + file);
            }
            File fileUnique = Downloads.unique(file, str2);
            copy(inputStream, new FileOutputStream(fileUnique));
            MediaScannerConnection.scanFile(context, new String[]{fileUnique.getAbsolutePath()}, new String[]{str3}, null);
            return "Zola/" + str + "/" + fileUnique.getName();
        }
        if (Build.VERSION.SDK_INT >= 29) {
            String str4 = Environment.DIRECTORY_DOWNLOADS + "/Zola/" + str;
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", str2);
            contentValues.put("mime_type", str3);
            contentValues.put("relative_path", str4);
            contentValues.put("is_pending", (Integer) 1);
            Uri uriInsert = contentResolver.insert(MediaStore.Downloads.getContentUri("external_primary"), contentValues);
            if (uriInsert == null) {
                throw new IOException("MediaStore insert failed");
            }
            OutputStream outputStreamOpenOutputStream = contentResolver.openOutputStream(uriInsert);
            if (outputStreamOpenOutputStream == null) {
                throw new IOException("openOutputStream failed");
            }
            copy(inputStream, outputStreamOpenOutputStream);
            contentValues.clear();
            contentValues.put("is_pending", (Integer) 0);
            contentResolver.update(uriInsert, contentValues, null, null);
            return str4 + "/" + str2;
        }
        File file2 = new File(context.getExternalFilesDir(null), "Zola/" + str);
        if (!file2.exists() && !file2.mkdirs()) {
            throw new IOException("mkdirs " + file2);
        }
        File fileUnique2 = Downloads.unique(file2, str2);
        copy(inputStream, new FileOutputStream(fileUnique2));
        return fileUnique2.getAbsolutePath();
    }

    static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            try {
                byte[] bArr = new byte[65536];
                while (true) {
                    int i = inputStream.read(bArr);
                    if (i <= 0) {
                        break;
                    } else {
                        outputStream.write(bArr, 0, i);
                    }
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Throwable th) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (Throwable th3) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th4) {
                    th3.addSuppressed(th4);
                }
            }
            throw th3;
        }
    }
}
