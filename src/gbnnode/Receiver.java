package gbnnode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;

public class Receiver extends Thread {

    // packet is an int sequence number plus a single byte of data
    private final int maxPacketLength = 1024;

    private BlockingQueue<Integer> ackQueue;
    private DatagramSocket socket;
    private int windowSize;
    private boolean isDeterministic;
    private int n;
    private double p;
    private int expectedSeqNum;


    public void run() {
        while (true) {
            // listen for packet
            var udpPacket = new DatagramPacket(new byte[maxPacketLength], maxPacketLength);
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
                if (gbnPacket.getSequenceNum() == 0) {
                    System.out.println(); // print newline if receiving new message
                }
                if (gbnPacket.getSequenceNum() == -1) {
                    endTransmission();
                    expectedSeqNum = 0;
                    continue;
                }
                if (gbnPacket.getSequenceNum() != expectedSeqNum) {
                    System.out.println(String.format(
                            "[%s] packet%d %c discarded",
                            Calendar.getInstance().getTime(),
                            gbnPacket.getSequenceNum(),
                            (char)gbnPacket.getDataByte()));
                    continue;
                }
                System.out.println(String.format(
                        "[%s] packet%d %c received",
                        Calendar.getInstance().getTime(),
                        gbnPacket.getSequenceNum(),
                        (char)gbnPacket.getDataByte()));
                // then send ack
                var ackPacket = Packet.createAck(gbnPacket.getSequenceNum());
                var ackPacketSerialized = ackPacket.serialize();
                udpPacket = new DatagramPacket(ackPacketSerialized, ackPacketSerialized.length, udpPacket.getAddress(), udpPacket.getPort());
                try {
                    socket.send(udpPacket);
                    System.out.println(String.format(
                            "[%s] ACK%d sent, expecting packet %d",
                            Calendar.getInstance().getTime(),
                            gbnPacket.getSequenceNum(),
                            gbnPacket.getSequenceNum() + 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ++expectedSeqNum;
            }

        }
    }

    private void endTransmission() {
        System.out.println("[Summary] ...");
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
