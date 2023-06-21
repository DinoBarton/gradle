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

package org.gradle.api.problems;

import org.gradle.api.Incubating;
import org.gradle.api.problems.interfaces.Problem;
import org.gradle.api.problems.interfaces.ProblemId;
import org.gradle.api.problems.interfaces.Severity;
import org.gradle.api.problems.internal.ProblemsProgressEventEmitterHolder;

/**
 * Prototype Problems API.
 *
 * @since 8.3
 */
@Incubating
public class Problems {

    public static ProblemBuilder createNew(ProblemId problemId, String message, Severity severity) {
        return new ProblemBuilder(problemId, message, severity);
    }

    public static ProblemBuilder throwing(Exception e, ProblemId problemId) { // TODO should be a different builder; that throws as a terminal operation
        return new ProblemBuilder(problemId, e.getMessage(), Severity.ERROR);
    }

    public static void collect(Throwable failure) {
        new ProblemBuilder(ProblemId.GENERIC, failure.getMessage(), Severity.ERROR).cause(failure).report(true);
    }

    public static void collect(Problem problem) {
        ProblemsProgressEventEmitterHolder.get().emitNowIfCurrent(problem);
    }
}
