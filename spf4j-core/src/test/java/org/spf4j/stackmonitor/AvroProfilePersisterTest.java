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
package org.spf4j.stackmonitor;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.specific.SpecificDatumReader;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.ApplicationStackSamples;
import org.spf4j.ssdump2.Converter;

/**
 *
 * @author Zoltan Farkas
 */
public class AvroProfilePersisterTest {

  private static final Logger LOG = LoggerFactory.getLogger(AvroProfilePersisterTest.class);

  @Test
  public void testPersister() throws IOException {
    Path file;
    SampleNode sn;
    try (AvroProfilePersister persister = new AvroProfilePersister(org.spf4j.base.Runtime.TMP_FOLDER_PATH,
            "testProfile", true, 60000L)) {
      SampleNodeTest snt = new  SampleNodeTest();
      sn = SampleNode.createSampleNode(snt.newSt1());
      SampleNode.addToSampleNode(sn, snt.newSt2());
      SampleNode.addToSampleNode(sn, snt.newSt3());
      SampleNode.addToSampleNode(sn, snt.newSt4());
      for (int i = 0; i < 10; i++) {
        persister.persist(ImmutableMap.of("test", sn), "tag", Instant.now(), Instant.now());
      }
      file = persister.getTargetFile();
      LOG.debug("persisted profile to {}", file);
    }
    SpecificDatumReader<ApplicationStackSamples> reader = new SpecificDatumReader<>(ApplicationStackSamples.class);
    try (DataFileStream<ApplicationStackSamples> stream = new DataFileStream<>(Files.newInputStream(file), reader)) {
      for (int i = 0; i < 10; i++) {
        ApplicationStackSamples samples = stream.next();
        Assert.assertEquals("test", samples.getContext());
        Assert.assertEquals(sn, Converter.convert(samples.getStackSamples().iterator()));
      }
    }

  }

}
