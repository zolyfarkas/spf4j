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
package org.spf4j.jmx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Simple Jmx Client utilities.
 *
 * @author zoly
 */
@SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // FB gets it wrong here
public final class Client {

  private Client() {
  }

  /**
   * get a an attribute from a JMX mbean.
   *
   * @param serviceUrl in the form of: "service:jmx:rmi:///jndi/rmi://:9999/jmxrmi"
   * @param domain - mbean domain name.
   * @param mbeanName - mbean name.
   * @param attribName - attribute name.
   * @return - the attribute value.
   * @throws java.io.IOException - IO issue communicating with mbean.
   * @throws javax.management.InstanceNotFoundException - mbean not found.
   * @throws javax.management.MBeanException - exception while getting the attribute.
   * @throws javax.management.AttributeNotFoundException - attribute not found.
   * @throws javax.management.ReflectionException - mbean reflection exception.
   */
  public static Object getAttribute(@Nonnull final String serviceUrl,
          @Nonnull final String domain, @Nonnull final String mbeanName, @Nonnull final String attribName)
          throws IOException, InstanceNotFoundException,
          MBeanException, AttributeNotFoundException, ReflectionException {
    JMXServiceURL url = new JMXServiceURL(serviceUrl);
    try (JMXConnector jmxc = JMXConnectorFactory.connect(url, null)) {
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      return mbsc.getAttribute(ExportedValuesMBean.createObjectName(domain, mbeanName), attribName);
    }
  }

  public static void setAttribute(@Nonnull final String serviceUrl,
          @Nonnull final String domain, @Nonnull final String mbeanName,
          @Nonnull final String attribName, @Nonnull final Object attribValue)
          throws IOException, InstanceNotFoundException,
          MBeanException, AttributeNotFoundException, ReflectionException, InvalidAttributeValueException {
    JMXServiceURL url = new JMXServiceURL(serviceUrl);
    try (JMXConnector jmxc = JMXConnectorFactory.connect(url, null)) {
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      mbsc.setAttribute(ExportedValuesMBean.createObjectName(domain, mbeanName),
              new Attribute(attribName, attribValue));
    }
  }

  public static Object callOperation(@Nonnull final String serviceUrl,
          @Nonnull final String domain, @Nonnull final String mbeanName, @Nonnull final String operationName,
          final Object... parameters)
          throws IOException, InstanceNotFoundException,
          MBeanException, ReflectionException {
    JMXServiceURL url = new JMXServiceURL(serviceUrl);
    try (JMXConnector jmxc = JMXConnectorFactory.connect(url, null)) {
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      return mbsc.invoke(ExportedValuesMBean.createObjectName(domain, mbeanName),
              operationName, parameters, null);
    }
  }

}
