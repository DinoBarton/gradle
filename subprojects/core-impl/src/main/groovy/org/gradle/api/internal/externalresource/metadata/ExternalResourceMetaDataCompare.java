/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.externalresource.metadata;

import org.gradle.api.Nullable;
import org.gradle.internal.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public abstract class ExternalResourceMetaDataCompare {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalResourceMetaDataCompare.class);

    public static boolean isDefinitelyUnchanged(@Nullable ExternalResourceMetaData local, Factory<ExternalResourceMetaData> remoteFactory) {
        if (local == null) {
            return false;
        }

        Date localLastModified = local.getLastModified();
        if (localLastModified == null) {
            return false;
        }
        
        long localContentLength = local.getContentLength();
        if (localContentLength < 1) {
            return false;
        }

        // We have enough local data to make a comparison, get the remote metadata
        ExternalResourceMetaData remote = remoteFactory.create();
        if (remote == null) {
            return false;
        }
        
        Date remoteLastModified = remote.getLastModified();
        if (remoteLastModified == null) {
            return false;
        }

        long remoteContentLength = remote.getContentLength();
        //noinspection SimplifiableIfStatement
        if (remoteContentLength < 1) {
            return false;
        }
        
        return localContentLength == remoteContentLength && remoteLastModified.equals(localLastModified);
    }
}
