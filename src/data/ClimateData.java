package data;

public class ClimateData {
    private double temperatura; 
    private double umidade;
    private double pressao;
    private double radiacao;

    public ClimateData(double temperatura, double umidade, double pressao, double radiacao) {
        this.temperatura = temperatura;
        this.umidade = umidade;
        this.pressao = pressao;
        this.radiacao = radiacao;
    }

    public double temperatura() {
        return temperatura;
    }

    public double umidade() {
        return umidade;
    }

    public double pressao() {
        return pressao;
    }

    public double radiacao() {
        return radiacao;
    }
}
