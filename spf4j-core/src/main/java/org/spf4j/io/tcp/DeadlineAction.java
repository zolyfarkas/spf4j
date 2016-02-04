package org.spf4j.io.tcp;

import java.util.Comparator;

/**
 *
 * @author zoly
 */
public final class DeadlineAction {

    private final long deadline;

    private final Runnable action;

    public DeadlineAction(final long deadline, final Runnable action) {
        this.deadline = deadline;
        this.action = action;
    }

    public long getDeadline() {
        return deadline;
    }

    public Runnable getAction() {
        return action;
    }

    public static final Comparator<DeadlineAction> COMPARATOR = new Comparator<DeadlineAction>() {
        @Override
        public int compare(final DeadlineAction o1, final DeadlineAction o2) {
            if (o1.deadline < o2.deadline) {
                return -1;
            } else if (o1.deadline > o2.deadline) {
                return 1;
            } else {
                return 0;
            }
        }
    };

}
