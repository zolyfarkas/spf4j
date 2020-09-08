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
package org.spf4j.avro.calcite;

import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.CoreRules;

/**
 *
 * @author Zoltan Farkas
 */
public final class PlannerUtils {

  private PlannerUtils() { }

  public static RelNode pushDownPredicatesAndProjection(final RelNode rootRel) {
    final HepProgram hepProgram = new HepProgramBuilder()
        //push down predicates
        .addRuleInstance(CoreRules.FILTER_INTO_JOIN)
        //push down projections
        .addRuleInstance(CoreRules.PROJECT_JOIN_TRANSPOSE)
        .addRuleInstance(CoreRules.FILTER_PROJECT_TRANSPOSE)
            .build();
    final HepPlanner planner = new HepPlanner(hepProgram);
    planner.setRoot(rootRel);
    return planner.findBestExp();
  }

}
