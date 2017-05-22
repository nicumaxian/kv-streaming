package com.koniosoftworks.kvstreaming.data.server;

import com.google.inject.Inject;
import com.koniosoftworks.kvstreaming.domain.concurrency.TaskScheduler;
import com.koniosoftworks.kvstreaming.domain.props.ServerProperties;
import com.koniosoftworks.kvstreaming.domain.server.Server;
import com.koniosoftworks.kvstreaming.domain.video.RealTimeStreamingAlgorithm;
import com.koniosoftworks.kvstreaming.domain.video.StaticStreamingAlgorithm;
import com.koniosoftworks.kvstreaming.utils.Formatting;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Created by nicu on 5/15/17.
 */
public class ServerImpl implements Server {
    private ServerSocket serverSocket;
    private final ClientsPool clientsPool;
    private final TaskScheduler taskScheduler;
    private final Logger logger;
    private ServerContext serverContext;
    private DatagramSocket datagramSocket;
    private Runnable videoStreamingRunnable;

    @Inject
    public ServerImpl(TaskScheduler taskScheduler, ClientsPool clientsPool, Logger logger) {
        this.taskScheduler = taskScheduler;
        this.clientsPool = clientsPool;
        this.logger = logger;
    }

    @Override
    public void start(int tcpPort) {
        try {
            serverSocket = new ServerSocket(tcpPort);
            datagramSocket = new DatagramSocket();
            serverContext = new ServerContext(serverSocket.getInetAddress(),tcpPort,datagramSocket.getPort());
            logger.info("TCP Server started at " + Formatting.getConnectionInfo(serverSocket));
            logger.info("UDP Server started at " + Formatting.getConnectionInfo(datagramSocket));
            taskScheduler.run(this::waitForConnections);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void stop() {
        try {
            taskScheduler.stopAll();
            clientsPool.dropConnections();
            serverSocket.close();
            stopStreaming();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void startStreaming(StaticStreamingAlgorithm algorithm) {
        //todo
    }

    @Override
    public void startStreaming(RealTimeStreamingAlgorithm algorithm) {
        videoStreamingRunnable = () -> {
            byte[] currentImage = algorithm.getCurrentImage();

            DatagramPacket datagramPacket = new DatagramPacket(currentImage, currentImage.length);
            try {
                datagramSocket.send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        taskScheduler.schedule(videoStreamingRunnable,ServerProperties.VIDEO_STREAMING_INTERVAL_SENDING, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopStreaming() {
        if(videoStreamingRunnable != null){
            taskScheduler.unschedule(videoStreamingRunnable);
            videoStreamingRunnable = null;
        }
    }

    private void waitForConnections() {
        logger.info("Waiting for connections");
        while (!serverSocket.isClosed()) {
            if (clientsPool.getSize() < ServerProperties.MAX_CONNECTIONS_ALLOWED - 1) {
                try {
                    Socket socket = serverSocket.accept();
                    clientsPool.addNewClient(socket);
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }

}
