package gbnnode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.lang.System.setErr;

public class Sender extends Thread {

    private BlockingQueue<Integer> ackQueue;
    private DatagramSocket socket;
    private int windowSize;
    private volatile int windowBase;
    private int peerPort;
    private String sendBuffer;
    private Timer timer;
    private volatile boolean transmissionIsComplete = false;
    private int totalPackets = 0;
    private int totalDropped = 0;

    // main thread waits for acks
    public void run() {
        while(true) {
            try {
                int ackNum = ackQueue.take();
                totalPackets++;
                if (ackNum == -1) {
                    totalDropped++;
                    continue;
                }
                if (ackNum == this.windowBase) {
                    System.out.println(String.format(
                            "[%s] ack%d received, window moves to %d",
                            Calendar.getInstance().getTime(),
                            this.windowBase,
                            this.windowBase + 1));
                    this.windowBase++;
                    this.timer.cancel();
                    this.timer = new Timer();
                    this.timer.schedule(new TimerTask() {
                        public void run() {
                            sendN();
                        }
                    },500);

                } else {
                    System.out.println(String.format(
                            "[%s] ack%d discarded",
                            Calendar.getInstance().getTime(),
                            ackNum));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(String stringToSend) {
        this.transmissionIsComplete = false;
        this.sendBuffer = stringToSend;
        this.windowBase = 0;
        this.timer = new Timer();
        sendN();
        try {
            while (windowBase < sendBuffer.length()) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return;
    }

    synchronized void sendN() {
        timer.cancel();
        if (this.windowBase >= sendBuffer.length()) {
            if (transmissionIsComplete) return;
            transmissionIsComplete = true;
            sendByte((byte)0, -1); // send packet with sequence number -1 to indicate end of transmission
            System.out.println(String.format("[Summary] %d/%d packets discarded, loss rate = %f%%", totalDropped, totalPackets, (float)totalDropped/(float)totalPackets));
            return;
        };
        int windowEnd = min(this.windowBase + windowSize, sendBuffer.length());
        for (int i = this.windowBase; i < windowEnd; i++) {
            sendByte((byte)sendBuffer.charAt(i), i);
            System.out.println(String.format(
                    "[%s] packet%d %c sent",
                    Calendar.getInstance().getTime(),
                    i,
                    sendBuffer.charAt(i)));
        }
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            public void run() {
                sendN();
            }
        },500);
    }

    private void sendByte(byte b, int sequenceNum) {
        byte[] packetBytes = (Packet.createDataPacket(sequenceNum, b)).serialize();
        try {
            DatagramPacket udpPacket = new DatagramPacket(packetBytes, packetBytes.length, InetAddress.getByName("127.0.0.1"), this.peerPort);
            this.socket.send(udpPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Sender(BlockingQueue<Integer> ackQueue, DatagramSocket socket, int windowSize, int peerPort) {
        this.ackQueue = ackQueue;
        this.socket = socket;
        this.windowSize = windowSize;
        this.peerPort = peerPort;
    }
}
