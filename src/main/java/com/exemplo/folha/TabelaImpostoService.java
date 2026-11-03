package com.exemplo.folha;

public class TabelaImpostoService {

    public double calcularInss(double salarioBruto) {
        if (salarioBruto <= 1412.00) {
            return salarioBruto * 0.075;
        } else if (salarioBruto <= 2666.68) {
            return salarioBruto * 0.09;
        } else if (salarioBruto <= 4000.03) {
            return salarioBruto * 0.12;
        } else {
            return 900.87; // Teto máximo simulado
        }
    }
}