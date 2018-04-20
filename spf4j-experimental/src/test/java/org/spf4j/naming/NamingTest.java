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
package org.spf4j.naming;

import com.twitter.finagle.Addr;
import com.twitter.finagle.Dentry;
import com.twitter.finagle.Dtab;
import com.twitter.finagle.DtabBuilder;
import com.twitter.finagle.Name;
import com.twitter.finagle.Namer;
import com.twitter.finagle.Path;
import com.twitter.finagle.Resolver;
import com.twitter.finagle.Resolvers;
import com.twitter.util.Var;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class NamingTest {


  @Test
  public void testNaming() {
    Name n1 = Resolvers.eval("/s/a/b/c");
    Name n2 = Resolvers.eval("/s/a/b/c");
    Assert.assertEquals(n1, n2);
    DtabBuilder builder = new DtabBuilder();
    builder.$plus$eq(Dentry.read("/s/a => /$/inet/localhost/8080"));
    Dtab dtab = builder.result();
    Var<Addr.Bound> addr = (Var) Namer.resolve(dtab, Path.read("/s/a/b/c"));
    System.out.println(addr.sample().addrs().seq().iterator().next());
  }


}
