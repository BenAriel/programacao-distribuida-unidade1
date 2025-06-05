package servers;

import utils.FileLogger;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;

import data.ClimateData;

public class DataCenterServer implements Runnable {
    private String id;
    private int port;
    private MulticastPublisher multicastPublisher;
    private volatile boolean ativo;
    private static final String DATABASE_HOST = "localhost";
    private static final int DATABASE_PORT = 9100;

    /**
     * Essa função server para informar ao loadBalancer que tem um servidor novo
     * @param ip
     * @param port
     * @throws IOException
     */
    public Socket connectToLoadBalancer(String load_balancer_ip, int load_balancer_port) throws IOException {
        Socket socket = new Socket(load_balancer_ip, load_balancer_port);
        ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
        output.writeObject("server-connect:" + "localhost" + ":" + port);
        output.flush();

        return socket;
    }

    public DataCenterServer(String id, int port, MulticastPublisher multicastPublisher) {
        this.id = id;
        this.port = port;
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

                  
                    clientSocket.setSoTimeout(100);
                    ObjectInputStream inObj = null;
                    BufferedReader inText = null;
                    ObjectOutputStream outObj = null;
                    boolean isClient = false;
                    String message = null;
                    try {
                        inObj = new ObjectInputStream(clientSocket.getInputStream());
                        outObj = new ObjectOutputStream(clientSocket.getOutputStream());
                        Object obj = inObj.readObject();
                        if (obj instanceof String) {
                            message = (String) obj;
                            isClient = true;
                        }
                    } catch (Exception e) {
                        
                    }
                    if (!isClient) {
                        try {
                            inText = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            message = inText.readLine();
                        } catch (Exception e) {
                            FileLogger.log("DataCenterServer", "Erro ao ler mensagem de drone: " + e.getMessage());
                        }
                    }
                    clientSocket.setSoTimeout(0); // remove timeout

                    FileLogger.log("mensagem do cliente: " + message + " recebida com sucesso");

                    if (isClient && message.equals("get-data")) {
                        List<String> allData = getAllDataFromDatabase();
                        outObj.writeObject(allData);
                        outObj.flush();
                        FileLogger.log("DataCenterServer", "Lista enviada para o cliente");
                        continue;
                    }

                    FileLogger.log("DataCenterServer", "Servidor " + id + ": Conexão recebida de "
                            + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                    // Espera-se que o LoadBalancer envie uma linha de dados do drone
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
        String[] split = rawData.split("//", 2);
        if (split.length != 2) {
            FileLogger.log("DataCenterServer", "Servidor " + id + ": Formato inválido de dados recebidos: " + rawData);
            return;
        }
        String origemDrone = split[0];
        String dadosDrone = split[1];
        ClimateData climateData = parseDroneData(dadosDrone);
        if (climateData != null) {
            String dadosParaArmazenar = formatarParaArmazenamento(climateData, origemDrone);
            addDataToDatabase(dadosParaArmazenar);
            FileLogger.log("DataCenterServer", "Servidor " + id + " armazenou: " + dadosParaArmazenar);

            // Enviar para usuários via multicast
            if (multicastPublisher != null) {
                multicastPublisher.send(dadosParaArmazenar);
            }
        } else {
            FileLogger.log("DataCenterServer", "Servidor " + id + ": Falha ao analisar dados: " + rawData);
        }
    }

    private void addDataToDatabase(String record) {
        try (Socket dbSocket = new Socket(DATABASE_HOST, DATABASE_PORT);
             PrintWriter out = new PrintWriter(dbSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(dbSocket.getInputStream()))) {
            out.println("ADD:" + record);
            in.readLine(); // Espera resposta OK
        } catch (IOException e) {
            FileLogger.log("DataCenterServer", "Erro ao adicionar dado ao DatabaseServer: " + e.getMessage());
        }
    }

    private List<String> getAllDataFromDatabase() {
        List<String> result = new ArrayList<>();
        try (Socket dbSocket = new Socket(DATABASE_HOST, DATABASE_PORT);
             PrintWriter out = new PrintWriter(dbSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(dbSocket.getInputStream()))) {
            out.println("GETALL");
            String line = in.readLine();
            while (line != null && !line.equals("END")) {
                result.add(line);

                line = in.readLine();
            }
        } catch (IOException e) {
            FileLogger.log("DataCenterServer", "Erro ao buscar dados do DatabaseServer: " + e.getMessage());
        }
        return result;
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

    private String formatarParaArmazenamento(ClimateData data, String origemDrone) {
        return String.format("[%s//%s//%s//%s//%s]",
                origemDrone,
                String.format("%.2f", data.temperatura()).replace(",", "."),
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