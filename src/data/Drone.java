package data;

import utils.FileLogger;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

//runnable ou callable? acho que não precisa ser callable, pois não tem retorno
public class Drone implements Runnable {
    private String cardealidade;
    private char delimitador;
    private int intervaloEnvioMinSegundos; // 2 sec
    private int intervaloEnvioMaxSegundos; // 5 sec
    private volatile boolean ativo; // 'volatile' é importante para visibilidade entre threads

    public Drone(String cardealidade, char delimitador, int minIntervalo, int maxIntervalo) {
        this.cardealidade = cardealidade;
        this.delimitador = delimitador;
        this.intervaloEnvioMinSegundos = minIntervalo;
        this.intervaloEnvioMaxSegundos = maxIntervalo;
        this.ativo = true;
    }

    private ClimateData coletarDadosClimaticos() {

        double temperatura = Math.random() * 25 + 15; // 15 a 40 graus Celsius
        double umidade = Math.random() * 60 + 30; // 30 a 90% de umidade
        double pressao = Math.random() * 70 + 980; // 980 a 1050 hPa
        double radiacao = Math.random() * 1000; // 0 a 1000 W/m²
        return new ClimateData(temperatura, umidade, pressao, radiacao);
    }

    private String formatarDados(ClimateData dados) {

        StringBuilder sb = new StringBuilder();
        if (this.cardealidade.equals("Sul"))
            sb.append("(");
        if (this.cardealidade.equals("Leste"))
            sb.append("{");

        sb.append(dados.pressao()).append(this.delimitador)
                .append(dados.radiacao()).append(this.delimitador)
                .append(dados.temperatura()).append(this.delimitador)
                .append(dados.umidade());

        if (this.cardealidade.equals("Sul"))
            sb.append(")");
        if (this.cardealidade.equals("Leste"))
            sb.append("}");

        return sb.toString();
    }

    private void enviarDados(String dadosFormatados) {
        FileLogger.log("Drone", "Drone " + this.cardealidade + " enviando: " + dadosFormatados);
        String loadBalancerHost = "localhost";
        int loadBalancerPort = 8000;

        try (Socket socket = new Socket(loadBalancerHost, loadBalancerPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(this.cardealidade + "//" + dadosFormatados);

        } catch (IOException e) {
            FileLogger.log("Drone",
                    "Drone " + this.cardealidade + ": Erro ao enviar dados para Load Balancer: " + e.getMessage());

            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        FileLogger.log("Drone", "Drone " + cardealidade + " iniciado.");
        while (ativo) {
            try {
                ClimateData dadosColetados = coletarDadosClimaticos();
                String dadosFormatados = formatarDados(dadosColetados);
                enviarDados(dadosFormatados);
                long intervalo = (long) (Math.random() * (intervaloEnvioMaxSegundos - intervaloEnvioMinSegundos + 1)
                        + intervaloEnvioMinSegundos) * 1000;
                Thread.sleep(intervalo);
            } catch (InterruptedException e) {
                FileLogger.log("Drone", "Drone " + cardealidade + " interrompido.");
                this.ativo = false;
                Thread.currentThread().interrupt();
            }
        }
        FileLogger.log("Drone", "Drone " + cardealidade + " finalizado.");
    }

    public void parar() {
        this.ativo = false;
    }
}
