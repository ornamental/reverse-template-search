package org.ornamental.text.impl;

import java.util.stream.Stream;

public interface Tokenizer {

    Stream<String> tokenize(String s);
}
