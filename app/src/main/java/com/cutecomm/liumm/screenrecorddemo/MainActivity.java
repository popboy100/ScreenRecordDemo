package com.cutecomm.liumm.screenrecorddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class MainActivity extends Activity implements View.OnClickListener{

    private Button mServerBtn;
    private Button mClientBtn;
    private RelativeLayout mButtonArea;
    private EditText mIpEditText;
    private EditText mPortEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButtonArea = (RelativeLayout)findViewById(R.id.buttonLayout);
        mServerBtn = (Button)findViewById(R.id.button);
        mClientBtn = (Button)findViewById(R.id.button2);
        mIpEditText = (EditText)findViewById(R.id.editText);
        mPortEditText = (EditText)findViewById(R.id.editText2);

        mServerBtn.setOnClickListener(this);
        mClientBtn.setOnClickListener(this);

        mServerBtn.setText(ServerManager.getInstance().isStart() ? R.string.stop_server : R.string.start_server);

        if (ServerManager.getInstance().isStart()) {
            mClientBtn.setVisibility(View.GONE);
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button) {
            ServerManager serverManager = ServerManager.getInstance();
            if (serverManager.isStart()) {
                stopServer();
                mServerBtn.setText(R.string.start_server);
            } else {
                startServer();
                mServerBtn.setText(R.string.stop_server);
            }
        } else if (v.getId() == R.id.button2) {
            startClient();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ServerManager.getInstance().onActivityResult(requestCode, resultCode, data);
    }

    private void startServer() {
        if (!ServerManager.getInstance().isStart()) {
            ServerManager.getInstance().start(this);
        }
    }

    private void stopServer() {
        if (ServerManager.getInstance().isStart()) {
            ServerManager.getInstance().stop();
        }
    }

    private void startClient() {
        Intent intent = new Intent(this, ClientActivity.class);
        intent.putExtra("ip", mIpEditText.getText().toString());
        intent.putExtra("port", Integer.valueOf(mPortEditText.getText().toString()));
        startActivity(intent);
    }
}
