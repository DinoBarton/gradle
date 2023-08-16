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

import org.gradle.api.problems.ProblemBuilderSpec;
import org.gradle.api.problems.interfaces.Problem;
import org.gradle.api.problems.interfaces.ProblemBuilder;
import org.gradle.api.problems.interfaces.ProblemBuilderDefiningMessage;
import org.gradle.api.problems.interfaces.ProblemGroup;
import org.gradle.internal.operations.BuildOperationProgressEventEmitter;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.gradle.api.problems.interfaces.ProblemGroup.DEPRECATION_ID;
import static org.gradle.api.problems.interfaces.ProblemGroup.GENERIC_ID;
import static org.gradle.api.problems.interfaces.ProblemGroup.TYPE_VALIDATION_ID;
import static org.gradle.api.problems.interfaces.ProblemGroup.VERSION_CATALOG_ID;

public class DefaultProblems extends InternalProblems {

    private final Map<String, ProblemGroup> problemGroups = new LinkedHashMap<>();

    public DefaultProblems(BuildOperationProgressEventEmitter buildOperationProgressEventEmitter) {
        super(buildOperationProgressEventEmitter);
        addPredefinedGroup(GENERIC_ID);
        addPredefinedGroup(TYPE_VALIDATION_ID);
        addPredefinedGroup(DEPRECATION_ID);
        addPredefinedGroup(VERSION_CATALOG_ID);
    }

    private void addPredefinedGroup(String genericId) {
        problemGroups.put(genericId, new PredefinedProblemGroup(genericId));
    }

    public ProblemBuilderDefiningMessage createProblemBuilder() {
        return createProblemBuilderInternal();
    }

    @Nonnull
    private DefaultProblemBuilder createProblemBuilderInternal() {
        return new DefaultProblemBuilder(this);
    }

    @Override
    public ProblemGroup getProblemGroup(String groupId) {
        return problemGroups.get(groupId);
    }

    @Override
    public RuntimeException throwing(ProblemBuilderSpec action) {
        DefaultProblemBuilder defaultProblemBuilder = createProblemBuilderInternal();
        ProblemBuilder problemBuilder =action.apply(defaultProblemBuilder);
        throw throwError(defaultProblemBuilder.getException(), problemBuilder.build());
    }

    @Override
    public RuntimeException rethrowing(RuntimeException e, ProblemBuilderSpec action) {
        DefaultProblemBuilder defaultProblemBuilder = createProblemBuilderInternal();
        ProblemBuilder problemBuilder = action.apply(defaultProblemBuilder);
        problemBuilder.withException(e);
        throw throwError(e, problemBuilder.build());
    }

    public RuntimeException throwError(RuntimeException exception, Problem problem) {
        reportAsProgressEvent(problem);
        throw exception;
    }
}
