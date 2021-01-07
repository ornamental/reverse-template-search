package org.ornamental.text.impl;

import java.util.function.Function;
import java.util.stream.Stream;

public final class MappingTokenizer implements Tokenizer {

    private final Tokenizer base;

    private final Function<String, String> mapping;

    public MappingTokenizer(Tokenizer base, Function<String, String> mapping) {
        this.base = base;
        this.mapping = mapping;
    }

    @Override
    public Stream<String> tokenize(String s) {
        return base.tokenize(s)
            .map(mapping);
    }
}
