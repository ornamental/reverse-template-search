package org.ornamental.text.impl;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PlaceholderRegexSplitter implements TemplateSplitter {

    private final Pattern placeholderRegex;

    public PlaceholderRegexSplitter(String placeholderRegex) {
        // non-space characters adjacent to placeholder will be considered a part of it
        this.placeholderRegex = Pattern.compile("\\S*" + placeholderRegex + "\\S*");
    }

    @Override
    public Stream<String> splitToAnchors(String template) {
        return Stream.of(placeholderRegex.split(template));
    }
}
