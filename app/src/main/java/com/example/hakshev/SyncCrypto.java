package com.example.hakshev;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM with the unit secret. Records are unreadable to the cloud provider. */
final class SyncCrypto {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    SyncCrypto(byte[] secret) {
        // The secret is already 256 random bits, so it is used directly as the key.
        byte[] k = new byte[32];
        System.arraycopy(secret, 0, k, 0, Math.min(32, secret.length));
        key = new SecretKeySpec(k, "AES");
    }

    String encrypt(String plain) throws Exception {
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] enc = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[12 + enc.length];
        System.arraycopy(iv, 0, out, 0, 12);
        System.arraycopy(enc, 0, out, 12, enc.length);
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }

    String decrypt(String b64) throws Exception {
        byte[] blob = Base64.decode(b64, Base64.NO_WRAP);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, blob, 0, 12));
        return new String(c.doFinal(blob, 12, blob.length - 12), StandardCharsets.UTF_8);
    }
}
