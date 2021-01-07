package org.ornamental.text;

public interface FitnessQuery<T> {

    int calculateFitness(T template);
}