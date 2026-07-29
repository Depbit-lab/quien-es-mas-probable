package com.depbit.sinfiltro;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public class ApkFileProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private File resolve(Uri uri) throws FileNotFoundException {
        if (!"apk".equals(uri.getPathSegments().isEmpty() ? "" : uri.getPathSegments().get(0))) throw new FileNotFoundException();
        File file = new File(new File(getContext().getCacheDir(), "shared"), "QuienEsMasProbable.apk");
        if (!file.exists()) throw new FileNotFoundException();
        return file;
    }
    @Override public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException { return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY); }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file=resolve(uri); MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});
            c.addRow(new Object[]{"QuienEsMasProbable.apk",file.length()}); return c;
        } catch(Exception e){ return null; }
    }
    @Override public int delete(Uri uri,String selection,String[] selectionArgs){return 0;}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs){return 0;}
    @Override public Uri insert(Uri uri,ContentValues values){return null;}
}
