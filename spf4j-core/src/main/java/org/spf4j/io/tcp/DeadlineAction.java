/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
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

    @Override
    public String toString() {
        return "DeadlineAction{" + "deadline=" + deadline + ", action=" + action + '}';
    }
}
