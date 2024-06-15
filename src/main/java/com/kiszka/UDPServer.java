package com.kiszka;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UDPServer {
    private static final int SERVER_SOCKET = 2053;
    private static final int BYTE_BUFFER = 1024;
    public static void main(String[] args) {
        try(DatagramSocket serverSocket = new DatagramSocket(SERVER_SOCKET)){
            byte[] receiveData = new byte[1024];
            for(;;){
                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                serverSocket.receive(receivePacket);
                String receivedMessage = new String(receivePacket.getData(),0,receivePacket.getLength());
                System.out.println(receivedMessage);
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                byte[] sendData = receivedMessage.getBytes(StandardCharsets.UTF_8);
                DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,clientAddress,clientPort);
                serverSocket.send(sendPacket);
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
