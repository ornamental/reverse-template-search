package org.ornamental.text.impl;

import java.util.stream.Stream;

public interface TemplateSplitter {

    Stream<String> splitToAnchors(String template);
}
