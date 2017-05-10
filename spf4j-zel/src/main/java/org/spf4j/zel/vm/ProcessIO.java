
package org.spf4j.zel.vm;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * @author zoly
 */
public interface ProcessIO {

  PrintStream getErr();

  InputStream getIn();

  PrintStream getOut();

}
