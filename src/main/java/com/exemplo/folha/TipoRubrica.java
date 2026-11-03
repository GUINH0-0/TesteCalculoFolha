package com.exemplo.folha;

public enum TipoRubrica {
    PROVENTO,     // Soma no salário (Ex: Salário Base, Horas Extras, Bônus)
    DESCONTO,     // Subtrai do salário (Ex: INSS, IRRF, VT)
    INTERMEDIARIO // Apenas cálculos de apoio / totais / entradas base
}