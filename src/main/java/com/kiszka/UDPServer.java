package com.kiszka;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class UDPServer {
    private static final int SERVER_SOCKET = 2053;
    private static final int BYTE_BUFFER = 512;
    public static void main(String[] args) {
        try(DatagramSocket serverSocket = new DatagramSocket(SERVER_SOCKET)){
            for(;;){
                final byte[] receiveData = new byte[BYTE_BUFFER];
                final DatagramPacket packet = new DatagramPacket(receiveData,receiveData.length);
                serverSocket.receive(packet);
                String receivedMessage = new String(packet.getData(),0,packet.getLength());
                System.out.println(receivedMessage);
                DNSHeader header = DNSHeader.expectedHeader();
                final DatagramPacket packetResponse = new DatagramPacket(
                        header.serializeToByteArray(),header.serializeToByteArray().length,packet.getSocketAddress()
                );
                serverSocket.send(packetResponse);
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
record DNSHeader(int id, short flags, int qdcount, int ancount, int nscount, int arcount){
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
        if (qdcount < 0 || qdcount > 0xFFFF){
            throw new IllegalArgumentException("qdcount must be a 16-bit value");
        }
        if (ancount < 0 || ancount > 0xFFFF){
            throw new IllegalArgumentException("ancount must be a 16-bit value");
        }
        if (nscount < 0 || nscount > 0xFFFF){
            throw new IllegalArgumentException("nscount must be a 16-bit value");
        }
        if (arcount < 0 || arcount > 0xFFFF){
            throw new IllegalArgumentException("arcount must be a 16-bit value");
        }
    }
    public static DNSHeader expectedHeader(){
        short flags = buildFlags(true,0,false,false,false,true,0,0);
        return new DNSHeader(1234,flags,0,0,0,0);
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
        buffer.putShort((short) qdcount);
        buffer.putShort((short) ancount);
        buffer.putShort((short) nscount);
        buffer.putShort((short) arcount);
        return buffer.array();
    }
}
