package com.unifor.br.chat_peer.socket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class SocketServer implements CommandLineRunner {

    @Value("${chat.mode}")
    private String mode;

    @Value("${chat.server.port}")
    private int port;


    @Override
    public void run(String... args) throws Exception {
        if (!mode.equals("server")) {
            return;
        }

        System.out.println("Iniciando servidor...");

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor aguardando conexão na porta " + port + "...");

        Socket socket = serverSocket.accept();
        System.out.println("Cliente conectado com sucesso!");

        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));


        PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        String mensagem;

        while (true) {
            if (entrada.ready()) {
                mensagem = entrada.readLine();

                System.out.println("Cliente: " +  mensagem);
            }

            if (teclado.ready()) {
                mensagem = teclado.readLine();
                saida.println(mensagem);
            }
        }
    }
}
