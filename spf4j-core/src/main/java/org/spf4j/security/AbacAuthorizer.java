/*
 * Copyright 2019 SPF4J.
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

public interface AbacAuthorizer {

  /**
   * Attribute bases access control, see https://en.wikipedia.org/wiki/Attribute-based_access_control
   * @param user the user requesting access.
   * @param resource  the properties of the accessed resource.
   * @param action the properties of the action attempted.
   * @param env environment params.
   * @return
   */
  boolean canAccess(Principal user,  Properties resource, Properties action, Properties env);

  AbacAuthorizer NO_ACCESS = new AbacAuthorizer() {
    @Override
    public boolean canAccess(final Principal user, final Properties resource,
            final Properties action, final Properties env) {
      return false;
    }
  };

  AbacAuthorizer ALL_ACCESS = new AbacAuthorizer() {
    @Override
    public boolean canAccess(final Principal user, final Properties resource,
            final Properties action, final Properties env) {
      return true;
    }
  };

}
