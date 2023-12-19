/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.spacecraft.security.impl;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class CryptoUtil {
    private static final int AES_KEY_LENGTH = 256;
    private static final int AES_ITERATION_COUNT = 65536;
    private static final String AES_SECRET_KEY_FACTORY = "PBKDF2WithHmacSHA256";
    private static final String AES_ALGORITHM = "AES";
    // private static final String AES_CHIPER = "AES/CBC/PKCS5Padding"; //
    private static final String AES_CHIPER = "AES/CBC/NoPadding"; //

    public static byte[] aesEncrypt(byte[] data, int offset, int length, String key, byte[] iv, byte[] salt) throws ReatmetricException {
        try {
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(AES_SECRET_KEY_FACTORY);
            KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, AES_ITERATION_COUNT, AES_KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);

            Cipher cipher = Cipher.getInstance(AES_CHIPER);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);
            return cipher.doFinal(data, offset, length);
        } catch (Exception e) {
            throw new ReatmetricException(e);
        }
    }

    public static byte[] aesDecrypt(byte[] data, int offset, int length, String key, byte[] iv, byte[] salt) throws ReatmetricException {
        try {
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(AES_SECRET_KEY_FACTORY);
            KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, AES_ITERATION_COUNT, AES_KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);

            Cipher cipher = Cipher.getInstance(AES_CHIPER);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);
            return cipher.doFinal(data, offset, length);
        } catch (Exception e) {
            throw new ReatmetricException(e);
        }
    }
}
