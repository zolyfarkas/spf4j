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
    public void generateCode(final Object... args) {
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
    public void process(final Object obj) {
    }

    @Override
    public ProgramBuilder getProgramBuilder() {
        return null;
    }

    @Override
    public void generateCodeAll(final ParsingContext parsingContext) {
    }

    @Override
    public NullParsingContext createSubContext() {
        return INSTANCE;
    }

    public static final NullParsingContext INSTANCE = new NullParsingContext();


    @Override
    public void generateCodeAt(final int address, final Object... args) {
    }

}
