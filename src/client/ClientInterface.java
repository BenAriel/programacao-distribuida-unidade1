package client;

import data.ClimateData;

import java.io.IOException;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// import java.awt.*;

/**
 * Essa classe serve para testar as funcionalidades do cliente
 */
public class ClientInterface {

    public static void main(String[] args) throws IOException {    
        boolean running = true;

        Scanner scanner = new Scanner(System.in);
        Client client = new Client();

        ExecutorService broadcastListener = Executors.newSingleThreadExecutor();
        
        broadcastListener.submit(() -> {
            try {
                MulticastSocket group = client.joinGroup("230.0.0.1", 55554);

                while (true) {
                    String groupMessage = client.waitMessage(group);


                    //a gente deixa esse print? ou só apenas quando digitar 1?
                    System.out.println("Mensagem do servidor: " + groupMessage);

                    // Essa função faz um beep no lugar de printar a mensagem na tela(mas irritante)
                    // Toolkit.getDefaultToolkit().beep();            
                }
            } catch (IOException e) {
                System.out.println("Erro ao se conectar ao grupo multicast!");
            }
        });

        while (running) {
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║        SISTEMA DE DADOS CLIMÁTICOS     ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ 1. Requisitar dados climáticos         ║");
            System.out.println("║ 0. Sair                                ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.print("Escolha uma opção: ");
            String option = scanner.nextLine();

            switch (option) {
                case "1":
                    try {
                        List<ClimateData> dados = client.requestData();
                        if (dados.isEmpty()) {
                            System.out.println("\nNenhum dado recebido do servidor.\n");
                        } else {
                            System.out.println("\nDados Climáticos Recebidos:");
                            System.out.println("──────────────────────────────────────────");
                            int i = 1;
                            for (ClimateData d : dados) {
                                String origem = "?";
                                try {
                                    origem = (String) d.getClass().getMethod("origem").invoke(d);
                                } catch (Exception e) {
                                    
                                }
                                System.out.printf(" Dado #%d (Drone: %s)\n", i++, origem);
                                System.out.printf(" Temperatura: %.2f °C\n", d.temperatura());
                                System.out.printf(" Umidade: %.2f %%\n", d.umidade());
                                System.out.printf(" Pressão: %.2f hPa\n", d.pressao());
                                System.out.printf(" Radiação: %.2f W/m²\n", d.radiacao());
                                System.out.println("──────────────────────────────────────────");
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("Erro ao requisitar dados: " + e.getMessage());

                        e.printStackTrace();
                    }
                    break;
                case "0":
                    System.out.println("Saindo...");
                    running = false;

                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }

        broadcastListener.shutdownNow();
        scanner.close();
    }
}

