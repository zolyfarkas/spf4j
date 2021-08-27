/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

/**
 * @author Zoltan Farkas
 */
public final class AesEncryptorDecryptor implements EncryptorDecryptor {

  private static class Random {

    private static final AesEncryptorDecryptor INSTANCE = new AesEncryptorDecryptor();
  }

  private static final String ALGO_CLASS = "AES";
  private static final String ALGO = "AES/GCM/NoPadding";

  private static final GCMParameterSpec DEFAULT_PARAMS = new GCMParameterSpec(96, new byte[]{
    (byte) 0x51, (byte) 0x65, (byte) 0x22, (byte) 0x23,
    (byte) 0x64, (byte) 0x05, (byte) 0x6A, (byte) 0xBE,
    (byte) 0x51, (byte) 0x65, (byte) 0x22, (byte) 0x23,
    (byte) 0x64, (byte) 0x05, (byte) 0x6A, (byte) 0xBE});

  private static final ThreadLocal<Cipher> CIPHER = ThreadLocal.withInitial(() -> {
    try {
      return Cipher.getInstance(ALGO);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
      throw new RuntimeException(ex);
    }
  });

  private final Key key;

  private AesEncryptorDecryptor() {
    this(randomKey());
  }

  public AesEncryptorDecryptor(final Key key) {
    this.key = key;
  }

  public static AesEncryptorDecryptor randomKeyInstance() {
    return Random.INSTANCE;
  }

  private static Key randomKey() {
    try {
      return KeyGenerator.getInstance(ALGO_CLASS).generateKey();
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  public byte[] decrypt(final byte[] bytes) throws GeneralSecurityException {
    Cipher cipher = CIPHER.get();
    cipher.init(Cipher.DECRYPT_MODE, key, DEFAULT_PARAMS);
    return cipher.doFinal(bytes);
  }

  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public byte[] encrypt(final byte[] bytes) {
    try {
      Cipher cipher = CIPHER.get();
      cipher.init(Cipher.ENCRYPT_MODE, key, DEFAULT_PARAMS);
      return cipher.doFinal(bytes);
    } catch (InvalidKeyException | InvalidAlgorithmParameterException
            | IllegalBlockSizeException | BadPaddingException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public String toString() {
    return "AesEncryptorDecryptor";
  }

}
