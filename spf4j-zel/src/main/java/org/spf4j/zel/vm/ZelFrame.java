
package org.spf4j.zel.vm;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author zoly
 */
public final class ZelFrame implements Serializable {


    private static final ConcurrentMap<String, String> SOURCES = new ConcurrentHashMap();

    private static final AtomicInteger IDX = new AtomicInteger();
    private static final long serialVersionUID = 1L;

    public static String newSource(final String sourceDetail) {
        String id = "zel_" + IDX.getAndIncrement();
        SOURCES.put(id, sourceDetail);
        return id;
    }

    public static String getDetail(final String id) {
        return SOURCES.get(id);
    }

    private final String programName;

    private final String source;

    private final int index;

    public ZelFrame(final String programName, final String source, final int index) {
        this.programName = programName;
        this.index = index;
        this.source = source;
    }

    @Override
    public String toString() {
        return programName + "(" + source + ":" + index + ")";
    }

    public String getProgramName() {
        return programName;
    }

    public String getSource() {
        return source;
    }

    public int getIndex() {
        return index;
    }



}
