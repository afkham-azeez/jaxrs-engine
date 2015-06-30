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

import com.google.common.base.Preconditions;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * HttpMethodInfo is a helper class having state information about the http handler method to be invoked, the handler
 * and arguments required for invocation by the Dispatcher. RequestRouter populates this class and stores in its
 * context as attachment.
 */
class HttpMethodInfo {

  private final Method method;
  private final HttpHandler handler;
  private final boolean isChunkedRequest;
  private final ChannelBuffer requestContent;
  private final HttpResponder responder;
  private final Object[] args;
  private final boolean isStreaming;

  private BodyConsumer bodyConsumer;

  HttpMethodInfo(Method method, HttpHandler handler, HttpRequest request, HttpResponder responder, Object[] args) {
    this.method = method;
    this.handler = handler;
    this.isChunkedRequest = request.isChunked();
    this.requestContent = request.getContent();
    this.responder = responder;
    this.isStreaming = BodyConsumer.class.isAssignableFrom(method.getReturnType());

    // The actual arguments list to invoke handler method
    this.args = new Object[args.length + 2];
    this.args[0] = rewriteRequest(request, isStreaming);
    this.args[1] = responder;
    System.arraycopy(args, 0, this.args, 2, args.length);
  }

  /**
   * Calls the httpHandler method.
   */
  void invoke() throws Exception {
    if (isStreaming) {
      // Casting guarantee to be succeeded.
      bodyConsumer = (BodyConsumer) method.invoke(handler, args);
      if (requestContent.readable()) {
        try {
          bodyConsumer.chunk(requestContent, responder);
        } catch (Throwable t) {
          bodyConsumer.handleError(t);
          bodyConsumer = null;
          throw new HandlerException(HttpResponseStatus.INTERNAL_SERVER_ERROR, "", t);
        }
      }
      if (!isChunkedRequest) {
        bodyConsumer.finished(responder);
        bodyConsumer = null;
      }
    } else {
      // Actually <T> would be void
      method.invoke(handler, args);
      bodyConsumer = null;
    }
  }

  void chunk(HttpChunk chunk) throws Exception {
    Preconditions.checkState(bodyConsumer != null, "Received chunked content without BodyConsumer.");
    if (chunk.isLast()) {
      bodyConsumer.finished(responder);
      bodyConsumer = null;
    } else {
      try {
        bodyConsumer.chunk(chunk.getContent(), responder);
      } catch (Throwable t) {
        bodyConsumer.handleError(t);
        bodyConsumer = null;
        throw new HandlerException(HttpResponseStatus.INTERNAL_SERVER_ERROR, "", t);
      }
    }
  }

  /**
   * Sends the error to responder.
   */
  void sendError(HttpResponseStatus status, Throwable ex) {
    if (ex instanceof InvocationTargetException) {
      responder.sendError(status, String.format("Exception Encountered while processing request : %s",
                                                ((InvocationTargetException) ex).getTargetException().getMessage()));
    } else {
      responder.sendError(status, String.format("Exception Encountered while processing request: %s", ex.getMessage()));
    }
  }

  /**
   * Returns true if the handler method's return type is BodyConsumer.
   */
  boolean isStreaming() {
    return isStreaming;
  }

  private HttpRequest rewriteRequest(HttpRequest request, boolean isStreaming) {
    if (!isStreaming) {
      return request;
    }

    if (!request.isChunked() || request.getContent().readable()) {
      request.setChunked(true);
      request.setContent(ChannelBuffers.EMPTY_BUFFER);
    }
    return request;
  }
}
