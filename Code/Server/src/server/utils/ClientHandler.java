package server.utils;

import javafx.application.Platform;
import server.model.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class ClientHandler extends Thread {

    private Socket socket;

    private BufferedReader input;

    private ArrayList<Node> recordedClicks;

    private String IP = "";

    public boolean running = true;

    private OutputConsole outputConsole;

    private Thread clientListener = new Thread() {
        public void run() {
            try {
                while (true) {
                    String response = input.readLine();
                    if (response != null && !response.equals("check")) {
                        // save recorded clicks
                        String[] data = response.split(" ");
                        //by default, our click is not a double click
                        Node node = new Node(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Long.parseLong(data[2]), false);
                        //Long.parseLong(data[2]) contains click delay
                        if (Long.parseLong(data[2]) > 600)
                            recordedClicks.add(node);
                        else {
                            Node doubleClickNode = recordedClicks.remove(recordedClicks.size() - 1);
                            doubleClickNode.setIsDouble(true);
                            recordedClicks.add(doubleClickNode);
                        }
                    }
                }
            } catch (SocketException e) {
                running = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    ClientHandler(Socket s, OutputConsole console) {
        socket = s;
        outputConsole = console;
        recordedClicks = new ArrayList<>();
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public ArrayList<Node> getRecordedClicks() {
        return recordedClicks;
    }

    public void run() {
        try {
            clientListener.start();
            while (running) {
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (!socket.isClosed()) {
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("close");
                    Platform.runLater(() -> {
                        outputConsole.writeLine("Klient o IP " + IP + " właśnie się rozłączył");
                    });
                    socket.close();
                    Server.connectedClientsMap.remove(IP);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}