package gbnnode;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
        var ackQueue = new ArrayBlockingQueue<Integer>(256);
        Receiver receiver;
        if (isDeterministic) {
            receiver = new Receiver(ackQueue, socket, windowSize, n);
        } else {
            receiver = new Receiver(ackQueue, socket, windowSize, p);
        }
        receiver.start();

        // setup and start sender
        Sender sender;
        sender = new Sender(ackQueue, socket, windowSize, peerPort);
        sender.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("node> ");
            var input = scanner.nextLine();
            String[] parsed = input.split("\\s+");
            if (parsed.length != 2 || !parsed[0].equals("send")) {
                System.out.println("command format: send <char>");
                continue;
            }
            sender.send(parsed[1]);
        }
    }

}

