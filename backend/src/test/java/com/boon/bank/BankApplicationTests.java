package com.boon.bank;

import javax.crypto.KeyGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class BankApplicationTests {

    @Test
    void generateJwtSecret() throws Exception {
        // HS512 = 512-bit key = 64 bytes (do dai toi da cho HMAC-SHA)
        KeyGenerator kg = KeyGenerator.getInstance("HmacSHA512");
        kg.init(512);
        var key = kg.generateKey();
        String base64Secret = Base64.getEncoder().encodeToString(key.getEncoded());

        System.out.println("===========================================");
        System.out.println("JWT SECRET (HS512 - 512-bit / 64 bytes):");
        System.out.println(base64Secret);
        System.out.println("Do dai: " + base64Secret.length() + " ky tu");
        System.out.println("===========================================");
    }
}
