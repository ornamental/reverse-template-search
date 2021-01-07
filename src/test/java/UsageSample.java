import org.ornamental.text.FitnessQuery;
import org.ornamental.text.impl.HashedTemplateSearch;
import org.ornamental.text.impl.MappingTokenizer;
import org.ornamental.text.impl.Md5Hash;
import org.ornamental.text.impl.PlaceholderRegexSplitter;
import org.ornamental.text.impl.WordNoPunctuationTokenizer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UsageSample {

    public static void main(String... args) {

        HashedTemplateSearch templateSearch = new HashedTemplateSearch(
            new PlaceholderRegexSplitter("%([1-9]\\d*\\$)?s"), // accept placeholders like %s, %1$s, &c.
            new MappingTokenizer(new WordNoPunctuationTokenizer(), s -> s.toLowerCase(Locale.ROOT)),
            new Md5Hash());

        FitnessQuery<HashedTemplateSearch.Template> query = templateSearch.prepareQuery(
            "host www.google.com is");

        Map<HashedTemplateSearch.Template, String> templates = Stream
            .of("An unknown error was returned by the remote service: '%s'.",
                "Synchronization failed due to an unknown error.",
                "Synchronization failed: host %1$s is not reachable.",
                "Host IP %s is incorrect. Please specify a reachable host and port.")
            .collect(Collectors.toMap(
                templateSearch::prepareTemplate,
                Function.identity()));

        List<String> sortedByFitness = templates.keySet().stream()
            .sorted(Comparator.comparing(query::calculateFitness).reversed())
            .map(templates::get)
            .collect(Collectors.toList());

        sortedByFitness.forEach(System.out::println);
    }
}
