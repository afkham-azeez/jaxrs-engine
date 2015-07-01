/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.microservices.server.internal.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * TODO: class level comment
 */
public class JAXRSBundleListener implements BundleListener {
    private BundleContext context;
//    private List<>

    public JAXRSBundleListener(BundleContext context) {
        this.context = context;
    }

    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        switch (event.getType()) {
            case BundleEvent.STARTED:
                if (context.getBundle() != bundle) {
//                    registry.register(event.getBundle());
                }
                break;

            case BundleEvent.STOPPED:
                if (context.getBundle() != bundle) {
//                    registry.unregister(event.getBundle());
                }
                break;
        }
    }
}
