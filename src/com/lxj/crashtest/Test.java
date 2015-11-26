package com.lxj.crashtest;

import android.app.Application;

public class Test extends Application {

    private static Test sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        //在这里为应用设置异常处理程序，然后我们的程序才能捕获未处理的异常
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
    }

    public static Test getInstance() {
        return sInstance;
    }

}
