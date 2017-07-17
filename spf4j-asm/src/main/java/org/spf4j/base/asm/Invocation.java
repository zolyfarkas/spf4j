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
package org.spf4j.base.asm;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author zoly
 */
public final class Invocation {

    private final String caleeClassName;
    private final String caleeMethodName;
    private final String caleeMethodDesc;
    private final String caleeSource;
    private final int caleeLine;
    private final Object[] parameters;
    private final Method invokedMethod;

    public Invocation(final String cName, final String mName, final String mDesc,
            final Object[] parameters, final String src, final int ln, final Method invokedMethod) {
        caleeClassName = cName;
        caleeMethodName = mName;
        caleeMethodDesc = mDesc;
        this.parameters = parameters.clone();
        caleeSource = src;
        caleeLine = ln;
        this.invokedMethod = invokedMethod;
    }

    public String getCaleeClassName() {
        return caleeClassName;
    }

    public String getCaleeMethodName() {
        return caleeMethodName;
    }

    public String getCaleeMethodDesc() {
        return caleeMethodDesc;
    }

    public String getCaleeSource() {
        return caleeSource;
    }

    public int getCaleeLine() {
        return caleeLine;
    }

    public Method getInvokedMethod() {
        return invokedMethod;
    }

    public Object[] getParameters() {
        return parameters.clone();
    }

    @Override
    public String toString() {
        return "Invocation{" + "caleeClassName=" + caleeClassName + ", caleeMethodName="
                + caleeMethodName + ", caleeMethodDesc=" + caleeMethodDesc + ", caleeSource="
                + caleeSource + ", caleeLine=" + caleeLine + ", parameters=" + Arrays.toString(parameters)
                + ", invokedMethod=" + invokedMethod + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.caleeClassName);
        return 97 * hash + Objects.hashCode(this.caleeMethodName);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Invocation other = (Invocation) obj;
        if (this.caleeLine != other.caleeLine) {
            return false;
        }
        if (!Objects.equals(this.caleeClassName, other.caleeClassName)) {
            return false;
        }
        if (!Objects.equals(this.caleeMethodName, other.caleeMethodName)) {
            return false;
        }
        if (!Objects.equals(this.caleeMethodDesc, other.caleeMethodDesc)) {
            return false;
        }
        if (!Objects.equals(this.caleeSource, other.caleeSource)) {
            return false;
        }
        if (!Arrays.deepEquals(this.parameters, other.parameters)) {
            return false;
        }
        return Objects.equals(this.invokedMethod, other.invokedMethod);
    }

}
