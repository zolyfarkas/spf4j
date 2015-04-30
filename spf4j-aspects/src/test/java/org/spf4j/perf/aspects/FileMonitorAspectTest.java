/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.perf.aspects;


import org.spf4j.perf.impl.RecorderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.perf.impl.ms.tsdb.TSDBMeasurementStore;
import org.spf4j.tsdb2.TSDBQuery;
import org.spf4j.tsdb2.TSDBWriter;
import org.spf4j.tsdb2.avro.TableDef;


/**
 * @author zoly
 */
public final class FileMonitorAspectTest {

    public FileMonitorAspectTest() {
    }


    /**
     * Test of nioReadLong method, of class FileMonitorAspect.
     */
    @Test
    public void testFileIOMonitoring() throws Exception {
        System.setProperty("perf.file.sampleTimeMillis", "1000");
        File tempFile = File.createTempFile("test", ".tmp");
        tempFile.deleteOnExit();
        try (Writer fw = new OutputStreamWriter(new FileOutputStream(tempFile))) {
            for (int i = 0; i < 10; i++) {
                fw.write("bla bla test\n");
                Thread.sleep(500);
            }
        }
        Thread.sleep(1000);
        try (BufferedReader fr = new BufferedReader(new InputStreamReader(new FileInputStream(tempFile)))) {
            String line = fr.readLine();
            while (line != null) {
                System.out.println(line);
                line = fr.readLine();
                Thread.sleep(500);
            }
        }
    }

}
