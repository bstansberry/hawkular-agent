/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.wildfly.agent.installer;

import java.util.Collection;

import org.hawkular.agent.monitor.util.Util;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.wildfly.agent.itest.util.AbstractITest;
import org.hawkular.wildfly.agent.itest.util.WildFlyClientConfig;
import org.jboss.as.controller.PathAddress;
import org.junit.Assert;
import org.testng.annotations.Test;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class AgentInstallerStandaloneITest extends AbstractITest {
    public static final String GROUP = "AgentInstallerStandloneITest";

    protected static final WildFlyClientConfig wfClientConfig;

    static {
        wfClientConfig = new WildFlyClientConfig();
    }

    @Test(groups = { GROUP })
    public void wfStarted() throws Throwable {
        waitForAccountsAndInventory();
        // System.out.println("wfFeedId = " + wfFeedId);
        Assert.assertNotNull("wfFeedId should not be null", wfClientConfig.getFeedId());
    }

    @Test(groups = { GROUP }, dependsOnMethods = { "wfStarted" })
    public void socketBindingGroupsInInventory() throws Throwable {

        Collection<String> dmrSBGNames = getSocketBindingGroupNames();
        for (String sbgName : dmrSBGNames) {
            Resource sbg = getResource(
                    "/feeds/" + wfClientConfig.getFeedId() + "/resourceTypes/Socket Binding Group/resources",
                    (r -> r.getName().contains(sbgName)));
            System.out.println("socket binding group in inventory=" + sbg);
        }

        // make sure we are testing against what we were expecting
        Assert.assertTrue(dmrSBGNames.contains("default"));
        Assert.assertTrue(dmrSBGNames.contains("ha"));
        Assert.assertTrue(dmrSBGNames.contains("full"));
        Assert.assertTrue(dmrSBGNames.contains("full-ha"));
        Assert.assertEquals("Wrong number of domain profiles", 4, dmrSBGNames.size());
    }

    @Test(groups = { GROUP }, dependsOnMethods = { "wfStarted" })
    public void datasourcesAddedToInventory() throws Throwable {

        for (String datasourceName : getDatasourceNames()) {
            Resource ds = getResource("/feeds/" + wfClientConfig.getFeedId() + "/resourceTypes/Datasource/resources",
                    (r -> r.getId().contains(datasourceName)));
            System.out.println("ds = " + ds);
        }

        Assert.assertNotNull("feedId should not be null", wfClientConfig.getFeedId());
    }

    private Collection<String> getSocketBindingGroupNames() {
        return getDMRChildrenNames(wfClientConfig, "socket-binding-group", PathAddress.EMPTY_ADDRESS);
    }

    private Collection<String> getDatasourceNames() {
        return getDMRChildrenNames(wfClientConfig,
                "data-source", PathAddress.parseCLIStyleAddress("/subsystem=datasources"));
    }

    @Test(dependsOnMethods = { "datasourcesAddedToInventory" })
    public void datasourceMetricsCollected() throws Throwable {
        String lastUrl = "";
        int second = 1000;
        int timeOutSeconds = 60;
        for (int i = 0; i < timeOutSeconds; i++) {
            Request request = newAuthRequest().url(baseMetricsUri + "/gauges").build();
            Response gaugesResponse = client.newCall(request).execute();

            if (gaugesResponse.code() == 200 && !gaugesResponse.body().string().isEmpty()) {
                for (String datasourceName : getDatasourceNames()) {
                    String id = "MI~R~[" + wfClientConfig.getFeedId() + "/Local~/subsystem=datasources/data-source="
                            + datasourceName
                            + "]~MT~Datasource Pool Metrics~Available Count";
                    id = Util.urlEncodeQuery(id);
                    String url = baseMetricsUri + "/gauges/stats?buckets=1&metrics=" + id;
                    lastUrl = url;
                    //System.out.println("url = " + url);
                    Response gaugeResponse = client.newCall(newAuthRequest().url(url).get().build()).execute();
                    if (gaugeResponse.code() == 200 && !gaugeResponse.body().string().isEmpty()) {
                        /* this should be enough to prove that some metric was written successfully */
                        return;
                    }
                }
            }
            Thread.sleep(second);
        }

        Assert.fail("Gauge still not gathered after [" + timeOutSeconds + "] seconds: [" + lastUrl + "]");
    }

    @Test(dependsOnMethods = { "datasourcesAddedToInventory" }, enabled = true)
    public void serverAvailCollected() throws Throwable {
        String lastUrl = "";
        int second = 1000;
        int timeOutSeconds = 60;

        for (int i = 0; i < timeOutSeconds; i++) {
            Request request = newAuthRequest().url(baseMetricsUri + "/availability").build();
            Response availabilityResponse = client.newCall(request).execute();

            if (availabilityResponse.code() == 200 && !availabilityResponse.body().string().isEmpty()) {
                String id = "AI~R~[" + wfClientConfig.getFeedId()
                        + "/Local~~]~AT~Server Availability~Server Availability";
                id = Util.urlEncode(id);
                String url = baseMetricsUri + "/availability/" + id + "/raw";
                //System.out.println("url = " + url);
                availabilityResponse = client.newCall(newAuthRequest().url(url).get().build()).execute();
                if (availabilityResponse.code() == 200 && !availabilityResponse.body().string().isEmpty()) {
                    /* this should be enough to prove that some metric was written successfully */
                    return;
                } else {
                    System.out.println("code = " + availabilityResponse.code());
                }
            }
            Thread.sleep(second);
        }

        Assert.fail("Availability still not gathered after [" + timeOutSeconds + "] seconds: [" + lastUrl + "]");

    }

}
