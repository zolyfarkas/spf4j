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
package org.spf4j.zel.vm;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Pair;
import org.spf4j.concurrent.FutureBean;

/**
 * Virtual Machine Execution Context
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class ExecutionContext {

    //CHECKSTYLE:OFF
    public final VMExecutor execService;

    public final ResultCache resultCache;

    /**
     * local memory context
     */
    public final HierarchicalMap memory;

    /**
     * the program
     */
    public final Program code;

    /**
     * The Instruction pointer
     */
    public int ip;

    /**
     * The halt register
     */
    public boolean terminated;

    /**
     * The main stack
     */
    private final SimpleStack<Object> stack;

    /**
     * Standard Input
     */
    public final InputStream in;

    /**
     * Standard Output
     */
    public final PrintStream out;

    /**
     * Standard Error Output
     */
    public final PrintStream err;

    
    //CHECKSTYLE:ON
    
    /**
     * The parent Execution Context
     */
    private final ExecutionContext parent;
    
    public FutureBean<Object> suspendedAt;

    private ExecutionContext(final ExecutionContext parent, final VMExecutor service, final Program program) {
        this.in = parent.in;
        this.out = parent.out;
        this.err = parent.err;
        this.memory = new HierarchicalMap(parent.memory);
        this.execService = service;
        this.stack = new SimpleStack(32);
        this.code = program;
        this.resultCache = parent.resultCache;
        this.parent = parent;
        this.ip = 0;
    }

    /**
     * aditional constructor that allows you to set the standard Input/Output streams
     *
     * @param program
     * @param in
     * @param out
     * @param err
     */
    public ExecutionContext(final Program program, final java.util.Map memory,
            @Nullable final InputStream in, @Nullable final PrintStream out, @Nullable final PrintStream err,
            @Nullable final ExecutorService execService) {
        this.code = program;
        this.memory = new HierarchicalMap(memory);
        this.in = in;
        this.out = out;
        this.err = err;
        if (execService != null) {
            this.execService = new VMExecutor(execService);
        } else {
            this.execService = null;
        }
        this.stack = new SimpleStack(32);
        this.ip = 0;
        this.resultCache = new SimpleResultCache();
        this.parent = null;
    }

    /**
     * pops object out of stack
     *
     * @return Object
     */
    public Object popSyncStackVal() throws SuspendedException {
        Object result = this.stack.pop();
        if (result instanceof FutureBean<?>) {
            try {
                final FutureBean<Object> resFut = (FutureBean<Object>) result;
                 Pair<Object, ? extends ExecutionException> resultStore = resFut.getResultStore();
                 if (resultStore != null) {
                    return FutureBean.processResult(resultStore);
                } else {
                    this.stack.push(result);
                    suspendedAt = resFut;
                    throw  SuspendedException.INSTANCE;
                }
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return result;
        }
    }

    public Object[] popSyncStackVals(final int nvals) throws SuspendedException {
        Object[] result = new Object[nvals];
        for (int i = 0; i < nvals; i++) {
            result[i] = stack.pop();
        }
        for (int i = 0; i < nvals; i++) {
            Object obj = result[i];
            if (obj instanceof FutureBean<?>) {
                try {
                    final FutureBean<Object> resFut = (FutureBean<Object>) obj;
                    Pair<Object, ? extends ExecutionException> resultStore = resFut.getResultStore();
                    if (resultStore != null) {
                        result[i] = FutureBean.processResult(resultStore);
                    } else {
                        for (int j = nvals - 1; j >= 0; j--) {
                            stack.push(result[j]);
                        }
                        suspendedAt = resFut;
                        throw  SuspendedException.INSTANCE;
                    }
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return result;
    }
    
    public List<Object> popSyncStackValsUntil(final Object until) throws SuspendedException {
        List<Object> result = new ArrayList<Object>();
        Object param;
        while (((param = stack.pop()) != until)) {
            result.add(param);
        }
        int l = result.size();
        for (int i = 0; i < l; i++) {
            Object obj = result.get(i);
            if (obj instanceof FutureBean<?>) {
                try {
                    final FutureBean<Object> resFut = (FutureBean<Object>) obj;
                    Pair<Object, ? extends ExecutionException> resultStore = resFut.getResultStore();
                    if (resultStore != null) {
                        result.set(i, FutureBean.processResult(resultStore));
                    } else {
                        stack.push(until);
                        for (int j = l - 1; j >= 0; j--) {
                            stack.push(result.get(j));
                        }
                        suspendedAt = resFut;
                        throw  SuspendedException.INSTANCE;
                    }
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return result;
    }
    
    

    public Object pop() {
        return this.stack.pop();
    }

    public void push(@Nullable final Object obj) {
        this.stack.push(obj);
    }

    public void pushAll(final Object[] objects) {
        this.stack.pushAll(objects);
    }

    public boolean isStackEmpty() {
        return this.stack.isEmpty();
    }

    public Object peek() {
        return this.stack.peek();
    }

    public Object getFromPtr(final int ptr) {
        return this.stack.getFromPtr(ptr);
    }

    public ExecutionContext getSubProgramContext(final Program program, final List<Object> parameters) {
        ExecutionContext ec = new ExecutionContext(this, this.execService, program);
        String[] parameterNames = program.getParameterNames();
        int i = 0;
        for (Object parameter : parameters) {
            ec.memory.put(parameterNames[i++], parameter);
        }
        return ec;
    }

    public Map newMem() {
        try {
            return this.memory.getClass().newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String toString() {
        return "ExecutionContext{" + "execService=" + execService + ", resultCache="
                + resultCache + ", memory=" + memory
                + ", code=" + code + ", ip=" + ip + ", terminated=" + terminated
                + ", stack=" + stack + ", in=" + in
                + ", out=" + out + ", err=" + err + ", parent=" + parent + '}';
    }

}
