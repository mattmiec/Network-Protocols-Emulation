package gbnnode;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.lang.System.arraycopy;

public class Packet {
    private static final int sequenceNumLength = 4;

    private int sequenceNum;
    private boolean isAck;
    private byte dataByte;

    // private constructor
    private Packet(int sequenceNum, boolean isAck, byte dataByte) {
        this.sequenceNum = sequenceNum;
        this.isAck = isAck;
        this.dataByte = dataByte;
    }

    // accessors
    public int getSequenceNum() {
        return sequenceNum;
    }

    public byte getDataByte() {
        return dataByte;
    }

    public boolean getIsAck() {
        return isAck;
    }

    // public factory methods
    public static Packet createAck(int sequenceNum) {
        return new Packet(sequenceNum, true, (byte)0);
    }

    public static Packet createDataPacket(int sequenceNum, byte c) {
        return new Packet(sequenceNum, false, c);
    }

    // deserialization
    public static Packet deserialize(byte[] bytes) {
        int sequenceNum = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, sequenceNumLength)).getInt();
        boolean isAck = false;
        if (bytes[sequenceNumLength] == -1) {
            isAck = true;
        }
        byte dataByte = bytes[sequenceNumLength + 1];
        return new Packet(sequenceNum, isAck, dataByte);
    }

    // serialization
    public byte[] serialize() {
        byte[] packetBytes = new byte[sequenceNumLength + 2];
        arraycopy(ByteBuffer.allocate(sequenceNumLength).putInt(sequenceNum).array(), 0, packetBytes, 0, sequenceNumLength);
        if (isAck) {
            packetBytes[sequenceNumLength] = -1;
        } else {
            packetBytes[sequenceNumLength] = 0;
        }
        packetBytes[sequenceNumLength + 1] = dataByte;
        return packetBytes;
    }
}
