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
package org.spf4j.recyclable.impl;

import org.spf4j.recyclable.Disposable;
import org.spf4j.recyclable.ObjectBorower;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.RecyclingSupplier;
import java.util.Collection;
import org.junit.Test;
import org.spf4j.base.Either;

/**
 *
 * @author zoly
 */
public final class SimpleSmartObjectPoolTest implements ObjectBorower<SimpleSmartObjectPoolTest.TestObject> {

    private TestObject borowedObject = null;
    private final SimpleSmartObjectPool<TestObject> instance;

    public SimpleSmartObjectPoolTest() throws ObjectCreationException {
        instance = new SimpleSmartObjectPool(2, 10, new RecyclingSupplier.Factory<TestObject>() {
            @Override
            public TestObject create() {
                System.out.println("Creating Object");
                return new TestObject("Object");
            }

            @Override
            public void dispose(final TestObject object) {
                System.out.println("Disposing Object");
                object.dispose();
            }

            @Override
            public boolean validate(final TestObject object, final Exception e) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        }, true);
    }

    @Override
    public Either<Action, SimpleSmartObjectPoolTest.TestObject> tryRequestReturnObject() {
        if (borowedObject != null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            instance.recycle(borowedObject, this);
        }
        return Either.left(Action.NONE);
    }

    @Override
    public TestObject tryReturnObjectIfNotInUse() {
        return borowedObject;
    }

    @Override
    public boolean scan(final ScanHandler<TestObject> handler) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<TestObject> tryReturnObjectsIfNotNeededAnymore() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<TestObject> tryReturnObjectsIfNotInUse() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void nevermind(final TestObject object) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static final class TestObject implements Disposable {

        private boolean disposed = false;
        private final String data;

        public TestObject(final String data) {
            this.data = data;
        }

        public String getData() {
            if (!disposed) {
                return data;
            } else {
                throw new RuntimeException(data + " is already disposed");
            }
        }

        @Override
        public void dispose() {
            disposed = true;
        }
    }

    /**
     * Test of get method, of class SimpleSmartObjectPool.
     */
    @Test
    public void testPool() throws Exception {
        System.out.println("borrowObject");
        borowedObject = instance.get(this);
        instance.recycle(borowedObject, this);
        instance.dispose();
    }
}
