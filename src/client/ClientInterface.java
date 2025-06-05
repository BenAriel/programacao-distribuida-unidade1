package client;

import data.ClimateData;

import java.io.IOException;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientInterface {

    public static void main(String[] args) throws IOException {
        boolean running = true;

        Scanner scanner = new Scanner(System.in);
        Client client = new Client();

        List<String> mensagensMulticast = Collections.synchronizedList(new ArrayList<>());

        ExecutorService broadcastListener = Executors.newSingleThreadExecutor();

        broadcastListener.submit(() -> {
            try {
                MulticastSocket group = client.joinGroup("230.0.0.1", 55554);

                while (true) {
                    String groupMessage = client.waitMessage(group);
                    mensagensMulticast.add(groupMessage);
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
            System.out.println("║ 2. Ver mensagens multicast             ║");
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
                                    // Ignorar falha ao obter origem
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
                case "2":
                    if (mensagensMulticast.isEmpty()) {
                        System.out.println("\nNenhuma mensagem multicast recebida ainda.\n");
                    } else {
                        System.out.println("\nMensagens Multicast Recebidas:");
                        System.out.println("──────────────────────────────────────────");
                        int i = 1;
                        synchronized (mensagensMulticast) {
                            for (String msg : mensagensMulticast) {
                                System.out.printf("Mensagem #%d: %s\n", i++, msg);
                            }
                        }
                        System.out.println("──────────────────────────────────────────\n");
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
