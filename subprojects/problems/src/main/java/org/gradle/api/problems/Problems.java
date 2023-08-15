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
import org.gradle.api.problems.interfaces.ProblemBuilderDefiningMessage;
import org.gradle.api.problems.interfaces.ProblemGroup;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Problems API service.
 *
 * @since 8.4
 */
@Incubating
@ServiceScope(Scope.Global.class)
public abstract class Problems {

    abstract public ProblemBuilderDefiningMessage createProblemBuilder();

    abstract public void collectError(RuntimeException failure);

    abstract public void collectError(Problem problem);

    abstract public void collectErrors(Collection<Problem> problem);

    abstract public RuntimeException throwing(ProblemSpec action);

    abstract public RuntimeException rethrowing(RuntimeException e, ProblemSpec action);

    abstract public @Nullable ProblemGroup getProblemGroup(String groupId);

    abstract public ProblemGroup registerProblemGroup(String typeId);

    abstract public ProblemGroup registerProblemGroup(ProblemGroup typeId);
}
