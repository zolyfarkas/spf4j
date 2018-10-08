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
package org.spf4j.maven;

import java.util.List;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

/**
 * @author Zoltan Farkas
 */
public final class ClassifierDependencyFilter implements DependencyFilter {

  private final String classifier;

  public ClassifierDependencyFilter(final String classifier) {
    this.classifier = classifier;
  }

  @Override
  public boolean accept(final DependencyNode node, final List<DependencyNode> parents) {
    return classifier.equals(node.getArtifact().getClassifier());
  }

  @Override
  public String toString() {
    return "ClassifierDependencyFilter{" + "classifier=" + classifier + '}';
  }


}
