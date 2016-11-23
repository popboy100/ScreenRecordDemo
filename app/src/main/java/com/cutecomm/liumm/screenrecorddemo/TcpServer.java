package com.cutecomm.liumm.screenrecorddemo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 25817 on 2016/11/17.
 */

public class TcpServer {
    private ServerSocket serverSocket;
    private List<Client> clients = new ArrayList<Client>();

    private volatile boolean start;
    private volatile int port = 8888;

    private AtomicInteger atomicCount = new AtomicInteger();

    private TcpServerListener tcpServerListener;

    public interface TcpServerListener {
        void onStartRecord();
        void onStopRecord();
    }

    public void setTcpServerListener(TcpServerListener listener) {
        tcpServerListener = listener;
    }

    public void start(int port) {
        this.port = port;
        stop();

        start = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(TcpServer.this.port);

                    while (start) {
                        Socket socket = serverSocket.accept();

                        if (socket != null) {
                            Client client = new Client(socket);
                            clients.add(client);
                            client.start();
                            int count = atomicCount.incrementAndGet();
                            if (count == 1) {
                                if (tcpServerListener != null) {
                                    tcpServerListener.onStartRecord();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    stop();
                }

            }
        }).start();
    }

    public void stop() {
        if (start) {
            start = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                serverSocket = null;
            }

            closeClients();
        }
    }

    private void closeClients() {
        for (Client client : clients) {
            if (client != null) {
                client.shutDown();
            }
        }
    }

    public void pushData(int offset, int size, int flags, int width, int height , long presentationTimeUsec, byte[] data) {
        if (data == null || data.length < 1) {
            return;
        }

        for (Client client : clients) {
            if (client != null) {
                client.pushData(offset, size, flags, width, height, presentationTimeUsec, data);
            }
        }
    }

    private class Client extends Thread{
        private Socket socket;
        private volatile boolean start = true;
        private DataOutputStream outputStream;
        public Client(Socket socket) {
            this.socket = socket;
            try {
                socket.setTcpNoDelay(true);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            try {
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void shutDown() {
            if (start) {
                start = false;
                int count = atomicCount.decrementAndGet();
                if (count == 0) {
                    if (tcpServerListener != null) {
                        tcpServerListener.onStopRecord();
                    }
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outputStream = null;
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
            }


        }

        public void pushData(int offset, int size, int flags, int width, int height , long presentationTimeUsec, byte[] data) {
            if (outputStream != null) {
                try {
                    outputStream.writeInt(width);
                    outputStream.writeInt(height);
                    outputStream.writeInt(offset);
                    outputStream.writeInt(size);
                    outputStream.writeInt(flags);
                    outputStream.writeLong(presentationTimeUsec);
                    outputStream.write(data);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            super.run();
        }
    }
}
