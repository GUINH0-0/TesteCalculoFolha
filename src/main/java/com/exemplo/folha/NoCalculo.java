package com.exemplo.folha;

import org.springframework.expression.Expression;
import java.util.ArrayList;
import java.util.List;

public class NoCalculo {

    private String nomeVariavel;
    private String formulaString;
    private TipoRubrica tipo;
    private Expression expressaoSpel;
    private boolean geraRubrica;
    private String codigoRubrica;
    private Object valorSaida;
    private List<NoCalculo> dependencias = new ArrayList<>();

    public NoCalculo(String nomeVariavel, String formulaString, TipoRubrica tipo, boolean geraRubrica, String codigoRubrica) {
        this.nomeVariavel = nomeVariavel;
        this.formulaString = formulaString;
        this.tipo = tipo;
        this.geraRubrica = geraRubrica;
        this.codigoRubrica = codigoRubrica;
    }

    public void adicionarDependencia(NoCalculo no) {
        if (!this.dependencias.contains(no)) {
            this.dependencias.add(no);
        }
    }

    public String getNomeVariavel() { return nomeVariavel; }
    public String getFormulaString() { return formulaString; }
    public void setFormulaString(String formulaString) { this.formulaString = formulaString; }
    public TipoRubrica getTipo() { return tipo; }
    public Expression getExpressaoSpel() { return expressaoSpel; }
    public void setExpressaoSpel(Expression expressaoSpel) { this.expressaoSpel = expressaoSpel; }
    public boolean isGeraRubrica() { return geraRubrica; }
    public String getCodigoRubrica() { return codigoRubrica; }
    public Object getValorSaida() { return valorSaida; }
    public void setValorSaida(Object valorSaida) { this.valorSaida = valorSaida; }
    public List<NoCalculo> getDependencias() { return dependencias; }

    @Override
    public String toString() {
        return nomeVariavel;
    }
}