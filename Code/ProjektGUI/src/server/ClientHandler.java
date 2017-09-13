package server;

import guiproject.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader input;
    private ArrayList<Node> recordedClicks;
    public boolean running = true;

    private Thread clientListener = new Thread() {
        public void run() {
            try {
                while (true) {
                    String response = input.readLine();
                    if (response != null && !response.equals("check")) {
                        // save recorded clicks
                        String[] data = response.split(" ");
                        Node node = new Node(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Integer.parseInt(data[2]));
                        recordedClicks.add(node);
                    }
                }
            } catch (SocketException e) {
                System.out.println("Zamykam socket clientlistenera");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    ClientHandler(Socket s) {
        socket = s;
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

    public ArrayList<Node> getRecordedClicks() {
        return recordedClicks;
    }

    public void run() {
        try {
            clientListener.start();
            while (running) {
                Thread.sleep(0);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (!socket.isClosed()) {
                try {
                    System.out.println("zamykam socket klienta");
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}