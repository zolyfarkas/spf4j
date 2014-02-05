/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.vm;

/**
 *
 * @author zoly
 */
public final class NullParsingContext implements ParsingContext {

    private NullParsingContext() {
    }

    @Override
    public void generateCode(Object... args) {
    }

    /**
     * return the current code address
     *
     * @return
     */
    @Override
    public int getAddress() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void process(Object obj) {
    }

    @Override
    public ProgramBuilder getProgramBuilder() {
        return null;
    }

    @Override
    public void generateCodeAll(ParsingContext parsingContext) {
    }

    @Override
    public NullParsingContext createSubContext() {
        return INSTANCE;
    }

    public static NullParsingContext INSTANCE = new NullParsingContext();


    @Override
    public void generateCodeAt(int address, Object... args) {
    }

}
