package com.cutecomm.liumm.screenrecorddemo;

import android.app.Activity;
import android.content.Context;

/**
 * Created by 25817 on 2016/11/17.
 */

public abstract class Manager {
    protected boolean start;
    protected Context context;

    public boolean isStart(){
        return start;
    }

    protected Context getContext(){
        return context;
    }

    public void start(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("ServerManager start activity pointer is null.");
        }
        if (!start) {
            context = activity;
            startImpl();
            start = true;
        }

    }

    public void stop() {
        if (start) {
            start = false;
            stopImpl();
        }
    }

    protected abstract void startImpl();
    protected abstract void stopImpl();
}
