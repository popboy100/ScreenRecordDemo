package com.cutecomm.liumm.screenrecorddemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Created by 25817 on 2016/11/18.
 */

public class ClientActivity extends Activity {
    private TextureView videoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_client);
        Intent intent = getIntent();
        String ip = intent.getStringExtra("ip");
        int port = intent.getIntExtra("port", 8888);

//        SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        FrameLayout fl = (FrameLayout)findViewById(R.id.ll);
        SurfaceView surfaceView = new MySurfaceView(this);
        fl.addView(surfaceView);
//        videoView = (TextureView)findViewById(R.id.textureView);
        ClientManager.getInstance().setServerAddress(ip, port);
//        ClientManager.getInstance().setTextureView(this, videoView);
        ClientManager.getInstance().setSurfaceView(this, surfaceView);
    }

    @Override
    protected void onDestroy() {
        ClientManager.getInstance().stop();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //横屏
            } else {
                //竖屏
            }
        }
    }
}
