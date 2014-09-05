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
package org.spf4j.pool.jdbc;

import com.google.common.annotations.Beta;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;

/**
 *
 * @author zoly
 */
@Beta
public final class JdbcConnectionFactory  implements RecyclingSupplier.Factory<Connection> {

    public JdbcConnectionFactory(final String driverName, final String url,
            final String user, final String password) {
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        this.url = url;
        this.password = password;
        this.user = user;
    }
    
    private final String url;
    private final String user;
    private final String password;
    private RecyclingSupplier<Connection> pool;
    
    
    @Override
    public Connection create() throws ObjectCreationException {
        final Connection conn;
        try {
            conn =  DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            throw new ObjectCreationException(ex);
        }
        
        return (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?> [] {Connection.class}, new InvocationHandler() {

            private Exception ex;

            
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
                if ("close".equals(method.getName())) {
                    pool.recycle((Connection) proxy, ex);
                    ex = null;
                    return null;
                } else {
                    try {
                        return method.invoke(conn, args);
                    } catch (IllegalAccessException e) {
                        ex = e;
                        throw e;
                    } catch (IllegalArgumentException e) {
                        ex = e;
                        throw e;
                    } catch (InvocationTargetException e) {
                        ex = e;
                        throw e;
                    } catch (RuntimeException e) {
                        ex = e;
                        throw e;
                    }
                }
            }
        });
        
    }

    @Override
    public void dispose(final Connection object) throws ObjectDisposeException {
        try {
            object.unwrap(Connection.class).close();
        } catch (SQLException ex) {
            throw new ObjectDisposeException(ex);
        }
    }

    
    @Override
    public boolean validate(final Connection object, final Exception e) throws SQLException {
        return object.isValid(60);
    }

    void setPool(final RecyclingSupplier<Connection> pool) {
        this.pool = pool;
    }
    
}
