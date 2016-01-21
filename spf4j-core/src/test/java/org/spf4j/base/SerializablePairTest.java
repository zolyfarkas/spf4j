/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import junit.framework.Assert;
import org.junit.Test;


/**
 *
 * @author zoly
 */
public class SerializablePairTest {


    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        final SerializablePair<String, String> serializablePair = new SerializablePair<>("a", "b");
        oos.writeObject(serializablePair);
        oos.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        SerializablePair<String, String> sp = (SerializablePair<String, String>) ois.readObject();
        Assert.assertEquals(serializablePair, sp);
        ois.close();
    }

}
