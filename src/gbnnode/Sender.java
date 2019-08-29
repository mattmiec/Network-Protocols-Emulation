package gbnnode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;

import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.lang.System.setErr;

public class Sender extends Thread {

    private BlockingQueue<String> queue;
    private BlockingQueue<Integer> ackQueue;
    private DatagramSocket socket;
    private int windowSize;
    private int peerPort;

    public void run() {
        while (true) {
            String stringToSend;
            try {
                stringToSend = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            int seqBase = 0;
            while (seqBase < stringToSend.length()) {
                int windowEnd = min(seqBase + windowSize, stringToSend.length());
                for (int i = seqBase; i < windowEnd; i++) {
                    sendByte((byte)stringToSend.charAt(i), i);
                    System.out.println(String.format(
                            "[%s] packet%d %c sent",
                            Calendar.getInstance().getTime(),
                            i,
                            stringToSend.charAt(i)));
                }
                for (int i = seqBase; i < windowEnd; i++) {
                    try {
                        var ackNum = ackQueue.take();
                        if (ackNum == seqBase) {
                            System.out.println(String.format(
                                    "[%s] ack%d received, window moves to %d",
                                    Calendar.getInstance().getTime(),
                                    i,
                                    seqBase + 1));
                            seqBase++;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }
    }

    private void sendByte(byte b, int sequenceNum) {
        var packetBytes = (Packet.createDataPacket(sequenceNum, b)).serialize();
        try {
            var udpPacket = new DatagramPacket(packetBytes, packetBytes.length, InetAddress.getByName("127.0.0.1"), this.peerPort);
            this.socket.send(udpPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Sender(BlockingQueue<String> queue, BlockingQueue<Integer> ackQueue, DatagramSocket socket, int windowSize, int peerPort) {
        this.queue = queue;
        this.ackQueue = ackQueue;
        this.socket = socket;
        this.windowSize = windowSize;
        this.peerPort = peerPort;
    }
}
