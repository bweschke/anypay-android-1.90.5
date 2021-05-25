package com.anywherecommerce.anypaysampleappcloud;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class PermissionsController {

    /**
     * Permissions
     */
    public static String[] permissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
    };

    public static ArrayList<String> grantedPermissions = new ArrayList<String>();

    public static boolean verifyAppPermissions(Activity activity) {

        grantedPermissions.clear();
        boolean hasNecessaryPermissions = true;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && activity != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    hasNecessaryPermissions = false;
                }
                else {
                    grantedPermissions.add(permission);
                }
            }
        }
        return hasNecessaryPermissions;
    }

    public static boolean verifyAppPermission(Activity activity, String permission)
    {
        // Check for device permission
        if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return true;
    }

    public static void requestAppPermissions(Activity activity, String s, int reqCode)
    {
        ActivityCompat.requestPermissions(activity, new String[]{s}, reqCode);
    }

    public static void requestAppPermissions(Activity activity, String[] s, int reqCode)
    {
        ActivityCompat.requestPermissions(activity, s, reqCode);
    }
}
