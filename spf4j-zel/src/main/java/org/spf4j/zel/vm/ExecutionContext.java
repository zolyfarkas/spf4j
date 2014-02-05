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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.spf4j.concurrent.FutureBean;

/**
 * Virtual Machine Execution Context
 *
 * @author zoly
 */
public final class ExecutionContext {

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
    public InputStream in = null;

    /**
     * Standard Output
     */
    public PrintStream out = null;

    /**
     * Standard Error Output
     */
    public PrintStream err = null;

    /**
     * The parent Execution Context
     */
    public final ExecutionContext parent;

    private ExecutionContext(final ExecutionContext parent, final VMExecutor service, final Program program) {
        this.in = parent.in;
        this.out = parent.out;
        this.err = parent.err;
        this.memory = new HierarchicalMap(parent.memory, new HashMap());
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
            final InputStream in, final PrintStream out, final PrintStream err, final ExecutorService execService) {
        this.code = program;
        this.memory = new HierarchicalMap(memory);
        this.in = in;
        this.out = out;
        this.err = err;
        this.execService = new VMExecutor(execService);
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
    public Object popSyncStackVal() throws VMExecutor.SuspendedException {
        Object result = this.stack.pop();
        if (result instanceof FutureBean<?>) {
            try {
                final FutureBean<Object> resFut = (FutureBean<Object>) result;
                if (resFut.isDone()) {
                    return (resFut).get();
                } else {
                    this.stack.push(result);
                    throw new VMExecutor.SuspendedException(resFut);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return result;
        }
    }

    public Object[] popSyncStackVals(int nvals) throws VMExecutor.SuspendedException {
        Object[] result = new Object[nvals];
        for (int i = 0; i < nvals; i++) {
            result[i] = stack.pop();
        }
        for (int i = 0; i < nvals; i++) {
            Object obj = result[i];
            if (obj instanceof FutureBean<?>) {
                try {
                    final FutureBean<Object> resFut = (FutureBean<Object>) obj;
                    if (resFut.isDone()) {
                        result[i] = resFut.get();
                    } else {
                        for (int j = nvals - 1; j >= 0; j--) {
                            stack.push(result[j]);
                        }
                        throw new VMExecutor.SuspendedException(resFut);
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return result;
    }
    
    public List<Object> popSyncStackValsUntil(final Object until) throws VMExecutor.SuspendedException {
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
                    if (resFut.isDone()) {
                        result.set(i, resFut.get());
                    } else {
                        stack.push(until);
                        for (int j = l - 1; j >= 0; j--) {
                            stack.push(result.get(j));
                        }
                        throw new VMExecutor.SuspendedException(resFut);
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
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

    public void push(final Object obj) {
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
