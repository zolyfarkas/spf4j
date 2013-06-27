/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

/**
 *
 * @author zoly
 */
public interface Handler<T, E extends Exception> {

    // CHECKSTYLE:OFF -- checkstyle does not seem to handle generic exceptions
    void handle(T object) throws E;
    // CHECKSTYLE:ON
    
}
