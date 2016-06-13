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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Pair;
import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.instr.LValRef;
import org.spf4j.zel.instr.var.ARRAY;
import org.spf4j.zel.instr.var.DECODE;
import org.spf4j.zel.instr.var.INT;
import org.spf4j.zel.instr.var.LOG;
import org.spf4j.zel.instr.var.MAX;
import org.spf4j.zel.instr.var.MIN;
import org.spf4j.zel.instr.var.OUT;
import org.spf4j.zel.instr.var.RANDOM;
import org.spf4j.zel.instr.var.SQRT;
import org.spf4j.zel.vm.ParsingContext.Location;
import org.spf4j.zel.vm.gen.ParseException;
import org.spf4j.zel.vm.gen.TokenMgrError;
import org.spf4j.zel.vm.gen.ZCompiler;

/**
 * <p> A ZEL program (function)</p>
 *
 * This is a Turing machine a Program will always be pretty much an array of operations (instructions).
 *
 * @author zoly
 * @version 1.0
 *
 */
@Immutable
public final class Program implements Serializable {

  private static final long serialVersionUID = 748365748433474932L;

  public enum Type {
    DETERMINISTIC, NONDETERMINISTIC
  };

  public enum ExecutionType {
    SYNC,
    ASYNC
  }

  private final Type type;
  private final ExecutionType execType;
  private final int id; // program ID, unique ID identifying the program

  private final Instruction[] instructions;
  private final Location[] debug;
  private final String source;
  private final boolean hasDeterministicFunctions;
  private final Object[] globalMem;
  private final int localMemSize;
  private final Map<String, Integer> localSymbolTable;
  private final Map<String, Integer> globalSymbolTable;
  private final String name;

//CHECKSTYLE:OFF
  Program(final String name, final Map<String, Integer> globalTable, final Object[] globalMem,
          final Map<String, Integer> localTable,
          @Nonnull final Instruction[] objs, final Location[] debug,
          final String source, @Nonnegative final int start,
          @Nonnegative final int end, final Type progType, final ExecutionType execType,
          final boolean hasDeterministicFunctions, final String... parameterNames) throws CompileException {
    //CHECKSTYLE:ON
    this.globalMem = globalMem;
    int length = end - start;
    this.instructions = new Instruction[length];
    System.arraycopy(objs, start, instructions, 0, length);
    this.type = progType;
    this.id = ProgramBuilder.generateID();
    this.execType = execType;
    this.hasDeterministicFunctions = hasDeterministicFunctions;
    this.localSymbolTable = buildLocalSymTable(objs, parameterNames, length, globalTable, localTable);
    this.localMemSize = localSymbolTable.size();
    this.globalSymbolTable = globalTable;
    this.debug = debug;
    this.source = source;
    this.name = name;
  }

  //CHECKSTYLE:OFF
  Program(final String name, final Map<String, Integer> globalTable, final Object[] globalMem,
          final Map<String, Integer> localTable,
          @Nonnull final Instruction[] instructions, final Location[] debug, final String source,
          final Type progType, final ExecutionType execType,
          final boolean hasDeterministicFunctions) {
    //CHECKSTYLE:ON
    this.globalMem = globalMem;
    this.instructions = instructions;
    this.type = progType;
    this.id = ProgramBuilder.generateID();
    this.execType = execType;
    this.hasDeterministicFunctions = hasDeterministicFunctions;
    this.localSymbolTable = localTable;
    this.localMemSize = localSymbolTable.size();
    this.globalSymbolTable = globalTable;
    this.debug = debug;
    this.source = source;
    this.name = name;
  }

  Location[] getDebug() {
    return debug;
  }

  public String getSource() {
    return source;
  }

  public String getName() {
    return name;
  }

  private static Map<String, Integer> buildLocalSymTable(final Instruction[] instructions,
          final String[] parameterNames1,
          final int length, final Map<String, Integer> globalTable,
          final Map<String, Integer> addTo) throws CompileException {
    final int addToSize = addTo.size();
    Map<String, Integer> symbolTable = new HashMap<>(addToSize + parameterNames1.length);
    symbolTable.putAll(addTo);
    // allocate program params
    int i = addToSize;
    for (String param : parameterNames1) {
      Integer existing = symbolTable.put(param, i++);
      if (existing != null) {
        throw new CompileException("Duplicate parameter defined: " + param);
      }
    }
    // allocate variables used in Program
    for (int j = 0; j < length; j++) {
      Instruction code = instructions[j];
      if (code instanceof LValRef) {
        String ref = ((LValRef) code).getSymbol();
        Integer idxr = symbolTable.get(ref);
        if (idxr == null) {
          idxr = globalTable.get(ref);
          if (idxr == null) {
            idxr = i++;
            symbolTable.put(ref, idxr);
          }
        }
      }
    }
    return symbolTable;
  }

  public Map<String, Integer> getGlobalSymbolTable() {
    return globalSymbolTable;
  }

  public Map<String, Integer> getLocalSymbolTable() {
    return localSymbolTable;
  }

  public int getLocalMemSize() {
    return localMemSize;
  }

  Object[] getGlobalMem() {
    return globalMem;
  }

  @Override
  @CheckReturnValue
  public boolean equals(final Object obj) {
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

  public boolean hasDeterministicFunctions() {
    return hasDeterministicFunctions;
  }

  /**
   * @param i - inst address.
   * @return the instruction.
   */
  @CheckReturnValue
  public Instruction get(final int i) {
    return instructions[i];
  }

  @CheckReturnValue
  Object[] toArray() {
    return instructions.clone();
  }

  @CheckReturnValue
  public Instruction[] getCode() {
    return Arrays.copyOf(instructions, instructions.length - 1);
  }

  @CheckReturnValue
  public Location[] getDebugInfo() {
    return Arrays.copyOf(debug, debug.length - 1);
  }

  @CheckReturnValue
  public int size() {
    return instructions.length;
  }

  public ExecutionType getExecType() {
    return execType;
  }

  @Nonnull
  public static Program compile(@Nonnull final String zExpr, @Nonnull final String... varNames)
          throws CompileException {

    ParsingContext cc = new CompileContext(ZEL_GLOBAL_FUNC.copy());
    final String srcId = ZelFrame.newSource(zExpr);
    try {
      ZCompiler.compile(srcId, zExpr, cc);
    } catch (TokenMgrError | ParseException err) {
      throw new CompileException(err);
    }
    Program result = RefOptimizer.INSTANCE.apply(cc.getProgramBuilder().toProgram("anon@root", srcId, varNames));
    ZelFrame.annotate(srcId, result);
    return result;
  }

  static Program compile(@Nonnull final String zExpr,
          final Map<String, Integer> localTable,
          final Object[] globalMem,
          final Map<String, Integer> globalTable,
          @Nonnull final String... varNames)
          throws CompileException {

    ParsingContext cc = new CompileContext(new MemoryBuilder(
            new ArrayList<>(Arrays.asList(globalMem)), globalTable));
    final String srcId = ZelFrame.newSource(zExpr);
    try {
      ZCompiler.compile(srcId, zExpr, cc);
    } catch (TokenMgrError | ParseException err) {
      throw new CompileException(err);
    }
    Program result = cc.getProgramBuilder().toProgram("anon@root", srcId, varNames, localTable);
    ZelFrame.annotate(srcId, result);
    return result;
  }

  public Object execute() throws ExecutionException, InterruptedException {
    return execute(System.in, System.out, System.err);
  }

  public Object execute(final Object... args) throws ExecutionException, InterruptedException {
    return execute(System.in, System.out, System.err, args);
  }

  public Object execute(@Nonnull final ExecutorService execService,
          final Object... args) throws ExecutionException, InterruptedException {
    return execute(new VMExecutor(execService), System.in, System.out, System.err, args);
  }

  public Object executeSingleThreaded(final Object... args) throws ExecutionException, InterruptedException {
    return execute(null, System.in, System.out, System.err, args);
  }

  private static final MemoryBuilder ZEL_GLOBAL_FUNC;

  static {
    ZEL_GLOBAL_FUNC = new MemoryBuilder();
    ZEL_GLOBAL_FUNC.addSymbol("out", OUT.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("sqrt", SQRT.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("int", INT.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("log", LOG.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("log10", LOG.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("min", MIN.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("max", MAX.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("array", ARRAY.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("random", RANDOM.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("channel", Channel.Factory.INSTANCE);
    ZEL_GLOBAL_FUNC.addSymbol("EOF", Channel.EOF);
    ZEL_GLOBAL_FUNC.addSymbol("decode", DECODE.INSTANCE);
  }

  public Object execute(@Nullable final VMExecutor execService,
          @Nullable final InputStream in,
          @Nullable final PrintStream out,
          @Nullable final PrintStream err,
          final Object... args)
          throws ExecutionException, InterruptedException {
    Object[] localMem = allocMem(args);
    final ExecutionContext ectx = new ExecutionContext(this, globalMem, localMem, in, out, err, execService);
    return execute(ectx);
  }

  Object[] allocMem(final Object[] args) {
    Object[] localMem;
    final int lms = this.getLocalMemSize();
    if (args.length == lms) {
      localMem = args;
    } else {
      localMem = new Object[lms];
      System.arraycopy(args, 0, localMem, 0, args.length);
    }
    return localMem;
  }

  public Pair<Object, ExecutionContext> executeX(@Nullable final VMExecutor execService,
          @Nullable final InputStream in,
          @Nullable final PrintStream out,
          @Nullable final PrintStream err,
          final ResultCache resultCache,
          final Object... args)
          throws ExecutionException, InterruptedException {
    Object[] localMem = allocMem(args);
    final ExecutionContext ectx = new ExecutionContext(this, globalMem, localMem,
            resultCache, in, out, err, execService);
    return Pair.of(execute(ectx), ectx);
  }

  public static Object executeSync(@Nonnull final ExecutionContext ectx) throws
          ExecutionException, InterruptedException {
    try {
      return ectx.call();
    } catch (SuspendedException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Object execute(@Nonnull final ExecutionContext ectx)
          throws ExecutionException, InterruptedException {
    Object result = ectx.executeSyncOrAsync();
    if (result instanceof Future) {
      return ((Future<Object>) result).get();
    } else {
      return result;
    }
  }

  public Object execute(@Nonnull final InputStream in,
          @Nonnull final PrintStream out, @Nonnull final PrintStream err, final Object... args)
          throws ExecutionException, InterruptedException {
    if (execType == ExecutionType.SYNC) {
      return execute((VMExecutor) null, in, out, err, args);
    } else {
      return execute(VMExecutor.Lazy.DEFAULT, in, out, err, args);
    }
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
          throws CompileException, InterruptedException, ExecutionException {
    return Program.compile(name + ';').execute(mem);
  }

  /**
   * Load a value into memory Have to go through the VM so that the assignement is acurate
   *
   * @param mem
   * @param name String
   * @param value Object
   * @throws java.lang.InterruptedException
   */
  public static void addValue(@Nonnull final java.util.Map mem, @Nonnull final String name,
          final Object value)
          throws CompileException, InterruptedException, ExecutionException {
    Program.compile(name + '=' + value + ';').execute(mem);
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
      org.spf4j.base.Strings.appendSpaces(result, indent);
      result.append(name).append('\n');
      for (Map.Entry<Object, Object> elem : ((Map<Object, Object>) mem).entrySet()) {
        result.append(dumpCore(elem.getKey().toString(), elem.getValue(), indent + 1, maxIndent));
      }
    } else {
     org.spf4j.base.Strings.appendSpaces(result, indent);
      result.append(name).append('=').append(mem).append('\n');
    }
    return result.toString();
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(Program.class);

  private static volatile boolean terminated = false;
  /**
   * *
   * This allows to run ZEL in an interactive mode
   *
   * @param args
   */
  public static void main(final String[] args) throws IOException, InterruptedException {
    LOGGER.info("ZEL Shell");
    System.out.println("ZEL Shell");
    Map<String, Integer> localSymTable = Collections.emptyMap();
    Pair<Object[], Map<String, Integer>> gmemPair = ZEL_GLOBAL_FUNC.build();
    Map<String, Integer> globalSymTable = gmemPair.getSecond();
    Object[] mem = new Object[]{};
    Object[] gmem = gmemPair.getFirst();
    ResultCache resCache = new SimpleResultCache();
    InputStreamReader inp = new InputStreamReader(System.in, Charsets.UTF_8);
    BufferedReader br = new BufferedReader(inp);
    org.spf4j.base.Runtime.queueHookAtBeginning(new Runnable() {
      @Override
      public void run() {
        terminated = true;
      }
    });
    while (!terminated) {
      System.out.print("zel>");
      String line = br.readLine();
      if (line != null) {
        if ("QUIT".equalsIgnoreCase(line)) {
          terminated = true;
        } else {
          try {
            final Program prog = Program.compile(line, localSymTable, gmem, globalSymTable);
            localSymTable = prog.getLocalSymbolTable();
            globalSymTable = prog.getGlobalSymbolTable();
            gmem = prog.getGlobalMem();
            long startTime = System.nanoTime();
            Pair<Object, ExecutionContext> res = prog.executeX(
                    VMExecutor.Lazy.DEFAULT, System.in, System.out, System.err, resCache, mem);
            long elapsed = System.nanoTime() - startTime;
            final Object result = res.getFirst();
            System.out.println("result>" + result);
            System.out.println("type>" +  (result == null ? "none" : result.getClass()));
            System.out.println("executed in>" + elapsed + " ns");

            final ExecutionContext execCtx = res.getSecond();
            mem = execCtx.getMem();
            resCache = execCtx.getResultCache();
          } catch (CompileException ex) {
            System.out.println("Syntax Error: " + Throwables.getStackTraceAsString(ex));
          } catch (ExecutionException ex) {
            System.out.println("Execution Error: " + Throwables.getStackTraceAsString(ex));
          }
        }
      }
    }
  }

  public String toAssemblyString() {
    StringBuilder result = new StringBuilder();
    result.append("Program: \n");
    for (int i = 0; i < instructions.length; i++) {
      Object obj = instructions[i];
      result.append(Strings.padEnd(Integer.toString(i), 8, ' '));
      result.append(':');
      result.append(obj);
      result.append('\n');
    }
    result.append("execType = ").append(this.execType).append('\n');
    result.append("type = ").append(this.type).append('\n');
    return result.toString();
  }

  @Override
  public String toString() {
    return source;
  }

  /**
   * @return the type
   */
  public Program.Type getType() {
    return type;
  }

  public boolean contains(final Class<? extends Instruction> instr) {
    Boolean res = itterate(new HasClass(instr));
    if (res == null) {
      return false;
    }
    return res;
  }

  @Nullable
  public <T> T itterate(final Function<Object, T> func) {
    for (Instruction code : instructions) {
      T res = func.apply(code);
      if (res != null) {
        return res;
      }
      for (Object param : code.getParameters()) {
        res = func.apply(param);
        if (res != null) {
          return res;
        }
        if (param instanceof Program) {
          res = ((Program) param).itterate(func);
        }
        if (res != null) {
          return res;
        }
      }
    }
    return null;
  }

  Instruction[] getInstructions() {
    return instructions;
  }

  public static final class HasClass implements Function<Object, Boolean> {

    private final Class<? extends Instruction> instr;

    public HasClass(final Class<? extends Instruction> instr) {
      this.instr = instr;
    }

    @Override
    @SuppressFBWarnings("TBP_TRISTATE_BOOLEAN_PATTERN")
    public Boolean apply(@Nonnull final Object input) {
      if (input.getClass() == instr) {
        return Boolean.TRUE;
      }
      return null;
    }
  }

}
