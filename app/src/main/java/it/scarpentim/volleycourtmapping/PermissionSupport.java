package it.scarpentim.volleycourtmapping;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class PermissionSupport {

    private static final String TAG = "volleyCourt";

    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 1;

    private static void validateReadStoragePermission(Activity context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission is not granted");
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_STORAGE);
        }
    }
}
