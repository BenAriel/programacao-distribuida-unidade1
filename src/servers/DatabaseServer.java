package servers;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DatabaseServer {
    private static final int PORT = 9100;
    private final List<String> storedData = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        new DatabaseServer().start();
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[DatabaseServer] Servidor de banco de dados iniciado na porta " + PORT);
        while (true) {
            Socket client = serverSocket.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    private void handleClient(Socket client) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("ADD:")) {
                    String record = line.substring(4);
                    storedData.add(record);
                    out.println("OK");
                } else if (line.equals("GETALL")) {
                    for (String record : storedData) {
                        out.println(record);
                    }
                    out.println("END");
                } else if (line.equals("GETLAST")) {
                    if (storedData.isEmpty()) {
                        out.println("NONE");
                    } else {
                        out.println(storedData.get(storedData.size() - 1));
                    }
                } else if (line.equals("QUIT")) {
                    break;
                } else {
                    out.println("ERROR: Comando desconhecido");
                }
            }
        } catch (IOException e) {
            System.err.println("[DatabaseServer] Erro ao lidar com cliente: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }
} 