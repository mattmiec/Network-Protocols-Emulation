package gbnnode;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.lang.System.arraycopy;

public class GbnNode {

    public static void main(String[] args) {
        // first parse input args
	    if (args.length != 5) {
	        System.out.println("call with arguments <self-port> <peer-port> <window-size> [-d <value-of-n> | -p <value-of-p>]");
	        return;
        }
	    int selfPort = Integer.parseInt(args[0]);
	    int peerPort = Integer.parseInt(args[1]);
	    int windowSize = Integer.parseInt(args[2]);
	    boolean isDeterministic = true;
	    int n = 0;
	    double p = 0.0;
        if (args[3].equals("-d")) {
            isDeterministic = true;
            n = Integer.parseInt(args[4]);
        } else if (args[3].equals("-p")) {
            isDeterministic = false;
            p = Float.parseFloat(args[4]);
        } else {
            System.out.println("call with arguments <self-port> <peer-port> <window-size> [-d <value-of-n> | -p <value-of-p>]");
            return;
        }

        // construct socket
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(selfPort);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        // setup and start receiver
        Receiver receiver;
        if (isDeterministic) {
            receiver = new Receiver(socket, windowSize, n);
        } else {
            receiver = new Receiver(socket, windowSize, p);
        }
        receiver.start();

        // setup and start sender
        var senderQueue = new ArrayBlockingQueue<Character>(256);
        Sender sender = new Sender(senderQueue, socket, windowSize, peerPort);
        sender.start();

        shell(senderQueue);
    }

    public static void shell(BlockingQueue<Character> queue) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("node> ");
            var input = scanner.nextLine();
            String[] parsed = input.split("\\s+");
            if (parsed.length != 2 || !parsed[0].equals("send") || parsed[1].length() != 1) {
                System.out.println("command format: send <char>");
                continue;
            }
            try {
                queue.put(parsed[1].charAt(0));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

class Sender extends Thread {

    // packet is an int sequence number plus a single byte of data
    private final int packetLength = 5;

    private BlockingQueue<Character> queue;
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
            arraycopy(ByteBuffer.allocate(4).putInt(0).array(), 0, packetBytes, 0, 4);
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

    Sender(BlockingQueue<Character> queue, DatagramSocket socket, int windowSize, int peerPort) {
        this.queue = queue;
        this.socket = socket;
        this.windowSize = windowSize;
        this.peerPort = peerPort;
    }
}

class Receiver extends Thread {

    // packet is an int sequence number plus a single byte of data
    private final int packetLength = 5;

    private DatagramSocket socket;
    private int windowSize;
    private boolean isDeterministic;
    private int n;
    private double p;


    public void run() {
        while (true) {
            // listen for packet
            var packet = new DatagramPacket(new byte[packetLength], packetLength);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                System.out.println("receiver closing on IOException, stacktrace:");
                e.printStackTrace();
                return;
            }

            // parse packet
            var sequenceNum = ByteBuffer.wrap(Arrays.copyOfRange(packet.getData(), 0, packetLength)).getInt();
            var dataByte = packet.getData()[packetLength - 1];

            // print output
            System.out.println((char)dataByte);
        }
    }

    Receiver(DatagramSocket socket, int windowSize, int n) {
        this.socket = socket;
        this.windowSize = windowSize;
        this.n = n;
        this.isDeterministic = true;
    }

    Receiver(DatagramSocket socket, int windowSize, double p) {
        this.socket = socket;
        this.windowSize = windowSize;
        this.p = p;
        this.isDeterministic = false;
    }
}
