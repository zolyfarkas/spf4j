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
 * @author Zoltan Farkas
 */
@Beta
public final class EncryptedBytes {

  private final byte[] content;

  public EncryptedBytes(final byte[] unencrypted) {
    content = AesEncryptorDecryptor.randomKeyInstance().encrypt(unencrypted);
    Arrays.fill(unencrypted, 0, unencrypted.length, (byte) 0);
  }

  public void access(final Consumer<byte[]> consumer) {
    byte[] decrypted;
    try {
      decrypted = AesEncryptorDecryptor.randomKeyInstance().decrypt(content);
    } catch (GeneralSecurityException ex) {
      throw new RuntimeException(ex);
    }
    consumer.accept(decrypted);
    Arrays.fill(decrypted, 0, decrypted.length, (byte) 0);
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
    final EncryptedBytes other = (EncryptedBytes) obj;
    return java.util.Arrays.equals(this.content, other.content);
  }

  @Override
  public String toString() {
    return "Not Accessible";
  }

}
