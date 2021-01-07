package org.ornamental.text.impl;

public final class StandardHash implements TokenHash {

    @Override
    public int hash(String token) {
        return token.hashCode();
    }
}
