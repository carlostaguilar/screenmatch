package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.DadosEpisodio;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.service.ConsumoAPI;
import br.com.alura.screenmatch.service.ConverteDados;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner sc = new Scanner(System.in);
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConverteDados conversor = new ConverteDados();

    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6c11dd23";

    public void exibeMenu() {

        // Pesquisa da Série

        System.out.print("Digite o nome da série para busca: ");
        var nomeSerie = sc.nextLine();

        var json = consumoAPI.sendRequest(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);

        // Impressão dos dados da Série

        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        System.out.println(dados);
        System.out.println();

        // Coleta de dados das temporadas

        List<DadosTemporada> temporadas = new ArrayList<>();

        System.out.println("Temporadas: ");

        for (int i = 1; i <= dados.totalTemporadas(); i++) {

            json = consumoAPI.sendRequest(ENDERECO + nomeSerie.replace(" ", "+") + "&season=" + i + API_KEY);
            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);

            temporadas.add(dadosTemporada);

        }

        temporadas.forEach(System.out::println);
        System.out.println();

        // Coleta de episódios por temporada

        System.out.println("Episódios: ");

        temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));

        System.out.println();

        // Coleta de avaliação por episódio

        List<DadosEpisodio> dadosEpisodios = temporadas.stream()
                                                       .flatMap(t -> t.episodios().stream())
                                                       .collect(Collectors.toList());

        //Impressão dos 5 melhores episódios

        System.out.println("Top 5 episódios: ");
        dadosEpisodios.stream()
                      .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
                      .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
                      .limit(5)
                      .forEach(System.out::println);

        System.out.println();

        // Conversão de variáveis por episódio

        List<Episodio> episodios = temporadas.stream()
                                             .flatMap(t -> t.episodios().stream()
                                             .map(d -> new Episodio(t.numero(), d)))
                                             .collect(Collectors.toList());


        System.out.println("Episódios convertidos: ");
        episodios.forEach(System.out::println);
        System.out.println();

        // Busca por titulo de episódio (contains)

        System.out.print("Digite um trecho do titulo do episódio: ");
        var trechoTitulo = sc.nextLine();

        Optional<Episodio> episodioBuscado = episodios.stream()
                                                      .filter(e -> e.getTitulo().toUpperCase().contains(trechoTitulo.toUpperCase()))
                                                      .findFirst();
        if(episodioBuscado.isPresent()) {
            System.out.println("Episódio encontrado!");
            System.out.println("Temporada: " + episodioBuscado.get().getTemporada());
            System.out.println();
        } else {
            System.out.println("Episódio não encontrado");
            System.out.println();
        }

        // Busca a partir de dado ano

        System.out.println("A partir de que ano você deseja ver os episódios? ");
        var ano = sc.nextInt();
        sc.nextLine();
        System.out.println();

        LocalDate dataBusca = LocalDate.of(ano, 1, 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyy");

        System.out.println("Lista de episódios a partir de " + dataBusca + ": ");

        episodios.stream()
                 .filter(e ->e.getDataDeLancamento() != null && e.getDataDeLancamento().isAfter(dataBusca))
                 .forEach(e -> System.out.println(
                         "Temporada: " + e.getTemporada() +
                         " Episódio: " + e.getTitulo() +
                         " Data de Lançamento: " + e.getDataDeLancamento().format(formatter)));
        System.out.println();

        // Coleta de avaliação por temporada

        Map<Integer, Double> avaliacoesPorTemporada = episodios.stream()
                                                               .filter(e -> e.getAvaliacao() > 0.0)
                                                               .collect(Collectors.groupingBy(Episodio::getTemporada,
                                                                        Collectors.averagingDouble(Episodio::getAvaliacao)));

        System.out.println("Avaliação por temporada: ");
        System.out.println(avaliacoesPorTemporada);
        System.out.println();

        // Coletando Estatísticas

        System.out.println("Estatísticas de Episódios: ");
        DoubleSummaryStatistics est = episodios.stream()
                                               .filter(e -> e.getAvaliacao() > 0.0)
                                               .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));

        System.out.println(est);
        System.out.println();

        System.out.println("Média: " + est.getAverage());
        System.out.println("Melhor Episódio: " + est.getMax());
        System.out.println("Pior Episódio: " + est.getMin());
        System.out.println("Quantidade de episódios avaliados: " + est.getCount());
        System.out.println();

    }

}
