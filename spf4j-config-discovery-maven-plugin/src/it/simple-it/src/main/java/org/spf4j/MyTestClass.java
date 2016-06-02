
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


    public int method2() {
        /**
         * another
         */
        return Integer.getInteger("spf4j.custom.prop2" + 1 + ".bla", 1);
    }


    public void method3() {
        /**
         * another
         */
        for (int i = 0; i < 10; i++) {
          Integer.getInteger("spf4j.custom.prop2" + i + ".bla");
        }
    }

    public static String doSomething() {
      return CONFIG + new MyTestClass().method();
    }

}
