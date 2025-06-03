package client;

import data.ClimateData;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ClientInterface {

    public static void main(String[] args) {
        boolean running = true;

        Scanner scanner = new Scanner(System.in);
        Client client = new Client();

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
                            System.out.println("\n⚠️  Nenhum dado recebido do servidor.\n");
                        } else {
                            System.out.println("\n📊 Dados Climáticos Recebidos:");
                            System.out.println("──────────────────────────────────────────");
                            int i = 1;
                            for (ClimateData d : dados) {
                                System.out.printf("🔹 Dado #%d\n", i++);
                                System.out.printf("   🌡️  Temperatura: %.2f °C\n", d.temperatura());
                                System.out.printf("   💧 Umidade: %.2f %%\n", d.umidade());
                                System.out.printf("   📈 Pressão: %.2f hPa\n", d.pressao());
                                System.out.printf("   ☀️  Radiação: %.2f W/m²\n", d.radiacao());
                                System.out.println("──────────────────────────────────────────");
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("❌ Erro ao requisitar dados: " + e.getMessage());
                    }
                    break;
                case "0":
                    System.out.println("Saindo... ☁️");
                    running = false;
                default:
                    System.out.println("❗ Opção inválida. Tente novamente.");
            }
        }

        scanner.close();
    }
}

