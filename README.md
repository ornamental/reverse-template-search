# Reverse template matching algorithm

## Problem statement

Localized messages are generally text templates, i.e. they may have placeholders,
which, when given concrete values, result in a message visible to product users
(_template instantiation_).

The goal is to implement an algorithm which, given a substring of a template instantiation,
will return the template(s) from which the instantiation was most likely obtained.

The naïve problem statement is\
_A template matches the query is there exists such a parameterization, so that the supplied query
string is a substring of the template instantiation with said parameters._
It is, however, useless, as formally each template with at least one placeholder satisfies this
condition for every query string.\
_Example._ Template `Volume named {0} does not exist.` matches the query `hello world!`, because
the latter is a substring of the template with `{0}` = `hello world!`.

Therefore, instead of a predicate, we implement a ranking function which would satisfy
the following conditions:
- its values must be non-negative;
- the higher the function value, the 'better' the template matches the query;
- zero function value indicates that the template either has no distinctive data for
  matching (e.g. template having nothing but placeholders) or if there is no way the query
  may fit any of the template instantiations.

## Algorithm description
### Heuristic
The implemented algorithm implements a heuristic estimating the maximum number of tokens
in the template which are matched by the query, so that each token in the query either
matches a template token or a placeholder, and the order of tokens and placeholders is preserved.

_Example 1._ For template `Volume named '{0}' does not exist.` and query string
`named 'MY VOLUME' does not` there exists a sequence of 4 tokens in the template
(`named`, placeholder, `does`, `not`) matched by the sequence of tokens (`named`, `does`, `not`)
in the query, such that `MY VOLUME` is covered by the placeholder. Since there exists no longer
such sequence, the fitness value is 3 (the number of non-placeholder tokens in the longest
matching token sequence).

### Tokenization
In order to apply the heuristic, a tokenization algorithm must be chosen.
For instance, considering each individual character as token works.\
We use coarser tokenization:
1. string is split by whitespace characters;
1. for each part, leading and trailing punctuation is removed;
1. each resulting non-empty 'word' is hashed using a 32-bit hash function.

When tokenizing a template, it must be first split by placeholders into non-variable parts
(_anchors_). Each anchor is tokenized as described, resulting in a sequence of 32-bit arrays.\
If a tokenized anchor proves empty, it is dropped unless it is the first or the last one
(which corresponds to considering a sequence of placeholders a single placeholder if they
are separated only with spaces and punctuation).

_Note._ When splitting the template by placeholders, the immediately preceding and following
non-whitespace characters have to be considered part of the placeholder so as to not produce
anchor tokens, since they will generally not produce separate tokens in the query.

The query is tokenized right away resulting in a 32-bit array.

The hash collisions,
though theoretically possible, have sufficiently low probability (1 in 100,000 for ~300 values)
and do not break the algorithm (they may only lead to fitness overestimation).
They are therefore disregarded.

### Fitness calculation
1\. The algorithm commences by checking if the query tokens are a subsequence of any anchor
of the template. If so, the fitness value is the number of tokens in the query.
(In this document, _subsequence_ only stands for elements immediately following one another
in the original sequence.)

_Example 2_.\
Template: `Volume named '{0}' does not exist.`\
Query: `not exist`\
Tokenized template: ((`Volume`, `named`), (`does`, `not`, `exist`))\
Tokenized query: (`not`, `exist`).\
Since (`not`, `exist`) is a subsequence of the anchor (`does`, `not`, `exist`), the fitness
value is 2.

2\. If the check (1) resulted in no value, the algorithm continues by checking which subsequences
of anchors can be found in the same order in the sequence of query tokens.

_Example 3_.\
Template: `Mismatch between volume group {0} (id: {1}) and volume {2} (id:{3})`\
Query: `group MY GROUP (id: 10) and volume MY VOLUME`\
Tokenized template: ((`Mismatch`, `between`, `volume`, `group`), (`id`), (`and`, `volume`), (`id`), ())\
Tokenized query: (`group`, `MY`, `GROUP`, `id`, `10`, `and`, `volume`, `MY`, `VOLUME`).\
The two subsequences of template anchors found in the query in the correct order:
((`id`), (`and`, `volume`)) and ((`id`), ()).

3\. For each found anchor subsequence two additional searches are performed.\
If the first anchor of the subsequence is not the first in the template,
the previous anchor is compared with the query to find the longest suffix of the
anchor which is at the same time a prefix of the query.\
Likewise, if the last anchor of the subsequence is not the last in the template,
the next anchor is compared with the query, but this time the longest affix to
be determined is the prefix of the anchor and suffix of the query.

_Example 3 (continued)._ Token (`id`) from the first found anchor subsequence
((`id`), (`and`, `volume`)) is not the first anchor of the template.
Therefore, a longest common affix is found for the previous anchor
(`Mismatch`, `between`, `volume`, `group`) and the query. The result is (`group`).\
As (`and`, `volume`) is not the last anchor in the template, the longest common
affix of the query and the next anchor (`id`) is also calculated. The result is ().\
Analogically, for the second found anchor subsequence ((`id`), ()) the token (`id`) is not the first
in the template, so longest common affix of (`and`, `volume`) and the query is searched for,
giving ().\
The anchor () of the second found subsequence is the last one in the template, so there is no next
anchor to perform the longest common affix calculation for.

4\. The same procedure as (3) is executed for each placeholder not covered by any of the found
anchor subsequences and not adjacent to one. The previous and the next anchors are the
one immediately to the left and the one immediately to the right of such a placeholder.

_Example 4._
Template: `No volume named '{0}' exists in the volume group {1}.`\
Query: `volume named 'volume 1' exists in`\
Tokenized template: ((`No`, `volume`, `named`), (`exists`, `in`, `the`, `volume`, `group`), ())\
Tokenized query: (`volume`, `named`, `volume`, `1`, `exists`, `in`).\
Step (2) yields no subsequences fully covered by the query, so both placeholders are considered
at step (4).\
For the placeholder `{0}` the previous anchor is (`No`, `volume`, `named`), whose longest suffix
being a query prefix is (`volume`, `named`). The next anchor is
(`exists`, `in`, `the`, `volume`, `group`), whose longest prefix being a query suffix is
(`exists`, `in`).\
For the placeholder `{1}` the previous anchor is (`exists`, `in`, `the`, `volume`, `group`), whose
longest suffix being a query prefix is (). The next anchor is (), the common affix search yields ().

5\. For each found anchor subsequence the number of tokens in the sequence is summated with the
numbers of tokens in common affixes found in (3). For each placeholder not covered by/adjacent to
any found anchor subsequence, the numbers of tokens in the affixes are summated.

6\. The maximum of the sums calculated in (5) is the fitness value.

_Example 3 (continued)._ In example (3) the first found anchor subsequence has total weight of
1 + 3 + 0 = 4, the 1 coming from common (`group`) affix, and the 3 coming from the anchors.\
The second found anchor subsequence has total weight of 0 + 1 + 0 = 1.\
There are no placeholders not covered by found anchor subsequences and not adjacent to any of them,
so there are no more weights to take into account.\
The maximum of the two weights is 4, which is the resulting value.

_Example 4 (continued)._ In example (4) there are no found anchor subsequences,
and the two placeholders have weights 2 + 2 = 4 and 0 + 0 = 0.\
The maximum (resulting value) is 4.

## Shortcomings of the algorithm
The thing to beware is the cases when the algorithm underestimates template fitness, as this may
lead to lower ranking of the template or, in case of zero fitness estimate, its being filtered out
of the result set.

The algorithm does not check that the same placeholder matches same substrings
in candidate anchor subsequences. Implementing this is unlikely to improve the result quality,
as the problem only applies to templates with repeated placeholders and does not lead
to template underestimation.

The algorithm does not check that every query token is matched by a template. The tokens
at the beginning and in the end of the query may be unmatched, and the algorithm may still
return a non-zero fitness value.
This does not lead to template underestimation. Queries issued for real template
instantiation substrings will have no such tokens anyway, unless cropped in the middle of a word.

The algorithm does not consider weights of individual tokens (they all have weight 1).
Giving tokens weights based on token lengths or classes of characters the tokens contain
might improve the ranking.

### Pathological cases

The algorithm may overestimate the weights, because steps (3), (4) do not check that the found
common affixes have empty intersection with the corresponding found anchor subsequence from step 2
or with each other (step (4)).

_Example 5._ If a template has pattern `.. X {0} X Z Y {1} Y ..` where
`X`, `Y`, `Z` are token sequences, and the query is `X Z Y`, the fitness will be
overestimated by |`X`| + |`Y`|.\
Likewise, for a template `.. X {0} X ..` and query `X ..` the overestimation may be of |`X`|.

The algorithm cannot handle queries with cropped first or last word (in the sense of
tokenization), since cropped words will not have hashes found in the template. This may result
in fitness underestimation, but it will apply to all the templates and so will not affect ranking.

## On implementation
When templates are stored in a database and ranking is run many times,
the template preprocessing operations are too costly to run against each template on each search.
This is the reason a template is to be pre-processed upon each update of the template in the database,
for instance, using triggers. 

The source code includes the necessary functions in PL/pgSQL (PostgreSQL).
Since the language has no support for jagged arrays,
the tokens are stored in 1-dimensional array with 0 value as
placeholder delimiter (hash collision has negligible effect here) and has to be split 
to produce the convenient `int[][]` form in Java.

Since in this case tokenization is performed both in Java (for the query) and PostgreSQL (for the templates),
tokenization algorithms MUST match at all times. MD5 is used for hashing, mainly because it is
available both in PostgreSQL and in Java.
