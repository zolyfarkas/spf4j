
package org.spf4j;

/**
 *
 * @author zoly
 */
public class MyTestClass {


    /**
     * example use of a configuration
     */
    private static final String CONFIG = System.getProperty("spf4j.custom.prop", "default");


    public int method() {
        /**
         * another
         */
        return Integer.getInteger("spf4j.custom.prop2", 1);
    }


}
