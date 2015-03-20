
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

    private Client() { }

    /**
     *
     * @param serviceUrl "service:jmx:rmi:///jndi/rmi://:9999/jmxrmi"
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
            final Object ... parameters)
            throws IOException, InstanceNotFoundException,
            MBeanException, AttributeNotFoundException, ReflectionException {
        JMXServiceURL url = new JMXServiceURL(serviceUrl);
        try (JMXConnector jmxc = JMXConnectorFactory.connect(url, null)) {
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
            return mbsc.invoke(ExportedValuesMBean.createObjectName(domain, mbeanName),
                    operationName, parameters, null);
        }
    }


}
