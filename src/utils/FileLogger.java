package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLogger {
    private static final String DEFAULT_LOG_FILE = "simulation_log.log";
    private static PrintWriter writer;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static boolean initialized = false;
    static {
        initialize(DEFAULT_LOG_FILE);
    }

    private FileLogger() {
    }

    public static synchronized void initialize(String logFilePath) {
        if (initialized) {
            // Se já estiver inicializado (possivelmente com um arquivo diferente), feche o
            // antigo.
            if (writer != null) {
                writer.close();
            }
        }
        try {

            FileWriter fw = new FileWriter(logFilePath, true);
            writer = new PrintWriter(fw, true);
            initialized = true;

            String initMessage = dateFormat.format(new Date()) + " - [FileLogger] Logger inicializado. Registrando em: "
                    + logFilePath;
            System.out.println(initMessage);
            if (writer != null) {
                writer.println(initMessage);
            }

        } catch (IOException e) {
            System.err.println("CRÍTICO: Não foi possível inicializar o FileLogger para o arquivo '" + logFilePath
                    + "'. Logando no console. Erro: " + e.getMessage());

            writer = new PrintWriter(System.err, true);
            writer.println(dateFormat.format(new Date())
                    + " - [FileLogger] FALHA AO INICIALIZAR ARQUIVO DE LOG. Este é um fallback para o console.");
            initialized = true;
        }
    }

    public static synchronized void log(String message) {
        if (!initialized || writer == null) {

            System.err.println(dateFormat.format(new Date()) + " - [FileLogger-ERRO_INIT] " + message);
            return;
        }
        String timestamp = dateFormat.format(new Date());
        writer.println(timestamp + " - " + message);
    }

    public static synchronized void log(String component, String message) {
        log("[" + component + "] " + message);
    }

    public static synchronized void close() {
        if (writer != null) {
            log("FileLogger", "Logger encerrando...");
            writer.close();
            writer = null;
            initialized = false;
        }
    }
}