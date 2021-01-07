package org.ornamental.text.impl;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class WordNoPunctuationTokenizer implements Tokenizer {

    private static final Pattern SPACES_PATTERN = Pattern.compile("\\s+");

    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("^\\p{Punct}+|\\p{Punct}+$");

    @Override
    public Stream<String> tokenize(String s) {
        return SPACES_PATTERN.splitAsStream(s)
            .map(w -> PUNCTUATION_PATTERN.matcher(w).replaceAll(""))
            .filter(w -> !w.isEmpty());
    }
}
