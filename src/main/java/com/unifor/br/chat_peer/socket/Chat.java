package com.unifor.br.chat_peer.socket;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Chat {

    private String userName;
    private int port;
    private ServerSocket serverSocket;

    private List<Socket> connections = new CopyOnWriteArrayList<>();
    private List<String> history = new CopyOnWriteArrayList<>();
    private Set<String> knownPeers = ConcurrentHashMap.newKeySet();

    private static final String DISCOVERY_ADDRESS = "230.0.0.0";
    private static final int DISCOVERY_PORT = 4446;

    public Chat(String userName, int port) throws IOException {
        this.userName = userName;
        this.port = port;
        this.serverSocket = new ServerSocket(port);
        System.out.println("O Peer " + userName + " está ouvindo na porta: " + port);
    }

    public void start() {
        new Thread(this::listenForConnections).start();
        new Thread(this::listenForUserInput).start();
        new Thread(this::listenForDiscovery).start();
        new Thread(this::announcePresence).start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void listenForUserInput() {
        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String mensagem = console.readLine();

                if (mensagem.equalsIgnoreCase("/sair")) {
                    shutdown();
                    break;
                }

                if (mensagem.equalsIgnoreCase("/historico")) {
                    System.out.println("=== HISTÓRICO ===");
                    for (String msg : history) {
                        System.out.println(msg);
                    }
                    continue;
                }

                String fullMessage = userName + ": " + mensagem;
                history.add(fullMessage);
                broadcastMessage(fullMessage);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    private void broadcastMessage(String mensagem) {
        for (Socket socket : connections) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(mensagem);
            } catch (IOException e) {
                connections.remove(socket);
            }
        }
    }

    private void listenForConnections() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleConnection(socket)).start();
            } catch (IOException e) {
                break;
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // handshake
            out.println("HELLO:" + port);
            String hello = in.readLine();

            if (!hello.startsWith("HELLO:")) return;

            int peerPort = Integer.parseInt(hello.split(":")[1]);
            String peerHost = socket.getInetAddress().getHostAddress();
            String key = peerHost + ":" + peerPort;

            if (knownPeers.contains(key)) {
                socket.close();
                return;
            }

            knownPeers.add(key);
            connections.add(socket);
            System.out.println("Conectado a peer " + key);

            String mensagem;
            while ((mensagem = in.readLine()) != null) {
                if (!mensagem.startsWith(userName + ":")) {
                    history.add(mensagem);
                    System.out.println(mensagem);
                }
            }

        } catch (IOException e) {
            connections.remove(socket);
        }
    }

    private void connectToPeer(String host, int port) {
        String key = host + ":" + port;
        if (knownPeers.contains(key) || port == this.port) return;

        try {
            Socket socket = new Socket(host, port);
            new Thread(() -> handleConnection(socket)).start();
        } catch (IOException ignored) {}
    }

    // -------- DESCOBERTA AUTOMÁTICA --------

    private void announcePresence() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(DISCOVERY_ADDRESS);

            while (true) {
                String msg = "DISCOVER:" + port;
                byte[] buffer = msg.getBytes();
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length, group, DISCOVERY_PORT);
                socket.send(packet);
                Thread.sleep(5000);
            }

        } catch (Exception ignored) {}
    }

    private void listenForDiscovery() {
        try (MulticastSocket socket = new MulticastSocket(DISCOVERY_PORT)) {
            InetAddress group = InetAddress.getByName(DISCOVERY_ADDRESS);
            socket.joinGroup(group);

            byte[] buffer = new byte[256];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());

                if (msg.startsWith("DISCOVER:")) {
                    int peerPort = Integer.parseInt(msg.split(":")[1]);
                    String peerHost = packet.getAddress().getHostAddress();

                    if (peerPort != this.port) {
                        connectToPeer(peerHost, peerPort);
                    }
                }
            }

        } catch (IOException ignored) {}
    }

    private void shutdown() {
        System.out.println("Encerrando conexões...");
        try {
            for (Socket socket : connections) socket.close();
            serverSocket.close();
        } catch (IOException ignored) {}
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Digite o nome do usuario: ");
        String userName = console.readLine();

        System.out.print("Digite a porta para escutar: ");
        int port = Integer.parseInt(console.readLine());

        Chat peer = new Chat(userName, port);
        peer.start();

        System.out.println("Comandos:");
        System.out.println("/historico → ver histórico");
        System.out.println("/sair → sair");
    }
}