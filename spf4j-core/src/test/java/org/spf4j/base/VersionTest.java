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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public class VersionTest {

  private static final Logger LOG = LoggerFactory.getLogger(VersionTest.class);

  @Test
  @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
  public void testVersion() {
    Version version1 = new Version("1.u1.3");
    Version version2 = new Version("1.u10.3");
    Assert.assertTrue(version1.compareTo(version2) < 0);
    LOG.debug("version1 = {}", version1);
    Assert.assertEquals(Integer.valueOf(3), version1.getComponents()[3]);
    Version javaVersion = new Version(org.spf4j.base.Runtime.JAVA_VERSION);
    LOG.debug("version1 = {}, image = {}", javaVersion, javaVersion.getImage());
    Assert.assertTrue(javaVersion.compareTo(new Version("1.6.0_1")) > 0);
  }

  @Test
  public void testVersion2() {
    Version version1 = new Version("1.1");
    Version version2 = new Version("1.1.2");
    Assert.assertTrue(version1.compareTo(version2) < 0);
  }

  @Test
  public void testVersion3() {
    Version version1 = new Version("1.8.1");
    Version version2 = new Version("1.8.0.25p");
    Assert.assertTrue(version1.compareTo(version2) > 0);
    Assert.assertTrue(version2.compareTo(version1) < 0);
  }

  @Test
  public void testVersion4() {
    Version version = new Version(org.spf4j.base.Runtime.JAVA_VERSION);
    Assert.assertEquals(1, version.getMajor());
    Assert.assertEquals(8, version.getMinor());
    Version clone = Objects.clone(version);
    Assert.assertEquals(version, clone);
  }

  @Test
  public void testVersion5() {
    Version version1 = new Version("1.8.1");
    Version version2 = new Version("1.8.2-SNAPSHOT");
    Assert.assertTrue(version1.compareTo(version2) < 0);
  }

  @Test
  public void testVersion6() {
    Version version1 = new Version("1.8.2");
    Version version2 = new Version("1.8.2-SNAPSHOT");
    Assert.assertTrue(version1.compareTo(version2) > 0);
    Assert.assertTrue(version2.compareTo(version1) < 0);
  }

  @Test
  public void testVersion7() {
    Version version1 = new Version("1.8.3-SNAPSHOT");
    Version version2 = new Version("1.8.2-SNAPSHOT");
    Assert.assertTrue(version1.compareTo(version2) > 0);
    Assert.assertTrue(version2.compareTo(version1) < 0);
  }

}
