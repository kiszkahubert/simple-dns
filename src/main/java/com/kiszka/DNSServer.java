package com.kiszka;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class DNSServer {
    private static final int SERVER_SOCKET = 2053;
    private static final int BYTE_BUFFER = 512;
    public static void main(String[] args) {
        try(DatagramSocket serverSocket = new DatagramSocket(SERVER_SOCKET)){
            for(;;){
                final byte[] buffer = new byte[BYTE_BUFFER];
                final DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
                serverSocket.receive(packet);
                String receivedMessage = new String(packet.getData(),0,packet.getLength());
                System.out.println(receivedMessage);
                DNSHeader header = DNSHeader.deserializeFromByteArray(Arrays.copyOf(buffer,12));
                DNSQuestion question = DNSQuestion.deserializeFromByteArray(Arrays.copyOfRange(buffer,12,buffer.length));
                DNSAnswer answer = DNSAnswer.answer(question);
                ByteBuffer result = ByteBuffer.allocate(512);
                result.put(header.response().serializeToByteArray());
                result.put(question.serializeToByteArray());
                result.put(answer.serializeToByteArray());
                final DatagramPacket packetResponse = new DatagramPacket(
                        result.array(), result.array().length,packet.getSocketAddress()
                );
                serverSocket.send(packetResponse);
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
record DNSHeader(int id, short flags, int qdCount, int anCount, int nsCount, int arCount){
    private static final int QR_BIT = 15;
    private static final int OPCODE_SHIFT = 1;
    private static final int AA_BIT = 10;
    private static final int TC_BIT = 9;
    private static final int RD_BIT = 8;
    private static final int RA_BIT = 7;
    private static final int Z_SHIFT = 4;
    private static final int RCODE_SHIFT = 0;
    public DNSHeader{
        if (id < 0 || id > 0xFFFF){
            throw new IllegalArgumentException("ID must be a 16-bit value");
        }
        if (qdCount < 0 || qdCount > 0xFFFF){
            throw new IllegalArgumentException("qdCount must be a 16-bit value");
        }
        if (anCount < 0 || anCount > 0xFFFF){
            throw new IllegalArgumentException("anCount must be a 16-bit value");
        }
        if (nsCount < 0 || nsCount > 0xFFFF){
            throw new IllegalArgumentException("nsCount must be a 16-bit value");
        }
        if (arCount < 0 || arCount > 0xFFFF){
            throw new IllegalArgumentException("arCount must be a 16-bit value");
        }
    }
    public DNSHeader response(){
        return new DNSHeader(id,(short) (flags | (1 << QR_BIT) | (4 & 0x0F) << RCODE_SHIFT),1,1,0,0); // ensures correct RCODE according to received OPCODE
    }
    public static short buildFlags(boolean qr, int opcode,boolean aa, boolean tc, boolean rd, boolean ra, int z, int rcode){
        int flag = 0;
        flag |= (qr ? 1 : 0) << QR_BIT;
        flag |= (opcode & 0x0F) << OPCODE_SHIFT;
        flag |= (aa ? 1 : 0) << AA_BIT;
        flag |= (tc ? 1 : 0) << TC_BIT;
        flag |= (rd ? 1 : 0) << RD_BIT;
        flag |= (ra ? 1 : 0) << RA_BIT;
        flag |= (z & 0x07) << Z_SHIFT;
        flag |= (rcode & 0x0F) << RCODE_SHIFT;
        return (short) flag;
    }
    public byte[] serializeToByteArray(){
        var buffer = ByteBuffer.allocate(12);
        buffer.putShort((short) id);
        buffer.putShort(flags);
        buffer.putShort((short) qdCount);
        buffer.putShort((short) anCount);
        buffer.putShort((short) nsCount);
        buffer.putShort((short) arCount);
        return buffer.array();
    }
    public static DNSHeader deserializeFromByteArray(byte[] data){
        if(data.length != 12){
            throw new IllegalArgumentException("Array must be 12 bytes long");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int id = Short.toUnsignedInt(buffer.getShort());
        short flags = buffer.getShort();
        int qdCount = Short.toUnsignedInt(buffer.getShort());
        int anCount = Short.toUnsignedInt(buffer.getShort());
        int nsCount = Short.toUnsignedInt(buffer.getShort());
        int arCount = Short.toUnsignedInt(buffer.getShort());
        return new DNSHeader(id,flags,qdCount,anCount,nsCount,arCount);
    }
}
record DNSQuestion(short type, short qClass, String name){
    public byte[] serializeToByteArray(){
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[nameBytes.length + 6];
        int i = 0;
        for (String part : name.split("\\.")){
            bytes[i++] = (byte) part.length();
            for(char c : part.toCharArray()){
                bytes[i++] = (byte) c;
            }
        }
        bytes[i++] = 0;
        bytes[i++] = (byte)(type >> 8);
        bytes[i++] = (byte)type;
        bytes[i++] = (byte)(qClass >> 8);
        bytes[i] = (byte)qClass;
        return bytes;
    }
    public static DNSQuestion deserializeFromByteArray(byte[] bytes){
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while(bytes[i] != 0){
            if(i != 0){
                builder.append(".");
            }
            int length = bytes[i++];
            for (int j = 0; j < length; j++) {
                builder.append((char) bytes[i++]);
            }
        }
        short type = (short) ((bytes[i+1] << 8) | (bytes[i+2] & 0xFF));
        short qClass = (short) ((bytes[i+3] << 8) | (bytes[i+4] & 0xFF));
        return new DNSQuestion(type,qClass,builder.toString());
    }
}
record DNSAnswer(String label, short type, short qClass, int ttl, short rdLength, byte[] rData){
    public DNSAnswer{
        if(label.length() > 63){
            throw new IllegalArgumentException("Label max size is 63 bytes");
        }
        if(rdLength < 0 || rdLength > 0xFFFF){
            throw new IllegalArgumentException("rdLength capped at 2 bytes");
        }
        if(rData.length != rdLength){
            throw new IllegalArgumentException("RDLENGTH must match RDATA");
        }
    }
    public static DNSAnswer answer(DNSQuestion question){
        return new DNSAnswer(question.name(), (short)1,(short)1,30,(short)4, new byte[]{8,8,8,8});
    }
    public byte[] serializeToByteArray(){
        ByteBuffer buffer = ByteBuffer.allocate(10 + label.length() + rData().length); //2+2+4+2
        for (String part : label.split("\\.")){
            buffer.put((byte)part.length());
            for(char c : part.toCharArray()){
                buffer.put((byte) c);
            }
        }
        buffer.put((byte)0);
        buffer.putShort(type);
        buffer.putShort(qClass);
        buffer.putInt(ttl);
        buffer.putShort(rdLength);
        buffer.put(rData);
        return buffer.array();
    }
}
