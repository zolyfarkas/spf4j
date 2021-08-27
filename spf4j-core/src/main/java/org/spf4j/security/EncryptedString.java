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

import com.google.common.annotations.Beta;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;
import org.spf4j.base.Arrays;

/**
 * A string implementation where the content is encrypted with a random key. This maybe a "better" way of storing
 * secrets in memory. Secrets life in memory should be "minimized" as such the use of char[] must be done combined with
 * immediate clear after use. The less time secrets spend in memory, the less chance they might be miss-appropriated.
 *
 * Using the class encrypts the content, and decrypts it every time it is needed to access. The problem with this class
 * is that the key is also in memory, so if you get ahold of the content of this class and the key (like a heap dump)
 * you will be able to decrypt the content. (although through a extra step)
 *
 * Using this class, will help you avoid accidental leakage of the secret though into logs (toString will not output the
 * content) or other similar places.
 *
 * This class is NOT a replacement to storing keys in hardware secure enclaves.
 *
 * @author Zoltan Farkas
 */
@Beta
public final class EncryptedString {

  private final byte[] content;

  public EncryptedString(final char[] chars) {
    byte[] bytes = Arrays.charsToBytes(chars);
    content = AesEncryptorDecryptor.randomKeyInstance().encrypt(bytes);
    Arrays.fill(bytes, 0, bytes.length, (byte) 0);
    Arrays.fill(chars, 0, chars.length, (char) 0);
  }

  public void access(final Consumer<char[]> consumer) {
    byte[] decrypted;
    try {
      decrypted = AesEncryptorDecryptor.randomKeyInstance().decrypt(content);
    } catch (GeneralSecurityException ex) {
      throw new RuntimeException(ex);
    }
    char[] chars = Arrays.bytesToChars(decrypted);
    consumer.accept(chars);
    Arrays.fill(decrypted, 0, decrypted.length, (byte) 0);
    Arrays.fill(chars, 0, chars.length, (char) 0);
  }

  @Override
  public int hashCode() {
    return java.util.Arrays.hashCode(this.content);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final EncryptedString other = (EncryptedString) obj;
    return java.util.Arrays.equals(this.content, other.content);
  }

  @Override
  public String toString() {
    return "Not Accessible";
  }

}
