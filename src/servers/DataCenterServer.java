package servers;

import utils.FileLogger;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import data.ClimateData;

public class DataCenterServer implements Runnable {
    private String id;
    private int port;
    private Database database;
    private MulticastPublisher multicastPublisher;
    private volatile boolean ativo;

    /**
     * Essa função server para informar ao loadBalancer que tem um servidor novo
     * @param ip
     * @param port
     * @throws IOException
     */
    public Socket connectToLoadBalancer(String load_balancer_ip, int load_balancer_port) throws IOException {
        Socket socket = new Socket(load_balancer_ip, load_balancer_port);
        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        output.writeObject("server-connect:" + id + ":" + port);
        output.flush();

        return socket;
    }

    public DataCenterServer(String id, int port, Database database, MulticastPublisher multicastPublisher) {
        this.id = id;
        this.port = port;
        this.database = database;
        this.multicastPublisher = multicastPublisher;
        this.ativo = true;
    }

    @Override
    public void run() {
        FileLogger.log("DataCenterServer", "Servidor " + id + " iniciado na porta: " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (ativo) {
                FileLogger.log("DataCenterServer", "Servidor " + id + " aguardando conexão do LoadBalancer...");
                try {
                    Socket clientSocket = serverSocket.accept();
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                    String message = (String) in.readObject();

                    FileLogger.log("mensagem do cliente: " + message + " recebida com sucesso");
                    
                    if (message.equals("get-data")) {
                        out.writeObject(database.getAllData());
                        out.flush();
                    
                        FileLogger.log("DataCenterServer", "Lista enviada para o cliente");
                        continue;
                    }

                    FileLogger.log("DataCenterServer", "Servidor " + id + ": Conexão recebida de "
                            + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                    // Espera-se que o LoadBalancer envie uma linha de dados do drone(talvez no
                    // futuro o mais correto seja esperar linhas se for usar stream?)
                    if (message != null) {
                        FileLogger.log("DataCenterServer", "Servidor " + id + " recebeu: " + message);
                        processDroneData(message);
                    }
                } catch (SocketException e) {
                    if (!ativo) {
                        FileLogger.log("DataCenterServer", "Servidor " + id + " parando (SocketException esperada).");
                    } else {
                        FileLogger.log("DataCenterServer", "Servidor " + id + ": SocketException: " + e.getMessage());
                    }
                } catch (IOException e) {
                    if (ativo) {
                        FileLogger.log("DataCenterServer",
                                "Servidor " + id + ": Erro de comunicação: " + e.getMessage());
                    }
                } catch (ClassNotFoundException e) {
                    if (ativo) {
                        FileLogger.log("DataCenterServer",
                                "Servidor " + id + ": Erro de comunicação: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if (ativo) {
                FileLogger.log("DataCenterServer",
                        "Servidor " + id + ": Erro ao iniciar o ServerSocket na porta " + port + ": " + e.getMessage());
            }
        }
        FileLogger.log("DataCenterServer", "Servidor " + id + " finalizado.");
    }

    private void processDroneData(String rawData) {
        ClimateData climateData = parseDroneData(rawData);
        if (climateData != null) {
            String dadosParaArmazenar = formatarParaArmazenamento(climateData);
            database.addData(dadosParaArmazenar);
            FileLogger.log("DataCenterServer", "Servidor " + id + " armazenou: " + dadosParaArmazenar);

            // Enviar para usuários via multicast
            if (multicastPublisher != null) {
                multicastPublisher.send(dadosParaArmazenar);
            }
        } else {
            FileLogger.log("DataCenterServer", "Servidor " + id + ": Falha ao analisar dados: " + rawData);
        }
    }

    private ClimateData parseDroneData(String dadosDrone) {
        String cleanData = dadosDrone;
        char delimitador = 0;

        if (dadosDrone.startsWith("(") && dadosDrone.endsWith(")")) {
            delimitador = ';';
            cleanData = dadosDrone.substring(1, dadosDrone.length() - 1);
        } else if (dadosDrone.startsWith("{") && dadosDrone.endsWith("}")) {
            delimitador = ',';
            cleanData = dadosDrone.substring(1, dadosDrone.length() - 1);
        } else if (dadosDrone.contains("-")) {
            delimitador = '-';
        } else if (dadosDrone.contains("#")) {
            delimitador = '#';
        } else {
            FileLogger.log("DataCenterServer",
                    "Servidor " + id + ": Formato de drone desconhecido ou dados corrompidos: " + dadosDrone);
            return null;
        }

        String[] partes = cleanData.split(String.valueOf(delimitador));
        if (partes.length == 4) {
            try {
                // Ordem de coleta: pressão, radiação, temperatura, umidade
                double pressao = Double.parseDouble(partes[0].trim());
                double radiacao = Double.parseDouble(partes[1].trim());
                double temperatura = Double.parseDouble(partes[2].trim());
                double umidade = Double.parseDouble(partes[3].trim());
                return new ClimateData(temperatura, umidade, pressao, radiacao);
            } catch (NumberFormatException e) {
                FileLogger.log("DataCenterServer", "Servidor " + id + ": Erro ao converter dados numéricos do drone: '"
                        + cleanData + "'. Erro: " + e.getMessage());
                return null;
            }
        } else {
            FileLogger.log("DataCenterServer",
                    "Servidor " + id + ": Número incorreto de partes após split com delimitador '" + delimitador
                            + "' em '" + cleanData + "'. Partes: " + partes.length);
        }
        return null;
    }

    private String formatarParaArmazenamento(ClimateData data) {

        return String.format("[%s//%s//%s//%s]",
                String.format("%.2f", data.temperatura()).replace(",", "."), // Formatando para 2 casas decimais e
                                                                             // usando ponto
                String.format("%.2f", data.umidade()).replace(",", "."),
                String.format("%.2f", data.pressao()).replace(",", "."),
                String.format("%.2f", data.radiacao()).replace(",", "."));
    }

    public void parar() {
        this.ativo = false;
        try {
            new Socket("localhost", this.port).close();
        } catch (IOException e) {
        }
        FileLogger.log("DataCenterServer", "Servidor " + id + " sinalizado para parar.");
    }
}