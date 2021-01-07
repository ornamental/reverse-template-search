package org.ornamental.text;

public interface TemplateSearch<T> {

    FitnessQuery<T> prepareQuery(String instantiationSubstring);
}
