package com.cutecomm.liumm.screenrecorddemo;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by 25817 on 2016/11/17.
 */

public class ClientManager extends Manager {

    private static final String TAG = "ClientManager";
    private static ClientManager instance;
    private SurfaceView surfaceView;
    private TextureView textureView;
    private SurfaceTexture surfaceTexture;
    private Activity activity;
    private TcpClient tcpClient;
    private String serverAddress;
    private int serverPort;
    private MediaCodec decoder;
    private List<EncodeDataHolder> encodeDatas = new LinkedList<EncodeDataHolder>();
    private Object outLock = new Object();
    private boolean surfaceCreated;
    private boolean useSurfaceView;

    public static ClientManager getInstance() {
        if (instance == null) {
            synchronized (ClientManager.class) {
                if (instance == null) {
                    instance = new ClientManager();
                }
            }
        }
        return instance;
    }

    protected ClientManager() {
    }

    public void setSurfaceView(Activity activity, SurfaceView surfaceView) {
        if (this.surfaceView != surfaceView) {
            this.surfaceView = surfaceView;
            this.surfaceView.getHolder().addCallback(callback2);
        }

        this.activity = activity;
        useSurfaceView = true;
    }

    public void setTextureView(Activity activity, TextureView textureView) {
        if (this.textureView != textureView) {
            this.textureView = textureView;
            this.textureView.setSurfaceTextureListener(textureListener);
        }

        this.activity = activity;
        useSurfaceView = false;
    }

    public void setServerAddress(String address, int port) {
        this.serverAddress = address;
        this.serverPort = port;
    }

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (!isStart()) {
                start(activity);
            }

            synchronized (outLock) {
                surfaceCreated = true;
            }

            surfaceTexture = surface;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            pause();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private SurfaceHolder.Callback2 callback2 = new SurfaceHolder.Callback2() {
        @Override
        public void surfaceRedrawNeeded(SurfaceHolder holder) {

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (!isStart()) {
                start(activity);
            }

            synchronized (outLock) {
                surfaceCreated = true;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            pause();
        }
    };

    @Override
    protected void startImpl() {
        startTcpClient();
    }

    @Override
    protected void stopImpl() {
        tcpClient.setTcpClientListener(null);
        if (decoder != null) {
            encodeDatas.add(new EncodeDataHolder(0, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM, 0, null));
//            decoder.stop();
//            decoder.release();
//            decoder = null;
        }
        stopTcpClient();
    }

    private void pause() {
        synchronized (outLock) {
            surfaceCreated = false;
        }
    }

    private MediaCodec.Callback callback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            synchronized (this) {
                ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);

                EncodeDataHolder encodeDataHolder = null;
                while (isStart()) {
                    while (encodeDatas.isEmpty()) {
                        if (!isStart()) {
                            codec.queueInputBuffer(
                                    index,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            return;
                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    encodeDataHolder = encodeDatas.remove(0);

                    if (encodeDataHolder != null) {
                        break;
                    }
                }

                if (!isStart()) {
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    return;
                }

//                Log.d(TAG, "encodeDataHolder width=" + encodeDataHolder.width + " height=" + encodeDataHolder.height
//                        + " offset=" + encodeDataHolder.bufferInfo.offset + " size=" + encodeDataHolder.bufferInfo.size
//                        + " flags=" + encodeDataHolder.bufferInfo.flags);
                if ((encodeDataHolder.bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    return;
                }

                if (encodeDataHolder.bufferInfo.size > 0) {
                    decoderInputBuffer.clear();
                    decoderInputBuffer.put(encodeDataHolder.encodeData);
                    codec.queueInputBuffer(
                            index,
                            0,
                            encodeDataHolder.bufferInfo.size,
                            encodeDataHolder.bufferInfo.presentationTimeUs,
                            encodeDataHolder.bufferInfo.flags);
                } else if (encodeDataHolder.bufferInfo.size == 0) {
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
            }
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                mediaCodec.releaseOutputBuffer(index, false);
                Log.d(TAG, "BUFFER_FLAG_CODEC_CONFIG");
//                return;
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                Log.d(TAG, "BUFFER_FLAG_KEY_FRAME");
//                return;
            }

            if ((info.flags & MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) != 0) {
                Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
//                return;
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                codec.releaseOutputBuffer(index, false);
                return;
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                if (decoder != null) {
                    decoder.stop();
                    decoder.release();
                    decoder = null;
                }
                return;
            }

            synchronized (outLock) {
                boolean render = info.size != 0;
                if (!surfaceCreated) {
                    render = false;
                }
                codec.releaseOutputBuffer(index, render);
            }
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {

        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {

        }
    };

    private void startTcpClient() {
        stopTcpClient();

        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            decoder.setCallback(callback);
        } catch (IOException e) {
            e.printStackTrace();
            stop();
            return;
        }
        tcpClient = new TcpClient(serverAddress, serverPort);
        tcpClient.setTcpClientListener(new TcpClient.TcpClientListener() {
            @Override
            public void onNewFrame(int width, int height, int offset, int size,
                                   int flags, long presentationTimeUsec, byte[] data) {
                if ((flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                    format.setByteBuffer("csd-0", ByteBuffer.wrap(data));
//                    decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    if (useSurfaceView) {
                        decoder.configure(format, surfaceView.getHolder().getSurface(), null, 0);
                    } else {
                        decoder.configure(format, new Surface(surfaceTexture), null, 0);
                    }
                    decoder.start();
                    return;
                }

                encodeDatas.add(new EncodeDataHolder(width, height, offset, size, flags, presentationTimeUsec, data));
                if ((flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                    Log.d(TAG, "I Frame ....");
                }
            }

            @Override
            public void onStop() {
                stop();
            }
        });
        tcpClient.start();
    }

    private void stopTcpClient() {
        if (tcpClient != null) {
            tcpClient.shutDown();
            tcpClient = null;
        }
    }

    private class EncodeDataHolder {
        public MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        public int width;
        public int height;
        public byte[] encodeData;

        public EncodeDataHolder(int width, int height, int offset, int size, int flags,
                                long presentationTimeUsec, byte[] data) {
            bufferInfo.offset = offset;
            bufferInfo.size = size;
            bufferInfo.flags = flags;
            bufferInfo.presentationTimeUs = presentationTimeUsec;

            this.width = width;
            this.height = height;
            encodeData = data;
        }
    }
}
