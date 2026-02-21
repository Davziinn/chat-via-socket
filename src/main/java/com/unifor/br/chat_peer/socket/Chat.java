package com.unifor.br.chat_peer.socket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Chat {

    private String userName;
    private ServerSocket serverSocket;
    private List<Socket> connections = new CopyOnWriteArrayList<>();
    private List<String> history = new CopyOnWriteArrayList<>();
    private BufferedReader console;

    public Chat(String userName, int port, BufferedReader console) {
        this.userName = userName;
        this.console = console;
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("O Peer " + userName + " está ouvindo na porta: " + port);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao abrir a porta", e);
        }
    }

    public void start() {
        new Thread(this::listenForConnections).start();
        new Thread(this::listenForUserInput).start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void listenForUserInput() {
        try {
            while (true) {
                String mensagem = console.readLine();

                if (mensagem == null) continue;

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
                connections.add(socket);
                new Thread(() -> handleConnection(socket)).start();
            } catch (IOException e) {
                break;
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String mensagem;
            while ((mensagem = in.readLine()) != null) {
                history.add(mensagem);
                System.out.println(mensagem);
            }
        } catch (IOException e) {
            connections.remove(socket);
        }
    }

    public void connectToPeer(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            connections.add(socket);
            new Thread(() -> handleConnection(socket)).start();
            System.out.println("Conectado a um peer em: " + host + ":" + port);
        } catch (IOException e) {
            System.out.println("Erro ao conectar ao peer.");
        }
    }

    private void shutdown() {
        System.out.println("Encerrando conexões...");
        try {
            for (Socket socket : connections) {
                socket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Digite o nome do usuario: ");
        String userName = console.readLine();

        System.out.print("Digite a porta para escutar: ");
        int port = Integer.parseInt(console.readLine());

        Chat peer = new Chat(userName, port, console);

        System.out.print("Deseja conectar a outro peer? (s/n): ");
        String resposta = console.readLine();

        if (resposta.equalsIgnoreCase("s")) {
            System.out.print("Digite endereço do outro host: ");
            String peerHost = console.readLine();

            System.out.print("Digite a porta do outro peer: ");
            int peerPort = Integer.parseInt(console.readLine());

            peer.connectToPeer(peerHost, peerPort);
        }

        peer.start();

        System.out.println("Comandos:");
        System.out.println("/historico → ver histórico");
        System.out.println("/sair → sair");
    }
}