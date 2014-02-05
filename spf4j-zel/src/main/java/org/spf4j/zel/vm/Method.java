/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.spf4j.zel.vm;

import java.util.List;

/**
 *
 * @author zoly
 */
public interface Method {

    Object invokeInverseParamOrder(final List<Object> parameters) throws Exception;
    
}
