package servers;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;

public class Database {
    //lista thread-safe para armazenar os dados climáticos
    private final List<String> storedData = new CopyOnWriteArrayList<>();

    public void addData(String climateRecord) {
        storedData.add(climateRecord);
    }

    public List<String> getAllData() {
        return new ArrayList<>(storedData);
    }

    public String getLastData() {
        if (storedData.isEmpty()) {
            return "Nenhum dado disponível ainda.";
        }
        return storedData.get(storedData.size() - 1);
    }
}
