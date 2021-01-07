package org.ornamental.text.impl;

import org.ornamental.text.FitnessQuery;
import org.ornamental.text.TemplateSearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HashedTemplateSearch implements TemplateSearch<HashedTemplateSearch.Template> {

    public static class Template {

        private final int[][] anchorTokenHashes;

        private Template(int[][] anchorTokenHashes) {
            this.anchorTokenHashes = anchorTokenHashes;
        }

        /**
         * Create template object from pre-calculated anchor token hashes. The caller side MUST ensure
         * the tokenization and hashing are compatible with those of the target {@link HashedTemplateSearch} instance.
         *
         * @param anchorTokenHashes the pre-calculated hashes of template anchor tokens, available from elsewhere
         * @return the template object
         */
        public static Template fromHashArray(int[][] anchorTokenHashes) {
            return new Template(
                Stream.of(anchorTokenHashes)
                    .map(int[]::clone)
                    .toArray(int[][]::new));
        }
    }

    private static class Query implements FitnessQuery<Template> {

        private final int[] tokenHashes;

        private Query(int[] tokenHashes) {
            this.tokenHashes = tokenHashes;
        }

        @Override
        public int calculateFitness(Template template) {
            return doCalculateFitness(tokenHashes, template.anchorTokenHashes);
        }
    }

    private final TemplateSplitter templateSplitter;

    private final Tokenizer tokenizer;

    private final TokenHash tokenHash;

    public HashedTemplateSearch(
        TemplateSplitter templateSplitter, Tokenizer tokenizer, TokenHash tokenHash) {

        this.templateSplitter = templateSplitter;
        this.tokenizer = tokenizer;
        this.tokenHash = tokenHash;
    }

    @Override
    public FitnessQuery<Template> prepareQuery(String instantiationSubstring) {
        return new Query(tokenizer.tokenize(instantiationSubstring)
            .mapToInt(tokenHash::hash)
            .toArray());
    }

    public Template prepareTemplate(String template) {
        List<int[]> anchorTokenHashes = templateSplitter.splitToAnchors(template)
            .map(a -> tokenizer.tokenize(a).mapToInt(tokenHash::hash).toArray())
            .collect(Collectors.toList());
        List<int[]> innerEmptyAnchorsMerged = new ArrayList<>(anchorTokenHashes.size());
        for (int i = 0; i < anchorTokenHashes.size(); i++) {
            int[] tokenHashes = anchorTokenHashes.get(i);
            if (i == 0 || i == anchorTokenHashes.size() - 1 || tokenHashes.length > 0) {
                innerEmptyAnchorsMerged.add(tokenHashes);
            }
        }

        return new Template(innerEmptyAnchorsMerged.toArray(int[][]::new));
    }

    private static int doCalculateFitness(int[] tokenizedQuery, int[][] tokenizedTemplate) {

        int anchorCount = tokenizedTemplate.length;
        if (anchorCount == 0) {
            return 0;
        }

        // see if the query completely fits in any of the template anchors
        for (int[] anchor : tokenizedTemplate) {
            if (indexOf(tokenizedQuery, anchor, 0) != -1) {
                return tokenizedQuery.length;
            }
        }

        // now see which sequences of anchors completely fit in the query
        int lastIndex = 0;
        int partCount = 0;
        int[] foundStart = new int[anchorCount]; // starts of ranges, inclusive
        int[] foundEnd = new int[anchorCount]; // ends of ranges, exclusive
        Arrays.fill(foundStart, -1);

        for (int i = 0; i < anchorCount; i++) {
            int[] anchor = tokenizedTemplate[i];
            int foundAtIndex = indexOf(anchor, tokenizedQuery, lastIndex);
            if (foundAtIndex != -1) {
                if (foundStart[partCount] == -1) {
                    foundStart[partCount] = i;
                }
                lastIndex = foundAtIndex + anchor.length;
            } else {
                if (foundStart[partCount] != -1) {
                    foundEnd[partCount] = i;
                    partCount++;
                } else if (i > 0) {
                    foundStart[partCount] = i;
                    foundEnd[partCount] = i;
                    partCount++;
                }
                lastIndex = 0;
            }
        }
        if (foundStart[partCount] != -1) {
            foundEnd[partCount] = anchorCount;
            partCount++;
        }

        // scan all the found sequences; weight them, taking into account possible additions
        // from previously unmatched query prefix and postfix
        int maxWeight = 0;
        for (int i = 0; i < partCount; i++) {
            int weight = 0;

            int start = foundStart[i];
            int end = foundEnd[i];

            if (start > 0) {
                weight += maxCommonAffix(tokenizedTemplate[start - 1], tokenizedQuery);
            }
            if (end < anchorCount) {
                weight += maxCommonAffix(tokenizedQuery, tokenizedTemplate[end]);
            }
            for (int j = start; j < end; j++) {
                weight += tokenizedTemplate[j].length;
            }

            maxWeight = Math.max(weight, maxWeight);
        }

        return maxWeight;
    }

    private static int indexOf(int[] what, int[] where, int offset) {
        outer:
        for (int i = offset; i <= where.length - what.length; i++) {
            for (int j = 0; j < what.length; j++) {
                if (what[j] != where[i + j]) {
                    continue outer;
                }
            }
            return i;
        }

        return -1;
    }

    private static int maxCommonAffix(int[] leading, int[] trailing) {
        outer:
        for (int i = Math.max(0, leading.length - trailing.length); i < leading.length; i++) {
            for (int j = 0; j < leading.length - i; j++) {
                if (leading[i + j] != trailing[j]) {
                    continue outer;
                }
            }
            return leading.length - i;
        }

        return 0;
    }
}
