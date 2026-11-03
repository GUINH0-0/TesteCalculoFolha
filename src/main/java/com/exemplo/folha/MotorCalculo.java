package com.exemplo.folha;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.stream.Collectors;

public class MotorCalculo {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    // varre todos os nós do sistema, agrupa quem é PROVENTO e quem é DESCONTO,
    // monta automaticamente as fórmulas de `total_proventos` e `total_descontos`.

    public void injetarFormulasDeAgregacao(List<NoCalculo> todosOsNos) {
        List<String> proventos = todosOsNos.stream()
                .filter(n -> n.getTipo() == TipoRubrica.PROVENTO && n.getFormulaString() != null)
                .map(n -> "#" + n.getNomeVariavel())
                .collect(Collectors.toList());

        List<String> descontos = todosOsNos.stream()
                .filter(n -> n.getTipo() == TipoRubrica.DESCONTO && n.getFormulaString() != null)
                .map(n -> "#" + n.getNomeVariavel())
                .collect(Collectors.toList());

        String formulaProventos = String.join(" + ", proventos);
        String formulaDescontos = descontos.isEmpty() ? "0.0" : String.join(" + ", descontos);

        for (NoCalculo no : todosOsNos) {
            if ("total_proventos".equals(no.getNomeVariavel())) {
                no.setFormulaString(formulaProventos);
            } else if ("total_descontos".equals(no.getNomeVariavel())) {
                no.setFormulaString(formulaDescontos);
            }
        }
    }

    // lê as strings pra descobrir quais variaveis cada nós usa e monstar as dependências
    public void mapearDependenciasDinamicamente(List<NoCalculo> todosOsNos) {
        for (NoCalculo noFilho : todosOsNos) {
            if (noFilho.getFormulaString() == null || noFilho.getFormulaString().trim().isEmpty()) {
                continue;
            }

            for (NoCalculo noCandidatoPai : todosOsNos) {
                if (noFilho.getNomeVariavel().equals(noCandidatoPai.getNomeVariavel())) {
                    continue;
                }

                String tokenVariavel = "#" + noCandidatoPai.getNomeVariavel();
                if (noFilho.getFormulaString().contains(tokenVariavel)) {
                    noFilho.adicionarDependencia(noCandidatoPai);
                }
            }
        }
    }

    // usa o SPEL pra parsear as formulas

    public void compilarFormulas(List<NoCalculo> nos) {
        for (NoCalculo no : nos) {
            if (no.getFormulaString() != null && !no.getFormulaString().trim().isEmpty()) {
                Expression expr = parser.parseExpression(no.getFormulaString());
                no.setExpressaoSpel(expr);
            }
        }
    }
    // usa JGRAPH pra montar o grafo e ordenar topológicamente (usa o algoritmo de Kahn)

    public List<NoCalculo> resolverOrdemExecucao(List<NoCalculo> nos) {
        Graph<NoCalculo, DefaultEdge> grafo = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (NoCalculo no : nos) {
            grafo.addVertex(no);
        }

        for (NoCalculo filho : nos) {
            for (NoCalculo pai : filho.getDependencias()) {
                grafo.addEdge(pai, filho);
            }
        }

        CycleDetector<NoCalculo, DefaultEdge> cycleDetector = new CycleDetector<>(grafo);
        if (cycleDetector.detectCycles()) {
            throw new IllegalStateException("ERRO CRÍTICO: Ciclo detectado nas dependências das variáveis!");
        }

        Iterator<NoCalculo> iterator = new TopologicalOrderIterator<>(grafo);
        List<NoCalculo> ordemTopologica = new ArrayList<>();
        iterator.forEachRemaining(ordemTopologica::add);

        return ordemTopologica;
    }


    // calcula
    public void executar(List<NoCalculo> ordemExecucao, Map<String, Object> entradas, String nomeColaborador) {
        System.out.println("==================================================");
        System.out.println("  EXECUTANDO FOLHA DE: " + nomeColaborador.toUpperCase());
        System.out.println("==================================================");

        // contexto do SPEL, funciona como uma mesa de trabalho que
        // guarda os valores em memória pro spel usar nas expressões
        StandardEvaluationContext context = new StandardEvaluationContext();
        entradas.forEach(context::setVariable);
        context.setVariable("tabelaImposto", new TabelaImpostoService());

        List<String> holerite = new ArrayList<>();

        for (NoCalculo no : ordemExecucao) {
            if (no.getExpressaoSpel() == null) {
                Object valorEntrada = entradas.get(no.getNomeVariavel());
                no.setValorSaida(valorEntrada);
                System.out.printf("[ENTRADA]      %-22s = %s%n", no.getNomeVariavel(), valorEntrada);
                continue;
            }

            Object resultado = no.getExpressaoSpel().getValue(context);
            no.setValorSaida(resultado);
            context.setVariable(no.getNomeVariavel(), resultado);

            System.out.printf("[CALCULADO]    %-22s = %.2f%n", no.getNomeVariavel(), ((Number) resultado).doubleValue());

            if (no.isGeraRubrica()) {
                holerite.add(String.format("  - Rubrica [%s] %-18s: R$ %.2f", 
                        no.getCodigoRubrica(), no.getNomeVariavel().toUpperCase(), ((Number) resultado).doubleValue()));
            }
        }

        System.out.println("\n--------------------------------------------------");
        System.out.println("  HOLERITE GERADO (RÚBRICAS PERSISTIDAS)");
        System.out.println("--------------------------------------------------");
        holerite.forEach(item -> System.out.println(item));
        System.out.println("==================================================\n");
    }
}