------------------------------------------------------------------------------------------------------------------------
-- SUPPORTING FUNCTIONS FOR PRE-CALCULATION OF TOKENIZED

-- Note that the template splitter, anchor tokenizer, and token hashing must have the same logic
-- as their counterparts in Java.
-- The functions below correspond to implementations:
--     * PlaceholderRegexSplitter as template placeholder (the placeholder pattern has to be supplied),
--     * WordNoPunctuationTokenizer as tokenizer
--     * Md5Hash as token hash function (mind its case sensitivity).

-- The function splits text at spaces, for each token removes leading and trailing punctuation,
-- then hashes each token to 32-bit integer (empty tokens are skipped).
CREATE FUNCTION tokenize(_text TEXT)
RETURNS INTEGER[] AS $$
    WITH part(pt, ord) AS (
        SELECT * FROM regexp_split_to_table(_text, '\s+') WITH ORDINALITY
    ),
    no_punctuation(pt, ord) AS (
        SELECT regexp_replace(pt, '^[[:punct:]]+|[[:punct:]]+$', '', 'g'), ord FROM part
    )
    SELECT COALESCE(array_agg(('x' || SUBSTR(md5(pt), 1, 8))::BIT(32)::INTEGER ORDER BY ord), '{}')
    FROM no_punctuation
    WHERE pt <> '';
$$
LANGUAGE SQL
STABLE;

-- The function handles a template string returning an array of its anchor tokens' hashes, with 0 hash serving
-- as delimiter between anchors. One-dimensional array is returned instead of two-dimensional one to facilitate
-- retrieval of its contents in Java.
CREATE FUNCTION prepare_template(_template TEXT, _regexp TEXT)
RETURNS INTEGER[] AS $$
    WITH parts(pt, ord) AS (
        -- non-space characters adjacent to placeholder must be considered a part of it
        SELECT * FROM regexp_split_to_table(_template, '\S*' || _regexp || '\S*') WITH ORDINALITY
    ),
    tokenized_parts(pt, ord) AS (
        SELECT tokenize(pt), ord FROM parts
    ),
    squashed_empty(pt, ord) AS (
        SELECT pt, ord FROM tokenized_parts AS tp1
        WHERE pt <> '{}' OR ord = 1 OR ord = (SELECT MAX(ord) FROM tokenized_parts)
    ),
    with_trailing_null(pt, ord) AS (
        SELECT
            CASE ord < (SELECT MAX(ord) FROM squashed_empty)
                -- consider possibility of hash = 0 negligible, so use as separator
                WHEN true THEN array_append(pt, 0)
                ELSE pt
            END,
            ord
        FROM squashed_empty
    )
    SELECT COALESCE(array_agg(val ORDER BY ord, ord_in_arr), '{}')
    FROM with_trailing_null, unnest(pt) WITH ORDINALITY AS arr(val, ord_in_arr);
$$
LANGUAGE SQL
STABLE;
