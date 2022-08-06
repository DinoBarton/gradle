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

package org.gradle.jvm.toolchain.internal;

import org.gradle.api.Transformer;
import org.gradle.api.internal.provider.ProviderInternal;
import org.gradle.api.internal.provider.TransformBackedProvider;

import javax.annotation.Nonnull;

// TODO: most likely needs to be removed, keeping for reference during review and discussion
public class JavaToolProvider<T> extends TransformBackedProvider<T, JavaToolchain> {

    private final DefaultJavaToolchainUsageProgressDetails.JavaTool tool;

    public JavaToolProvider(
        Transformer<? extends T, ? super JavaToolchain> transformer,
        ProviderInternal<? extends JavaToolchain> provider,
        DefaultJavaToolchainUsageProgressDetails.JavaTool tool
    ) {
        super(transformer, provider);
        this.tool = tool;
    }

    @Nonnull
    @Override
    protected Value<? extends T> mapValue(Value<? extends JavaToolchain> value) {
        if (value.isMissing()) {
            return value.asType();
        }
        JavaToolchain javaToolchain = value.get();
        return super.mapValue(value).withSideEffect(ignored -> javaToolchain.emitUsageEvent(tool));
    }
}