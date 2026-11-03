package com.exemplo.folha;

import java.util.*;

public class Main {

    public static void main(String[] args) {

        // Fúrmulas q o RH cadastrou        
        NoCalculo salarioBase = new NoCalculo("salario_base", null, false, null);
        NoCalculo dependentes = new NoCalculo("dependentes", null, false, null);
        NoCalculo horasExtrasQtd = new NoCalculo("horas_extras_qtd", null, false, null);
        NoCalculo valorHora = new NoCalculo("valor_hora", null, false, null);
        NoCalculo valorHorasExtras = new NoCalculo("valor_horas_extras", "#horas_extras_qtd * #valor_hora * 1.5", true, "102");
        NoCalculo salarioBruto = new NoCalculo("salario_bruto", "#salario_base + #valor_horas_extras", false, null);
        NoCalculo inss = new NoCalculo("inss", "#tabelaImposto.calcularInss(#salario_bruto)", true, "201");
        NoCalculo deducaoDependente = new NoCalculo("deducao_dependente", "#dependentes * 189.59", false, null);
        NoCalculo baseIrrf = new NoCalculo("base_irrf", "#salario_bruto - #inss - #deducao_dependente", false, null);
        NoCalculo irrf = new NoCalculo("irrf", "#base_irrf <= 2250.0 ? 0.0 : (#base_irrf * 0.15) - 381.44", true, "202");
        NoCalculo salarioLiquido = new NoCalculo("salario_liquido", "#salario_bruto - #inss - #irrf", true, "900");

        List<NoCalculo> todosOsNos = Arrays.asList(
                salarioBase, dependentes, horasExtrasQtd, valorHora,
                valorHorasExtras, salarioBruto, inss, deducaoDependente,
                baseIrrf, irrf, salarioLiquido
        );

        MotorCalculo motor = new MotorCalculo();

        // Aq o motor descobre todas as dependências só analisando as strings
        motor.mapearDependenciasDinamicamente(todosOsNos);

        // Aq o motor parseia todas as fórmulas
        motor.compilarFormulas(todosOsNos);

        // Aq o Jgraph monta e ordena o grafo
        List<NoCalculo> ordemExecucao = motor.resolverOrdemExecucao(todosOsNos);

        System.out.println("\n>>> ORDEM DE EXECUÇÃO DETECTADA AUTOMATICAMENTE PELO MOTOR:");
        for (int i = 0; i < ordemExecucao.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + ordemExecucao.get(i).getNomeVariavel());
        }
        System.out.println();

        // Dados genéricos simulando funcionários

        // Colaborador 1: João (Salário menor)
        Map<String, Object> entradasJoao = new HashMap<>();
        entradasJoao.put("salario_base", 2000.00);
        entradasJoao.put("dependentes", 1);
        entradasJoao.put("horas_extras_qtd", 5.0);
        entradasJoao.put("valor_hora", 20.00);

        motor.executar(ordemExecucao, entradasJoao, "João Silva");

        // Colaborador 2: Maria (Salário maior, testando faixa do INSS e isenção de IRRF pelo ternário)
        Map<String, Object> entradasMaria = new HashMap<>();
        entradasMaria.put("salario_base", 7000.00);
        entradasMaria.put("dependentes", 0);
        entradasMaria.put("horas_extras_qtd", 0.0);
        entradasMaria.put("valor_hora", 0.0);

        motor.executar(ordemExecucao, entradasMaria, "Maria Santos");
    }
}