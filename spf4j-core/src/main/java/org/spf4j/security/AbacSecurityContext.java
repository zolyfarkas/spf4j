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
package org.spf4j.security;

import java.security.Principal;
import java.util.Properties;

/**
 * @author Zoltan Farkas
 */
public interface AbacSecurityContext extends RbacSecurityContext {

  static AbacSecurityContext from(final RbacSecurityContext rbac, final AbacAuthorizer abacAuth) {
    return new AbacSecurityContext() {
      @Override
      public boolean canAccess(final Properties resource, final Properties action, final Properties env) {
        return abacAuth.canAccess(getUserPrincipal(), resource, action, env);
      }

      @Override
      public Principal getUserPrincipal() {
        return rbac.getUserPrincipal();
      }

      @Override
      public boolean isUserInRole(final String role) {
        return rbac.isUserInRole(role);
      }
    };
  }

  /**
   * Attribute bases access control, see https://en.wikipedia.org/wiki/Attribute-based_access_control
   * @param resource  the properties of the accessed resource.
   * @param action the properties of the action attempted.
   * @param env environment params.
   * @return
   */
  boolean canAccess(Properties resource, Properties action, Properties env);

  AbacSecurityContext NOAUTH = new AbacSecurityContext() {
    @Override
    public Principal getUserPrincipal() {
      return null;
    }

    @Override
    public boolean isUserInRole(final String role) {
      return false;
    }

    @Override
    public boolean canAccess(final Properties resource, final Properties action, final Properties env) {
      return false;
    }
  };

}
