
package org.spf4j.zel.vm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.spf4j.base.CharSequences;
import org.spf4j.zel.instr.Instruction;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("CD_CIRCULAR_DEPENDENCY")
public final class ZelFrame implements Serializable {


    private static final ConcurrentMap<String, CharSequence> SOURCES = new ConcurrentHashMap();

    private static final AtomicInteger IDX = new AtomicInteger();
    private static final long serialVersionUID = 1L;

    public static String newSource(final String sourceDetail) {
        String id = "zel_" + IDX.getAndIncrement();
        SOURCES.put(id, CharSequences.toLineNumbered(0, sourceDetail));
        return id;
    }

    public static void annotate(final String id, final Program program) {
      CharSequence source = SOURCES.get(id);
      String[] lines = source.toString().split("\n");
      StringBuilder[] annotations = new StringBuilder[lines.length];
      Instruction[] instructions = program.getInstructions();
      ParsingContext.Location[] debug = program.getDebug();
      for (int i = 0; i < instructions.length; i++) {
        ParsingContext.Location loc = debug[i];
        int lineNr = loc.getRow();
        StringBuilder existing = annotations[lineNr];
        if (existing == null) {
          existing = new StringBuilder();
          existing.append(" /* ").append(instructions[i]);
          annotations[lineNr] = existing;
        } else {
          existing.append(';').append(instructions[i]);
        }
      }
      StringBuilder result = new StringBuilder(source.length() * 2);
      for (int i = 0; i < lines.length; i++) {
        result.append(lines[i]);
        StringBuilder annotation = annotations[i];
        if (annotation != null) {
          result.append(annotation).append(" */\n");
        } else {
          result.append('\n');
        }
      }
      SOURCES.put(id, result);
    }

    public static CharSequence getDetail(final String id) {
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
        return programName + '(' + source + ':' + index + ')';
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
