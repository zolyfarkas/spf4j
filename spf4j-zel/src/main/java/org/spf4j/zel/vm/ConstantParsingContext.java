/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.spf4j.zel.vm;

import org.spf4j.zel.instr.Instruction;

/**
 *
 * @author zoly
 */
public final class ConstantParsingContext implements ParsingContext
{
    private Object obj;


    @Override
    public void generateCode(Object... args)
    {
      for (Object o : args)
      if (!(o instanceof Instruction))
            obj=o;

    }

    /**
     * return the current code address
     * @return
     */
    @Override
    public int getAddress()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public void process(Object obj)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ProgramBuilder getProgramBuilder()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void generateCodeAll(ParsingContext parsingContext)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ConstantParsingContext createSubContext()
    {
        return this;
    }

    public Object getConstant()
    {
        return obj;
    }

    @Override
    public void generateCodeAt(int address, Object... args)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
