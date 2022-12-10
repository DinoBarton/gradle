/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.api.internal.tasks.schema;

import com.google.common.collect.ImmutableSortedSet;
import org.gradle.internal.execution.schema.AbstractWorkInstanceSchema;
import org.gradle.internal.execution.schema.FileInputPropertySchema;
import org.gradle.internal.execution.schema.ScalarInputPropertySchema;
import org.gradle.internal.schema.NestedPropertySchema;

import java.util.stream.Stream;

public class DefaultTaskInstanceSchema extends AbstractWorkInstanceSchema implements TaskInstanceSchema {
    private final ImmutableSortedSet<FileOutputPropertySchema> outputs;
    private final ImmutableSortedSet<LocalStatePropertySchema> localState;
    private final ImmutableSortedSet<DestroysPropertySchema> destroys;

    public DefaultTaskInstanceSchema(
        ImmutableSortedSet<NestedPropertySchema> nestedProperties,
        ImmutableSortedSet<ScalarInputPropertySchema> inputs,
        ImmutableSortedSet<FileInputPropertySchema> fileInputs,
        ImmutableSortedSet<FileOutputPropertySchema> outputs,
        ImmutableSortedSet<LocalStatePropertySchema> localState,
        ImmutableSortedSet<DestroysPropertySchema> destroys
    ) {
        super(nestedProperties, inputs, fileInputs);
        this.outputs = outputs;
        this.localState = localState;
        this.destroys = destroys;
    }

    @Override
    public Stream<FileOutputPropertySchema> getOutputs() {
        return outputs.stream();
    }

    @Override
    public Stream<LocalStatePropertySchema> getLocalState() {
        return localState.stream();
    }

    @Override
    public Stream<DestroysPropertySchema> getDestroys() {
        return destroys.stream();
    }
}
