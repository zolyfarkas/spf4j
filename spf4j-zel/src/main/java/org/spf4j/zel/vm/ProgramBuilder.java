/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.vm;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author zoly
 */
public final class ProgramBuilder {

    private static final int DEFAULT_SIZE = 16;


    private Object[] instructions;

    private int instrNumber;

    private Program.Type type;
    
    private Program.ExecutionType execType;

    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static int generateID() {
        return COUNTER.getAndIncrement();
    }
    /**
     * initializes the program
     */
    public ProgramBuilder() {
        instructions = new Object[DEFAULT_SIZE];
        instrNumber = 0;
        type = Program.Type.NONDETERMINISTIC;
        execType = Program.ExecutionType.ASYNC;
    }

    /**
     * initializes the program with the provided size
     *
     * @param size
     */
    public ProgramBuilder(final int size) {
        instructions = new Object[16];
        instrNumber = 0;
        this.type = Program.Type.NONDETERMINISTIC;
    }

    /**
     * @return the type
     */
    public Program.Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(final Program.Type type) {
        this.type = type;
    }
    
    public void setExecType(final Program.ExecutionType execType) {
        this.execType = execType;
    }
    
    

    public void add(final Object object) {
        ensureCapacity(instrNumber + 1);
        instructions[instrNumber++] = object;
    }

    public void set(final int idx, final Object object) {
        ensureCapacity(idx + 1);
        instructions[idx] = object;
        instrNumber = Math.max(idx + 1, instrNumber);
    }

    public void addAll(final Object[] objects) {
        ensureCapacity(instrNumber + objects.length);
        System.arraycopy(objects, 0, instructions, instrNumber, objects.length);
        instrNumber += objects.length;
    }

    public void setAll(final int idx, final Object[] objects) {
        ensureCapacity(idx + objects.length);
        System.arraycopy(objects, 0, instructions, idx, objects.length);
        instrNumber = Math.max(objects.length + idx, instrNumber);
    }

    public void addAll(final ProgramBuilder opb) {
        ensureCapacity(instrNumber + opb.instrNumber);
        System.arraycopy(opb.instructions, 0, instructions, instrNumber, opb.instrNumber);
        instrNumber += opb.instrNumber;
    }

    /**
     * Increases the capacity of this <tt>ArrayList</tt> instance, if necessary, to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    public void ensureCapacity(final int minCapacity) {
        int oldCapacity = instructions.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            instructions = Arrays.copyOf(instructions, newCapacity);
        }
    }

    public int size() {
        return instrNumber;
    }

    public Object[] toArray() {
        return Arrays.copyOf(instructions, instrNumber);
    }

    public Program toProgram(final String[] parameterNames) {
        return new Program(instructions, 0, instrNumber, type, execType, parameterNames);
    }
    
    
    public Program toProgram(final List<String> parameterNames) {
        return new Program(instructions, 0, instrNumber, type, execType,
                parameterNames.toArray(new String[parameterNames.size()]));
    }

}
