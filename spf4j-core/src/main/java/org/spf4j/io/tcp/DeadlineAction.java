/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.io.tcp;

import java.util.Comparator;

/**
 * @author zoly
 */
public final class DeadlineAction {

    public static final Comparator<DeadlineAction> COMPARATOR = new Comparator<DeadlineAction>() {
        @Override
        public int compare(final DeadlineAction o1, final DeadlineAction o2) {
            long result = o1.deadline - o2.deadline;
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        }
    };

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

    @Override
    public String toString() {
        return "DeadlineAction{" + "deadline=" + deadline + ", action=" + action + '}';
    }
}
