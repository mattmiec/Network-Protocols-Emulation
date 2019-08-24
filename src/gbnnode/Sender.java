package gbnnode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import static java.lang.System.arraycopy;

public class Sender extends Thread {

    // packet is an int sequence number plus a single byte of data
    private final int packetLength = 5;

    private BlockingQueue<String> queue;
    private DatagramSocket socket;
    private int windowSize;
    private int peerPort;

    public void run() {
        while (true) {
            char charToSend;
            try {
                charToSend = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            var packetBytes = new byte[packetLength];
            arraycopy(ByteBuffer.allocate(packetLength - 1).putInt(0).array(), 0, packetBytes, 0, packetLength - 1);
            packetBytes[packetLength - 1] = (byte) charToSend;
            try {
                var packet = new DatagramPacket(packetBytes, packetLength, InetAddress.getByName("127.0.0.1"), peerPort);
                socket.send(packet);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Sender(BlockingQueue<String> queue, DatagramSocket socket, int windowSize, int peerPort) {
        this.queue = queue;
        this.socket = socket;
        this.windowSize = windowSize;
        this.peerPort = peerPort;
    }
}
