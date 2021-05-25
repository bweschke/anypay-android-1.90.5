package com.anywherecommerce.anypaysampleappcloud;

import android.app.Application;

import com.anywherecommerce.android.sdk.AnyPay;
import com.anywherecommerce.android.sdk.SDKManager;

public class AppManager extends Application {

    public void onCreate()
    {
        super.onCreate();

        // The first step should always be to initialize the SDK.
        AnyPay.initialize(this);
    }
}
