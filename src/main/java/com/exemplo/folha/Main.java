package com.exemplo.folha;

import java.util.*;

public class Main {

    public static void main(String[] args) {

        // vars de entrada
        NoCalculo salarioBase = new NoCalculo("salario_base", null, TipoRubrica.INTERMEDIARIO, false, null);
        NoCalculo dependentes = new NoCalculo("dependentes", null, TipoRubrica.INTERMEDIARIO, false, null);
        NoCalculo horasExtrasQtd = new NoCalculo("horas_extras_qtd", null, TipoRubrica.INTERMEDIARIO, false, null);
        NoCalculo valorHora = new NoCalculo("valor_hora", null, TipoRubrica.INTERMEDIARIO, false, null);
        NoCalculo recebeVt = new NoCalculo("recebe_vt", null, TipoRubrica.INTERMEDIARIO, false, null);

        // formulas proventos (feitas pelo RH)
        NoCalculo valorHorasExtras = new NoCalculo("valor_horas_extras", "#horas_extras_qtd * #valor_hora * 1.5", TipoRubrica.PROVENTO, true, "102");
        NoCalculo bonusAssiduidade = new NoCalculo("bonus_assiduidade", "#salario_base * 0.1", TipoRubrica.PROVENTO, true, "105"); // Nova rubrica de bônus!

        // formulas descontos descontos
        NoCalculo inss = new NoCalculo("inss", "#tabelaImposto.calcularInss(#total_proventos)", TipoRubrica.DESCONTO, true, "201");
        NoCalculo deducaoDependente = new NoCalculo("deducao_dependente", "#dependentes * 189.59", TipoRubrica.INTERMEDIARIO, false, null);
        NoCalculo baseIrrf = new NoCalculo("base_irrf", "#total_proventos - #inss - #deducao_dependente", TipoRubrica.INTERMEDIARIO, false, null);
        NoCalculo irrf = new NoCalculo("irrf", "#base_irrf <= 2250.0 ? 0.0 : (#base_irrf * 0.15) - 381.44", TipoRubrica.DESCONTO, true, "202");
        NoCalculo descontoVt = new NoCalculo("desconto_vt", "#recebe_vt == true ? (#salario_base * 0.06) : 0.0", TipoRubrica.DESCONTO, true, "301");

        // proventos e descontos (começa com formula nula, ela é criada pelo motor)
        NoCalculo totalProventos = new NoCalculo("total_proventos", null, TipoRubrica.INTERMEDIARIO, false, null);
        NoCalculo totalDescontos = new NoCalculo("total_descontos", null, TipoRubrica.INTERMEDIARIO, false, null);

        // salario liquido
        NoCalculo salarioLiquido = new NoCalculo("salario_liquido", "#salario_base + #total_proventos - #total_descontos", TipoRubrica.INTERMEDIARIO, true, "900");

        List<NoCalculo> todosOsNos = Arrays.asList(
                salarioBase, dependentes, horasExtrasQtd, valorHora, recebeVt,
                valorHorasExtras, bonusAssiduidade, inss, deducaoDependente,
                baseIrrf, irrf, descontoVt, totalProventos, totalDescontos, salarioLiquido
        );

        MotorCalculo motor = new MotorCalculo();

        // aq o motor monta as formulas do total de proventos e total de descontos
        motor.injetarFormulasDeAgregacao(todosOsNos);

        // aq o motor descobre as dependências
        motor.mapearDependenciasDinamicamente(todosOsNos);

        // aq o spel parseia as strings em formulas
        motor.compilarFormulas(todosOsNos);

        // aq o motor monta o grafo via JGraph e obtém a ordenação topológica
        List<NoCalculo> ordemExecucao = motor.resolverOrdemExecucao(todosOsNos);

        System.out.println("\n>>> ORDEM DE EXECUÇÃO GERADA PELO GRAFO:");
        for (int i = 0; i < ordemExecucao.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + ordemExecucao.get(i).getNomeVariavel());
        }
        System.out.println();

        // dados genéricos de funcionarios
        Map<String, Object> entradasJoao = new HashMap<>();
        entradasJoao.put("salario_base", 3000.00);
        entradasJoao.put("dependentes", 0);
        entradasJoao.put("horas_extras_qtd", 10.0);
        entradasJoao.put("valor_hora", 20.00);
        entradasJoao.put("recebe_vt", true);

        motor.executar(ordemExecucao, entradasJoao, "João Silva");

        Map<String, Object> entradasMaria = new HashMap<>();
        entradasMaria.put("salario_base", 9000.00);
        entradasMaria.put("dependentes", 3);
        entradasMaria.put("horas_extras_qtd", 0.0);
        entradasMaria.put("valor_hora", 60.00);
        entradasMaria.put("recebe_vt", false);

        motor.executar(ordemExecucao, entradasMaria, "Maria Ana");
    }
}