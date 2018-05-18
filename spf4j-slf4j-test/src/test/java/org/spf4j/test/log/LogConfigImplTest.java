/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.test.log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Zoltan Farkas
 */
public class LogConfigImplTest {


  @Test
  public void testLogConfigImpl() {
    LogPrinter h1 = new LogPrinter(Level.INFO);
    LogPrinter h2 = new LogPrinter(Level.DEBUG);
    LogPrinter h3 = new LogPrinter(Level.DEBUG);
    LogPrinter h4 = new LogPrinter(Level.ERROR);
    LogPrinter h5 = new LogPrinter(Level.DEBUG);
    LogConfigImpl cfg = new LogConfigImpl(ImmutableList.of(h1),
            ImmutableMap.of("a.b", Collections.singletonList(h2),
            "a.c", Collections.singletonList(h3),
            "c.d", Collections.singletonList(h4)));
    LogConsumer handler = cfg.getLogConsumer("a", Level.DEBUG);
    Assert.assertNull(handler);
    List<LogHandler> handlers = ((CompositeLogHandler) cfg.getLogConsumer("a", Level.INFO)).logHandlers;
    Assert.assertSame(h1, handlers.get(0));
    handlers = ((CompositeLogHandler) cfg.getLogConsumer("a.b", Level.INFO)).logHandlers;
    Assert.assertSame(h2, handlers.get(0));
    Assert.assertSame(h1, handlers.get(1));
    cfg = cfg.add("a", h5);
    handlers = ((CompositeLogHandler) cfg.getLogConsumer("a.b", Level.INFO)).logHandlers;
    Assert.assertSame(h2, handlers.get(0));
    Assert.assertSame(h5, handlers.get(1));
    Assert.assertSame(h1, handlers.get(2));
    cfg = cfg.remove("a", h5);
    handlers = ((CompositeLogHandler) cfg.getLogConsumer("a.b", Level.INFO)).logHandlers;
    Assert.assertSame(h2, handlers.get(0));
    Assert.assertSame(h1, handlers.get(1));
  }

}
