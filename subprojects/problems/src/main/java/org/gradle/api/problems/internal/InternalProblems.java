/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.api.problems.internal;

import org.gradle.api.problems.Problems;
import org.gradle.api.problems.interfaces.Problem;
import org.gradle.api.problems.interfaces.ProblemBuilder;
import org.gradle.api.problems.interfaces.ProblemBuilderDefiningMessage;
import org.gradle.api.problems.interfaces.ReportableProblem;
import org.gradle.internal.operations.BuildOperationProgressEventEmitter;

import java.util.Collection;

import static java.util.Collections.singleton;

public abstract class InternalProblems extends Problems {

    private final BuildOperationProgressEventEmitter buildOperationProgressEventEmitter;

    public InternalProblems(BuildOperationProgressEventEmitter buildOperationProgressEventEmitter) {
        this.buildOperationProgressEventEmitter = buildOperationProgressEventEmitter;
    }

    protected ProblemBuilderDefiningMessage create() {
        return createProblemBuilder();
    }

    protected RuntimeException throwing(ProblemBuilder problem, RuntimeException cause) {
        problem.cause(cause);
        return throwing(singleton(problem.build()), cause);
    }

    protected RuntimeException throwing(Collection<ReportableProblem> problems, RuntimeException cause) {
        for (ReportableProblem problem : problems) {
            problem.report();
        }
        throw cause;
    }

    public void reportAsProgressEvent(Problem problem) {
        buildOperationProgressEventEmitter.emitNowIfCurrent(problem);
    }
}
