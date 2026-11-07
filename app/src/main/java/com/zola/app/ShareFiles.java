package com.zola.app;

import android.content.ClipData;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class ShareFiles extends ContentProvider {
    static final String AUTHORITY = "com.zola.app.share";

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        return true;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }

    static File dir(Context context) {
        return new File(context.getCacheDir(), "share");
    }

    static void share(Context context, String str, String str2, String str3) {
        File fileDir = dir(context);
        File[] fileArrListFiles = fileDir.listFiles();
        if (fileArrListFiles != null) {
            for (File file : fileArrListFiles) {
                file.delete();
            }
        }
        fileDir.mkdirs();
        String strReplaceAll = new File(str).getName().replaceAll("[^A-Za-z0-9._-]", "_");
        if (strReplaceAll.isEmpty()) {
            strReplaceAll = "zalo-file";
        }
        File file2 = new File(fileDir, strReplaceAll);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                fileOutputStream.write(Base64.decode(str3, 0));
                fileOutputStream.close();
                Uri uri = Uri.parse("content://com.zola.app.share/" + Uri.encode(file2.getName()));
                Intent intent = new Intent("android.intent.action.SEND");
                if (str2 == null || str2.isEmpty()) {
                    str2 = "application/octet-stream";
                }
                Intent intentAddFlags = intent.setType(str2).putExtra("android.intent.extra.STREAM", uri).addFlags(1);
                intentAddFlags.setClipData(ClipData.newRawUri(file2.getName(), uri));
                try {
                    context.startActivity(Intent.createChooser(intentAddFlags, null).addFlags(268435456));
                } catch (RuntimeException e) {
                    Log.w("Zola", "share: no chooser", e);
                }
            } catch (Throwable th) {
                try {
                    fileOutputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException | IllegalArgumentException e2) {
            Log.w("Zola", "share: write failed", e2);
        }
    }

    private File fileFor(Uri uri) throws FileNotFoundException {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null || getContext() == null) {
            throw new FileNotFoundException();
        }
        File file = new File(dir(getContext()), new File(lastPathSegment).getName());
        if (file.isFile()) {
            return file;
        }
        throw new FileNotFoundException(lastPathSegment);
    }

    @Override // android.content.ContentProvider
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(uri), 268435456);
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        String fileExtensionFromUrl = MimeTypeMap.getFileExtensionFromUrl(String.valueOf(uri.getLastPathSegment()));
        String mimeTypeFromExtension = fileExtensionFromUrl == null ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtensionFromUrl.toLowerCase());
        return mimeTypeFromExtension != null ? mimeTypeFromExtension : "application/octet-stream";
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        try {
            File fileFileFor = fileFor(uri);
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_display_name", "_size"});
            matrixCursor.addRow(new Object[]{fileFileFor.getName(), Long.valueOf(fileFileFor.length())});
            return matrixCursor;
        } catch (FileNotFoundException unused) {
            return null;
        }
    }
}
