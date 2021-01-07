package org.ornamental.text.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Md5Hash implements TokenHash {

    @Override
    public int hash(String token) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Expecting MD5 hashing algorithm to be available.");
        }

        byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        int hash = 0;
        for (int i = 0; i < 4; i++) {
            hash <<= 8;
            hash |= (int)hashBytes[i] & 0xFF;
        }

        return hash;
    }
}
