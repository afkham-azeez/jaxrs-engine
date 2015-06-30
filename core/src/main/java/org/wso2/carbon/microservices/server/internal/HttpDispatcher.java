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

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpDispatcher that invokes the appropriate http-handler method. The handler and the arguments are read
 * from the {@code RequestRouter} context.
 */

public class HttpDispatcher extends SimpleChannelUpstreamHandler {

  private static final Logger LOG = LoggerFactory.getLogger(HttpDispatcher.class);

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    HttpMethodInfo methodInfo  = (HttpMethodInfo) ctx.getPipeline().getContext("router").getAttachment();
    Object message = e.getMessage();
    if (message instanceof HttpMessage) {
      methodInfo.invoke();
    } else if (message instanceof HttpChunk) {
      methodInfo.chunk((HttpChunk) message);
    } else {
      super.messageReceived(ctx, e);
    }
  }
}
