/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.vm;

/**
 *
 * @author zoly
 */
public final class ExecAbortException extends RuntimeException {

    public ExecAbortException() {
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    public static final ExecAbortException INSTANCE = new ExecAbortException();

}
