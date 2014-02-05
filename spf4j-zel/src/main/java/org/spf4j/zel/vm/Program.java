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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.instr.var.OUT;


/**
 * <p>
 * Title: Program</p>
 *
 * @author zoly
 * @version 1.0
 *
 * This is a Turing machine a Program will always be preety much an array of operations.
 */
@Immutable
public final class Program implements Serializable {

    static final long serialVersionUID = 748365748433474932L;

    public enum Type {
        DETERMINISTIC, NONDETERMINISTIC
    };

    private final Type type;
    private final int id; // program ID, unique ID identifying the program

    private final Object[] instructions;
    private final String[] parameterNames;

    /**
     * initializes program with an array of objects.
     *
     * @param objs
     * @param start
     * @param end
     * @param progType
     * @param parameterNames
     */
    public Program(@Nonnull final Object[] objs, @Nonnegative final int start,
            @Nonnegative final int end, final Type progType, final String... parameterNames) {
        int length = end - start;
        instructions = new Object[length];
        System.arraycopy(objs, start, instructions, 0, length);
        this.type = progType;
        id = ProgramBuilder.generateID();
        this.parameterNames = parameterNames;

    }

    @Override
    @CheckReturnValue
    public boolean equals(@Nullable final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Program other = (Program) obj;
        return (this.id == other.id);
    }

    @Override
    @CheckReturnValue
    public int hashCode() {
        return this.id;
    }

    /**
     * @return the instructions
     */
    @CheckReturnValue
    public Object get(final int i) {
        return instructions[i];
    }

    @CheckReturnValue
    Object[] toArray() {
        return instructions.clone();
    }

    @CheckReturnValue
    public int size() {
        return instructions.length;
    }

    /**
     * create a compiled Z Byte Code
     *
     * @param zExpr String
     * @throws com.zoltran.z.vm.ParseException
     * @return Program
     */
    public static Program compile(@Nonnull final String zExpr, @Nonnull final String... varNames)
            throws ParseException {

        CompileContext cc = new CompileContext();
        try {
            ZCompiler.compile(zExpr, cc, varNames);
        } // transform TokenMgrError to ParseException, Errors are bad and
        // I like to reserve them for non-recoverable faults
        catch (TokenMgrError err) {
            ParseException te = new ParseException(err.getMessage());
            te.setStackTrace(err.getStackTrace());
            throw te;
        }
        return cc.getProgramBuilder().toProgram(varNames);

    }

    public Object execute() throws ZExecutionException, InterruptedException {
        return execute(newMem(), System.in, System.out, System.err);
    }

    public Object execute(Object... args) throws ZExecutionException, InterruptedException {
        return execute(newMem(), System.in, System.out, System.err, args);
    }

    /**
     * Execute the program with the provided memory
     *
     * @param memory Map
     * @return Object
     */
    public Object execute(@Nonnull java.util.Map memory, Object... args) throws ZExecutionException, InterruptedException, ExecutionException {
        return execute(memory, System.in, System.out, System.err, args);
    }
    
    private static final HierarchicalMap BUILTINS = new HierarchicalMap(new HashMap<Object, Object>());
    
    static {
        BUILTINS.put("out", OUT.INSTANCE);             
    }
    
    
    /**
     * Execute the program with the provided memory and input / output streams when a exec service is specified this
     * function will return a Future Also it is recomended that a thread safe memory is used in case your functions will
     * modify the memory (not recomended)
     *
     * @param memory Map
     * @param in
     * @param out
     * @param err
     * @param execService
     * @return Object
     * @throws com.zoltran.z.vm.ExecutionException
     */
    public Object execute(@Nonnull final java.util.Map memory, @Nonnull final InputStream in,
            @Nonnull final PrintStream out, @Nonnull final PrintStream err, ThreadPoolExecutor execService, Object... args)
            throws ZExecutionException, InterruptedException {
        HierarchicalMap map = new HierarchicalMap(BUILTINS, memory);
        final ExecutionContext ectx = new ExecutionContext(this, map, in, out, err, execService);
        for (int i = 0; i < args.length; i++) {
            ectx.memory.put(this.parameterNames[i], args[i]);
        }
        return execute(ectx);
    }

    
    
    public static VMExecutor.Suspendable<Object> getCallable(@Nonnull final ExecutionContext ectx) {
        return new VMExecutor.Suspendable<Object>() {

            @Override
            public synchronized Object call() throws ZExecutionException, InterruptedException, VMExecutor.SuspendedException {
                while (!ectx.terminated) {
                    Object code = ectx.code.instructions[ectx.ip];
                    if (code instanceof Instruction) {
                        ((Instruction) code).execute(ectx);
                    } else {
                        ectx.push(ectx.code.instructions[ectx.ip++]);
                    }
                }
                if (!ectx.isStackEmpty()) {
                    return ectx.popSyncStackVal();
                } else {
                    return null;
                }
            }
        };
    }
 
    
    
    public static Future<Object> executeAsync(@Nonnull final ExecutionContext ectx)
            throws ZExecutionException, InterruptedException {
        final VMExecutor.Suspendable<Object> execution = getCallable(ectx);
        if (ectx.execService != null) {
            return ectx.execService.submit(execution);
        } else {
            FutureTask future = new FutureTask(execution);
            future.run();
            return future;
        }
    }

    public static Object execute(@Nonnull final ExecutionContext ectx)
            throws ZExecutionException, InterruptedException {
        try {
            return executeAsync(ectx).get();
        } catch (ExecutionException ex) {
           throw new ZExecutionException(ex);
        }
    }

private static final ThreadPoolExecutor DEF_EXEC = new ThreadPoolExecutor(1, 16,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());    
    
    public Object execute(@Nonnull final java.util.Map memory, @Nonnull final InputStream in,
            @Nonnull final PrintStream out, @Nonnull final PrintStream err, Object... args)
            throws ZExecutionException, InterruptedException {
        return execute(memory, in, out, err, DEF_EXEC, args);
    }

    @CheckReturnValue
    @Nonnull
    public static java.util.Map newMem() {
        //return  (java.util.Map) exampleMem.clone(); // (cannot be used because of java's poor implementation of clone)
        //return new gnu.trove.THashMap(); // (in comparisons vs the java implementations the VM performs 5% slower)
        return new Memory();
    }

    /**
     * get a value of a variable from memory, this function is syntax safe
     *
     * @param mem Map
     * @param name String
     * @throws Exception
     * @return Object
     */
    public static Object getValue(@Nonnull final java.util.Map mem, @Nonnull final String name)
            throws ParseException, ZExecutionException, InterruptedException, ExecutionException {
        return Program.compile(name + ";").execute(mem);
    }

    /**
     * Load a value into memory Have to go through the VM so that the assignement is acurate
     *
     * @param mem
     * @param name String
     * @param value Object
     * @throws net.sf.zel.vm.ParseException
     * @throws net.sf.zel.vm.ZExecutionException
     * @throws java.lang.InterruptedException
     */
    public static void addValue(@Nonnull final java.util.Map mem, @Nonnull final String name,
            final Object value) 
            throws ParseException, ZExecutionException, InterruptedException, ExecutionException {
        Program.compile(name + "=" + value + ";").execute(mem);
    }

    /**
     * build indentation string
     *
     * @param indent
     * @return
     */
    @CheckReturnValue
    public static String strIndent(@Nonnegative final int indent) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            result.append(' ');
        }
        return result.toString();
    }

    /**
     * Output Core, in hierarchical tab indented mode
     *
     * @param name
     * @param mem
     * @param indent
     * @param maxIndent
     * @return
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    public static String dumpCore(final String name, final Object mem, final int indent, final int maxIndent) {
        if (mem == null) {
            return "";
        }
        if (maxIndent > 0 && indent > maxIndent) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        if (mem instanceof java.util.Map) {
            result.append(strIndent(indent)).append(name).append('\n');
            for (Map.Entry<Object, Object> elem : ((Map<Object, Object>) mem).entrySet()) {
                result.append(dumpCore(elem.getKey().toString(), elem.getValue(), indent + 1, maxIndent));
            }
        } else {
            result.append(strIndent(indent)).append(name).append('=').append(mem.toString()).append('\n');
        }
        return result.toString();
    }

    /**
     * *
     * This allows to run ZEL in an interactive mode
     *
     * @param args
     */
    public static void main(final String[] args) throws IOException, ZExecutionException, InterruptedException {
        System.out.println("ZEL Shell");
        boolean terminated = false;
        Memory mem = new Memory();
        InputStreamReader inp = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(inp);
        while (!terminated) {
            System.out.print("zel>");
            String line = br.readLine();
            if (line.toUpperCase().startsWith("QUIT")) {
                terminated = true;
            } else {
                try {
                    Program.compile(line).execute(mem, System.in, System.out, System.err);
                } catch (ParseException ex) {
                    System.out.println("Syntax Error: " + ex.getMessage());
                } catch (ZExecutionException ex) {
                    System.out.println("Execution Error: " + ex.getMessage());
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Program: " + Arrays.toString(instructions);
    }

    /**
     * @return the type
     */
    public Program.Type getType() {
        return type;
    }

    String[] getParameterNames() {
        return parameterNames;
    }

}
