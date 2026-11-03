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

public class MotorCalculo {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    // Aq ele analisa diretamente as strings para ver as variáveis e descobrir as dependências
    public void mapearDependenciasDinamicamente(List<NoCalculo> todosOsNos) {
        for (NoCalculo noFilho : todosOsNos) {
            if (noFilho.getFormulaString() == null || noFilho.getFormulaString().trim().isEmpty()) {
                continue;
            }

            // Compara a fórmula do nó atual com o nome de todas as outras variáveis cadastradas no sistema
            for (NoCalculo noCandidatoPai : todosOsNos) {
                if (noFilho.getNomeVariavel().equals(noCandidatoPai.getNomeVariavel())) {
                    continue; // Uma variável não depende dela mesma
                }

                // O padrão no SpEL para usar uma variável é #nome_variavel
                String tokenVariavel = "#" + noCandidatoPai.getNomeVariavel();

                // Se a string da fórmula contém a variável, criamos a dependência organicamente!
                if (noFilho.getFormulaString().contains(tokenVariavel)) {
                    noFilho.adicionarDependencia(noCandidatoPai);
                }
            }
        }
    }

    // Parseia as fórmulas com o SPEL
    public void compilarFormulas(List<NoCalculo> nos) {
        for (NoCalculo no : nos) {
            if (no.getFormulaString() != null && !no.getFormulaString().trim().isEmpty()) {
                Expression expr = parser.parseExpression(no.getFormulaString());
                no.setExpressaoSpel(expr);
            }
        }
    }

    // Resolve a ordem topológica usando JGraphT (usando o algoritmo de Kahn!!)
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

    // Executa o motor por colaborador
    public void executar(List<NoCalculo> ordemExecucao, Map<String, Object> entradas, String nomeColaborador) {
        System.out.println("==================================================");
        System.out.println("  EXECUTANDO FOLHA DE: " + nomeColaborador.toUpperCase());
        System.out.println("==================================================");

        StandardEvaluationContext context = new StandardEvaluationContext();

        // Injeta dados contratuais de entrada
        entradas.forEach(context::setVariable);

        // Injeta serviços auxiliares (como tabelas de impostos complexas ou faixas)
        context.setVariable("tabelaImposto", new TabelaImpostoService());

        List<String> holerite = new ArrayList<>();

        for (NoCalculo no : ordemExecucao) {
            if (no.getExpressaoSpel() == null) {
                Object valorEntrada = entradas.get(no.getNomeVariavel());
                no.setValorSaida(valorEntrada);
                System.out.printf("[ENTRADA]      %-22s = %s%n", no.getNomeVariavel(), valorEntrada);
                continue;
            }

            // Avalia a expressão SpEL com base no contexto acumulado
            Object resultado = no.getExpressaoSpel().getValue(context);
            no.setValorSaida(resultado);

            // Disponibiliza o resultado atualizado para os próximos nós da fila salvando no contexto do SPEL
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