package com.akshaymathur.design.tcpserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TCPServer {
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final Server server;

    public TCPServer(Server server) throws IOException {
        serverSocket = new ServerSocket(9999);
        executorService = Executors.newFixedThreadPool(5);
        this.server = server;
    }

    public class Action implements Runnable {
        private Socket client;

        public Action(Socket client) {
            this.client = client;
        }

        public void run() {
            try (Socket client = this.client;
                    var inputStream = new DataInputStream(client.getInputStream());
                    var outputStream = new DataOutputStream(client.getOutputStream());) {
                System.out.println("Connection Established with client :" + client.getInetAddress());
                String input = inputStream.readLine();
                String output = "null";
                try {
                    output = server.callMethod(input);
                } catch (RuntimeException e) {
                    System.out.println("A runtime exception occured while calling the function. ");
                    System.err.println(e);
                }
                outputStream.writeUTF(output);
                outputStream.flush();
            } catch (IOException e) {
                System.out.println("ERROR OCCURED");
                System.err.println(e);
                return;
            }
            System.out.println("connection closed with client!");
        }
    }

    public void start() {
        try (serverSocket) {
            System.out.println("Server ready to start accepting connections.");
            while (true) {
                Socket client = serverSocket.accept();
                executorService.submit(new Action(client));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
    }
}
