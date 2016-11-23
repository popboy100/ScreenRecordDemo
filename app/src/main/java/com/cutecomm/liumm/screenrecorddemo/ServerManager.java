package com.cutecomm.liumm.screenrecorddemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by 25817 on 2016/11/17.
 */

public class ServerManager extends Manager implements TcpServer.TcpServerListener {
    private static final String TAG = "ServerManager";
    private static final int REQUEST_MEDIA_PROJECTION = 789;
    private static ServerManager instance;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private TcpServer tcpServer;
    private int port = 8888;
    private RecordThread recordThread;

    public static ServerManager getInstance() {
        if (instance == null) {
            synchronized (ServerManager.class) {
                if (instance == null) {
                    instance = new ServerManager();
                }
            }
        }
        return instance;
    }

    protected ServerManager() {
    }

    public void setServerBindPort(int port) {
        this.port = port;
    }

    @Override
    protected void startImpl() {
        if (mediaProjectionManager == null) {
            mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        if (context instanceof Activity) {
            ((Activity)context).startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        }

        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(RecordService.ACTION_START);
        context.startService(intent);
    }

    @Override
    protected void stopImpl() {
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        stopScreenRecord();
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(RecordService.ACTION_STOP);
        context.startService(intent);
    }

    private void startTcpServer() {
        tcpServer = new TcpServer();
        tcpServer.setTcpServerListener(this);
        tcpServer.start(port);
    }

    private void stopTcpServer() {
        if (tcpServer != null) {
            tcpServer.setTcpServerListener(null);
            tcpServer.stop();
            tcpServer = null;
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection != null) {
                stopTcpServer();
                startTcpServer();
//                startScreenRecord();
            }
        }
    }

    @Override
    public void onStartRecord() {
        stopScreenRecord();
        startScreenRecord();
    }

    @Override
    public void onStopRecord() {
        stopScreenRecord();
    }

    private void startScreenRecord() {
        recordThread = new RecordThread();
        recordThread.start();
    }

    private void stopScreenRecord() {
        if (recordThread != null) {
            recordThread.shutDown();
            recordThread = null;
        }
    }

    private class RecordThread extends Thread {
        private static final String DISPLAY_NAME = "RemoteDesktop";
        private static final int SCALE = 2;
        private static final int BIT_RATE = 1 * 1024 * 1024;
        private static final int FPS = 15;
        private static final int I_FRAME_INTERVAL = 40; // second
        private int width;
        private int height;
        private int dpi;

        private volatile boolean start = true;

        private MediaCodec mediaCodec;
        private Surface surface;
        private VirtualDisplay virtualDisplay;
        private MediaFormat outputFormat;

        private long frameIndex = 0;

        public void shutDown() {
            if (start) {
                start = false;
            }

            if (mediaCodec != null) {
                mediaCodec.signalEndOfInputStream();
            }
        }

        private void release() {
            stopMuxer();

            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }

            if (surface != null) {
                surface.release();
                surface = null;
            }

            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }

            stopTcpServer();

            frameIndex = 0;
        }

        @Override
        public void run() {
            if (setting()) {
                start = true;
            }
        }

        private MediaCodec.Callback callback = new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(MediaCodec codec, int index) {

            }

            @Override
            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                Log.d(TAG, "onOutputBufferAvailable index=" + index);
//                muxVideo(index, info);
                pushEncodeData(codec, index, info);
            }

            @Override
            public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                Log.e(TAG, "" + e.getMessage());
                release();
            }

            @Override
            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                Log.d(TAG, "onOutputFormatChanaged = " + format.toString());
                if (codec == mediaCodec) {
                    outputFormat = format;
//                    setupMuxer();
                }
            }
        };

        private boolean setting() {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            width = dm.widthPixels / SCALE;
            height = dm.heightPixels / SCALE;
            dpi = dm.densityDpi;

            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);

            try {
                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }

            mediaCodec.setCallback(callback);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            outputFormat = mediaCodec.getOutputFormat();
            surface = mediaCodec.createInputSurface();

            if (mediaProjection == null) {
                return false;
            }


            virtualDisplay = mediaProjection.createVirtualDisplay(DISPLAY_NAME,
                            width,
                            height,
                            dpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            surface,
                            null,
                            null);


            mediaCodec.start();

            return true;
        }


        /**
         * Generates the presentation time for frame N, in microseconds.
         */
        private long computePresentationTimeUsec(long frameIndex) {
            final long ONE_MILLION = 1000000000;
            return frameIndex * ONE_MILLION / FPS;
        }

        private void pushEncodeData(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            Log.d(TAG, "pushEncodeData index=" + index + " flags=" + info.flags);
            ByteBuffer encoderOutputBuffer = mediaCodec.getOutputBuffer(index);
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


            if (info.size > 0) {

                long presentationTimeUsec = computePresentationTimeUsec(frameIndex);
                int offset = info.offset;
                int size = info.size;
                int flags = info.flags;
                byte[] data = new byte[info.size];
                encoderOutputBuffer.get(data);
                frameIndex++;

                if (tcpServer != null) {
                    tcpServer.pushData(offset, size, flags, width, height, presentationTimeUsec, data);
                }
            }
            mediaCodec.releaseOutputBuffer(index, false);
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                release();
            }
        }

        private MediaMuxer muxer;

        private String outFile = "/sdcard/test-video.mp4";
        private int outputVideoTrack = -1;

        private void createMuxer() {
            stopMuxer();
            if (muxer == null) {
                try {
                    muxer = new MediaMuxer(outFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }


        }

        private void muxVideo(int index, MediaCodec.BufferInfo info) {
            ByteBuffer encoderOutputBuffer = mediaCodec.getOutputBuffer(index);
            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                mediaCodec.releaseOutputBuffer(index, false);
                return;
            }

            if (info.size > 0) {
                encoderOutputBuffer.position(info.offset);
                encoderOutputBuffer.limit(info.offset + info.size);
                muxer.writeSampleData(
                        outputVideoTrack, encoderOutputBuffer, info);
            }
            mediaCodec.releaseOutputBuffer(index, false);
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                release();
            }
        }

        private void setupMuxer() {
            createMuxer();
            outputVideoTrack = muxer.addTrack(outputFormat);
            muxer.start();
        }

        private void stopMuxer() {
            if (muxer != null) {
                muxer.stop();
                muxer.release();
                muxer = null;
            }
        }
    }


}
