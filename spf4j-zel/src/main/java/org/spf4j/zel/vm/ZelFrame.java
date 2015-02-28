
package org.spf4j.zel.vm;

/**
 *
 * @author zoly
 */
public class ZelFrame {

    private final String programName;

    private final int index;

    public ZelFrame(final String programName, final int index) {
        this.programName = programName;
        this.index = index;
    }

    @Override
    public String toString() {
        return programName + ":" + index;
    }

}
