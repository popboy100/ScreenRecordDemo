package com.cutecomm.liumm.screenrecorddemo;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by 25817 on 2016/11/18.
 */

public class TcpClient extends Thread {

    private String ip;
    private int port;
    private DataInputStream inputStream;
    private Socket socket;
    private boolean start = true;

    private TcpClientListener listener;

    public interface TcpClientListener {
        void onNewFrame(int width, int height, int offset, int size, int flags, long presentationTimeUsec, byte[] data);
        void onStop();
    }

    public void setTcpClientListener(TcpClientListener listener) {
        this.listener = listener;
    }

    public TcpClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void shutDown() {
        start = false;

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }

        if (listener != null) {
            listener.onStop();
        }
    }

    @Override
    public void run() {
        try {
            socket = new Socket(ip, port);
        } catch (IOException e) {
            e.printStackTrace();
            shutDown();
            return;
        }

        try {
            socket.setTcpNoDelay(true);
        } catch (SocketException e) {
            e.printStackTrace();
            shutDown();
            return;
        }

        try {
            inputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            shutDown();
            return;
        }

        if (!start) {
            shutDown();
            return;
        }

        while (start) {
            if (socket == null || socket.isClosed()) {
                break;
            }
            if (inputStream == null) {
                break;
            }

            try {
                int width = inputStream.readInt();
                int height = inputStream.readInt();
                int offset = inputStream.readInt();
                int size = inputStream.readInt();
                int flags = inputStream.readInt();
                long presentationTimeUsec = inputStream.readLong();
                byte[] data = new byte[size];
                int offset1 = 0;
                int size1 = size;
                while (size1 > 0) {
                    int count = inputStream.read(data, offset1, size1);
                    offset1 += count;
                    size1 -= count;
                }

                if (listener != null) {
                    listener.onNewFrame(width, height, offset, size, flags, presentationTimeUsec, data);
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        shutDown();
    }
}
