/**
 * Copyright WSO2 Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.wso2.carbon.microservices.server.internal;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * A base implementation of {@link HandlerHook} that provides no-op for both
 * {@link HandlerHook#preCall(org.jboss.netty.handler.codec.http.HttpRequest, HttpResponder, HandlerInfo)}
 * and {@link HandlerHook#postCall(org.jboss.netty.handler.codec.http.HttpRequest,
 * org.jboss.netty.handler.codec.http.HttpResponseStatus, HandlerInfo)} methods.
 */
public abstract class AbstractHandlerHook implements HandlerHook {

    public boolean preCall(HttpRequest request, HttpResponder responder, HandlerInfo handlerInfo) {
        return true;
    }

    public void postCall(HttpRequest request, HttpResponseStatus status, HandlerInfo handlerInfo) {
        // no-op
    }
}
