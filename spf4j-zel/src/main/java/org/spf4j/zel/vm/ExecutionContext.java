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
package org.spf4j.zel.vm;

import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Either;
import org.spf4j.base.Throwables;
import org.spf4j.ds.SimpleStackNullSupport;
import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.operators.Operator;
import static org.spf4j.zel.vm.Program.ExecutionType.SYNC;

/**
 * Virtual Machine Execution Context
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class ExecutionContext implements VMExecutor.Suspendable<Object> {

  public static final Object VOID = new Object() {
    @Override
    public String toString() {
      return "VOID";
    }

  };

  private final Object[] tuple = new Object[2];

  @Nonnull
  private MathContext mathContext;

  private final VMExecutor execService;

  private final ResultCache resultCache;

  private final Object[] mem;

  private final Object[] globalMem;

  /**
   * the program
   */
  private final Program code;

  /**
   * The Instruction pointer
   */
  private int ip;

  /**
   * The main stack
   */
  private final SimpleStackNullSupport<Object> stack;

  /**
   * Standard Input
   */
  private final transient ProcessIO io;

  private List<VMFuture<Object>> suspendedAt;

  private final boolean isChildContext;

  private ExecutionContext(final ExecutionContext parent, @Nullable final VMExecutor service,
          final Program program, final Object[] localMem) {
    this(program, parent.globalMem, localMem, parent.resultCache, parent.io, service, true);
  }

  /**
   * additional constructor that allows you to set the standard Input/Output streams
   *
   * @param program
   * @param in
   * @param out
   * @param err
   */
  ExecutionContext(final Program program, final Object[] globalMem,
          @Nullable final ProcessIO io,
          @Nullable final VMExecutor execService) {
    this(program, globalMem, new Object[program.getLocalMemSize()],
            program.hasDeterministicFunctions() ? new SimpleResultCache() : null,
            io, execService);
  }

  public ExecutionContext(final Program program, final Object[] globalMem, final Object[] localMem,
          @Nullable final ProcessIO io,
          @Nullable final VMExecutor execService) {
    this(program, globalMem, localMem,
            program.hasDeterministicFunctions() ? new SimpleResultCache() : null,
            io, execService);
  }

  ExecutionContext(final Program program, final Object[] globalMem, final Object[] localMem,
          @Nullable final ResultCache resultCache,
          @Nullable final ProcessIO io,
          @Nullable final VMExecutor execService) {
    this(program, globalMem, localMem, resultCache, io, execService, false);
  }

  ExecutionContext(final Program program, final Object[] globalMem, final Object[] localMem,
          @Nullable final ResultCache resultCache,
          @Nullable final ProcessIO io,
          @Nullable final VMExecutor execService, final boolean isChildContext) {
    this.code = program;
    this.io = io;
    this.execService = execService;
    this.stack = new SimpleStackNullSupport<>(8);
    this.ip = 0;
    this.mem = localMem;
    this.globalMem = globalMem;
    this.resultCache = resultCache;
    this.isChildContext = isChildContext;
    this.mathContext = MathContext.DECIMAL128;
  }

  public ProcessIO getIo() {
    return io;
  }

  public JavaMethodCall newJavaCall(final Object object, final String method) {
    if (object instanceof Class && !"getName".equals(method)) {
      throw new UnsupportedOperationException("Not allowed invoking methods on class: " + object);
    }
    return new JavaMethodCall(object, method);
  }


  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Object[] getMem() {
    return mem;
  }

  public void globalPoke(final int addr, final Object value) {
    globalMem[addr] = value;
  }

  public void localPoke(final int addr, final Object value) {
    mem[addr] = value;
  }

  public Object localPeek(final int addr) {
    return mem[addr];
  }

  public Object globalPeek(final int addr) {
    return globalMem[addr];
  }

  public Program getProgram() {
    return code;
  }

  public void incrementInstructionPointer() {
    ip++;
  }

  public void terminate() {
    ip =  code.size();
  }

  // TODO: Need to employ Either here
  @SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
  public Object executeSyncOrAsync()
          throws ExecutionException, InterruptedException {
    if (this.execService != null && this.code.getExecType() == Program.ExecutionType.ASYNC) {
      if (this.isChildContext()) {
        return this.execService.submitInternal(VMExecutor.synchronize(this));
      } else {
        return this.execService.submit(VMExecutor.synchronize(this));
      }
    } else {
      try {
        return this.call();
      } catch (SuspendedException ex) {
        throw new ExecutionException("Suspending not supported in current context: " + this, ex);
      }
    }
  }

  @SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
  // TODO: Need to employ Either here
  public Object executeAsync()
          throws ExecutionException, InterruptedException {
    if (this.execService != null) {
      if (this.isChildContext()) {
        return this.execService.submitInternal(VMExecutor.synchronize(this));
      } else {
        return this.execService.submit(VMExecutor.synchronize(this));
      }
    } else {
      try {
        return this.call();
      } catch (SuspendedException ex) {
        throw new ExecutionException("Suspending not supported for " + this, ex);
      }
    }
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void suspend(final VMFuture<Object> future) throws SuspendedException {
    suspendedAt = Collections.singletonList(future);
    throw SuspendedException.INSTANCE;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void suspend(final List<VMFuture<Object>> futures) throws SuspendedException {
    suspendedAt = futures;
    throw SuspendedException.INSTANCE;
  }


  @Override
  public Object call()
          throws ExecutionException, InterruptedException, SuspendedException {
    suspendedAt = null;
    Operator.MATH_CONTEXT.set(getMathContext());
    Instruction[] instructions = code.getInstructions();
    int l = instructions.length;
    try {
      while (ip < l) {
        Instruction icode = instructions[ip];
        ip += icode.execute(ExecutionContext.this);
      }
      if (!isStackEmpty()) {
        Object result = popSyncStackVal();
        syncStackVals();
        return result;
      } else {
        return VOID;
      }
    } catch (SuspendedException | InterruptedException e) {
      throw e;
    } catch (ZExecutionException e) {
      ParsingContext.Location[] debug = code.getDebug();
      if (debug != null) {
        if (ip >= debug.length) {
          // program finished successfully.
          // Exception thrown in sync stack evals.
          e.addZelFrame(new ZelFrame(code.getName(), code.getSource(),
                debug[debug.length - 1].getRow()));
        } else {
          e.addZelFrame(new ZelFrame(code.getName(), code.getSource(),
                debug[ip].getRow()));
        }
      }
      throw e;
    }
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public List<VMFuture<Object>> getSuspendedAt() {
    return suspendedAt;
  }


  /**
   * pops object out of stack
   *
   * @return Object
   */
  public Object popSyncStackVal() throws SuspendedException, ExecutionException {
    Object result = this.stack.peek();
    if (result instanceof VMFuture<?>) {
      final VMFuture<Object> resFut = (VMFuture<Object>) result;
      Either<Object, ? extends ExecutionException> resultStore = resFut.getResult();
      if (resultStore != null) {
        this.stack.remove();
        return Either.processResult(resultStore);
      } else {
        suspend(resFut);
        throw new IllegalThreadStateException();
      }
    } else {
      this.stack.remove();
      return result;
    }
  }

  public Object peekSyncStackVal() throws SuspendedException, ExecutionException {
    Object result = this.stack.peek();
    if (result instanceof VMFuture<?>) {
      final VMFuture<Object> resFut = (VMFuture<Object>) result;
      Either<Object, ? extends ExecutionException> resultStore = resFut.getResult();
      if (resultStore != null) {
        return Either.processResult(resultStore);
      } else {
        suspend(resFut);
        throw new IllegalThreadStateException();
      }
    } else {
      return result;
    }
  }

  public void syncStackVal() throws SuspendedException, ExecutionException {
    Object result = this.stack.peek();
    if (result instanceof VMFuture<?>) {
      final VMFuture<Object> resFut = (VMFuture<Object>) result;
      Either<Object, ? extends ExecutionException> resultStore = resFut.getResult();
      if (resultStore == null) {
        suspend(resFut);
        throw new IllegalThreadStateException();
      } else {
        this.stack.replaceFromTop(0, Either.processResult(resultStore));
      }
    }
  }

  public void syncStackVals() throws SuspendedException, ExecutionException {
    for (int i = 0; i < stack.size(); i++) {
      Object result = this.stack.peekFromTop(i);
      if (result instanceof VMFuture<?>) {
        final VMFuture<Object> resFut = (VMFuture<Object>) result;
        Either<Object, ? extends ExecutionException> resultStore = resFut.getResult();
        if (resultStore == null) {
          suspend(resFut);
          throw new IllegalThreadStateException();
        } else {
          this.stack.replaceFromTop(i, Either.processResult(resultStore));
        }
      }
    }
  }

  public Object[] popStackVals(final int nvals) {
    return stack.pop(nvals);
  }

  public void popStackVals(final Object[] to, final int nvals) {
    stack.popTo(to, nvals);
  }

  public Object popStackVal() {
    return stack.pop();
  }

  public int getNrStackVals() {
    return stack.size();
  }

  public Object[] popSyncStackVals(final int nvals) throws SuspendedException, ExecutionException {
    if (nvals == 0) {
      return org.spf4j.base.Arrays.EMPTY_OBJ_ARRAY;
    }
    Object[] result = new Object[nvals];
    popSyncStackVals(result);
    return result;
  }

  @SuppressFBWarnings
  public Object[] tuple() {
    return tuple;
  }

  public void popSyncStackVals(final Object[] vals) throws SuspendedException, ExecutionException {
    final int l = vals.length;
    popSyncStackVals(vals, l);
  }

  public void popSyncStackVals(final Object[] vals, final int l)
          throws ExecutionException, SuspendedException {
    for (int i = 0, j = l - 1; i < l; i++, j--) {
      Object obj = stack.peekFromTop(i);
      if (obj instanceof VMFuture<?>) {
        final VMFuture<Object> resFut = (VMFuture<Object>) obj;
        Either<Object, ? extends ExecutionException> resultStore = resFut.getResult();
        if (resultStore != null) {
          final Object processResult = Either.processResult(resultStore);
          stack.replaceFromTop(i, processResult);
          vals[j] = processResult;
        } else {
          suspend(resFut);
          throw new IllegalStateException();
        }
      } else {
        vals[j] = obj;
      }
    }
    stack.removeFromTop(l);
  }

  public Object popFirstAvail(final int nr) throws SuspendedException {
    int nrErrors = 0;
    ExecutionException e = null;
    List<VMFuture<Object>> futures = null;
    for (int i = 0; i < nr; i++) {
      Object obj = stack.peekFromTop(i);
      if (obj instanceof VMFuture<?>) {
        final VMFuture<Object> resFut = (VMFuture<Object>) obj;
        Either<Object, ? extends ExecutionException> resultStore = resFut.getResult();
        if (resultStore != null) {
          if (resultStore.isLeft()) {
            stack.removeFromTop(nr);
            return resultStore.getLeft();
          } else {
            nrErrors++;
            ExecutionException exRes = resultStore.getRight();
            if (e != null) {
              Throwables.suppressLimited(exRes, e);
            }
            e = exRes;
          }
        } else {
          if (futures == null) {
            futures = new ArrayList<>(nr);
          }
          futures.add(resFut);
        }
      } else {
        stack.removeFromTop(nr);
        return obj;
      }
    }
    if (nrErrors == nr) {
      if (e == null) {
        throw new IllegalStateException();
      } else {
        throw new UncheckedExecutionException(e);
      }
    }
    if (futures == null || futures.isEmpty()) {
      throw new IllegalStateException();
    }
    suspend(futures);
    throw new IllegalStateException();
  }

  public Object pop() {
    return this.stack.pop();
  }

  public void push(@Nullable final Object obj) {
    this.stack.push(obj);
  }

  public void pushNull() {
    this.stack.pushNull();
  }


  public boolean isStackEmpty() {
    return this.stack.isEmpty();
  }

  public Object peek() {
    return this.stack.peek();
  }

  public Object peekFromTop(final int n) {
    return this.stack.peekFromTop(n);
  }


  public ExecutionContext getSubProgramContext(final Program program, final int nrParams)
          throws ExecutionException, SuspendedException {
    Object[] localMem = new Object[program.getLocalMemSize()];
    if (program.getExecType() == SYNC) {
      this.popSyncStackVals(localMem, nrParams);
      return new ExecutionContext(this, null, program, localMem);
    } else {
      this.popStackVals(localMem, nrParams);
      return new ExecutionContext(this, this.execService, program, localMem);
    }
  }

  public ExecutionContext getSyncSubProgramContext(final Program program, final int nrParams)
          throws ExecutionException, SuspendedException {
    Object[] localMem = new Object[program.getLocalMemSize()];
    this.popSyncStackVals(localMem, nrParams);
    return new ExecutionContext(this, null, program, localMem);
  }

  public ExecutionContext getSyncSubProgramContext(final Program program, final Object[] parameters) {
    Object[] localMem = program.allocMem(parameters);
    return new ExecutionContext(this, null, program, localMem);
  }


  @Override
  public String toString() {
    return "ExecutionContext{" + "execService=" + getExecService() + ",\nresultCache="
            + getResultCache() + ",\nmemory=" + Arrays.toString(mem)
            + ",\nlocalSymbolTable=" + code.getLocalSymbolTable()
            + ",\nglobalMem=" + Arrays.toString(globalMem)
            + ",\nglobalSymbolTable=" + code.getGlobalSymbolTable()
            + ",\ncode=" + code + ", ip=" + ip
            + ",\nstack=" + stack + ", io=" + io + '}';
  }

  public boolean isChildContext() {
    return isChildContext;
  }

  /**
   * @return the mathContext
   */
  @Nonnull
  public MathContext getMathContext() {
    return mathContext;
  }

  /**
   * @param mathContext the mathContext to set
   */
  public void setMathContext(@Nonnull final MathContext mathContext) {
    this.mathContext = mathContext;
  }

  /**
   * @return the execService
   */
  @Nullable
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public VMExecutor getExecService() {
    return execService;
  }

  /**
   * @return the resultCache
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public ResultCache getResultCache() {
    return resultCache;
  }

}
