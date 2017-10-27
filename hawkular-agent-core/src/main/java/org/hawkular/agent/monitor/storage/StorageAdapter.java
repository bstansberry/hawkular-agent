/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.agent.monitor.storage;

import org.hawkular.agent.monitor.api.InventoryStorage;
import org.hawkular.agent.monitor.api.NotificationStorage;
import org.hawkular.agent.monitor.config.AgentCoreEngineConfiguration;
import org.hawkular.agent.monitor.diagnostics.Diagnostics;

public interface StorageAdapter extends InventoryStorage, NotificationStorage {

    /**
     * Initializes the storage adapter.
     *
     * @param feedId identifies the feed that is storing data
     * @param config the configuration of the storage adapter
     * @param diag the object used to track internal diagnostic data for the storage adapter
     * @param httpClientBuilder used to communicate with the storage server
     */
    void initialize(
            String feedId,
            AgentCoreEngineConfiguration.StorageAdapterConfiguration config,
            Diagnostics diag,
            HttpClientBuilder httpClientBuilder);

    /**
     * Clean up and stop whatever the storage adapter is doing.
     */
    void shutdown();

    /**
     * @return the storage adapter's configuration settings
     */
    AgentCoreEngineConfiguration.StorageAdapterConfiguration getStorageAdapterConfiguration();
}
