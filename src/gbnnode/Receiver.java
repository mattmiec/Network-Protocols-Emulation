package gbnnode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class Receiver extends Thread {

    // packet is an int sequence number plus a single byte of data
    private final int packetLength = 1024;

    private BlockingQueue<Integer> ackQueue;
    private DatagramSocket socket;
    private int windowSize;
    private boolean isDeterministic;
    private int n;
    private double p;


    public void run() {
        while (true) {
            // listen for packet
            var udpPacket = new DatagramPacket(new byte[packetLength], packetLength);
            try {
                socket.receive(udpPacket);
            } catch (IOException e) {
                System.out.println("receiver closing on IOException, stacktrace:");
                e.printStackTrace();
                return;
            }

            // deserialize packet
            var gbnPacket = Packet.deserialize(udpPacket.getData());

            if (gbnPacket.getIsAck()) {
                // if this is an ack put it in ackQueue
                try {
                    this.ackQueue.put(gbnPacket.getSequenceNum());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // otherwise print output data
                System.out.println((char)gbnPacket.getDataByte());
                // then send ack
                var ackPacket = Packet.createAck(gbnPacket.getSequenceNum());
                var ackPacketSerialized = ackPacket.serialize();
                udpPacket = new DatagramPacket(ackPacketSerialized, ackPacketSerialized.length, udpPacket.getAddress(), udpPacket.getPort());
                try {
                    socket.send(udpPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    Receiver(BlockingQueue<Integer> ackQueue, DatagramSocket socket, int windowSize, int n) {
        this.ackQueue = ackQueue;
        this.socket = socket;
        this.windowSize = windowSize;
        this.n = n;
        this.isDeterministic = true;
    }

    Receiver(BlockingQueue<Integer> ackQueue, DatagramSocket socket, int windowSize, double p) {
        this.ackQueue = ackQueue;
        this.socket = socket;
        this.windowSize = windowSize;
        this.p = p;
        this.isDeterministic = false;
    }
}
