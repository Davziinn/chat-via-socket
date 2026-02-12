package com.unifor.br.chat_peer.socket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Component
public class SocketClient implements CommandLineRunner {

    @Value("${chat.mode}")
    private String mode;

    @Value("${chat.server.host}")
    private String host;

    @Value("${chat.server.port}")
    private int port;

    @Override
    public void run(String... args) throws Exception {
        if (!mode.equals("client")) {
            return;
        }

        System.out.println("Conectando ao servidor...");

        Socket socket = new Socket(host, port);
        System.out.println("Conectado ao servidor!");

        BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

        BufferedReader teclado = new BufferedReader(
                new InputStreamReader(System.in)
        );

        String mensagem;

        while (true) {
            if (entrada.ready()) {
                mensagem = entrada.readLine();
                System.out.println("Servidor: " + mensagem);
            }

            if (teclado.ready()) {
                mensagem = teclado.readLine();
                saida.println(mensagem);
            }
        }
    }
}
