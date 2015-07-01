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

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.microservices.server.HttpResponder;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

/**
 * Wrap HttpResponder to call post handler hook.
 */
final class WrappedHttpResponder implements HttpResponder {
  private static final Logger LOG = LoggerFactory.getLogger(WrappedHttpResponder.class);

  private final HttpResponder delegate;
  private final Iterable<? extends HandlerHook> handlerHooks;
  private final HttpRequest httpRequest;
  private final HandlerInfo handlerInfo;
  private volatile HttpResponseStatus status;

  public WrappedHttpResponder(HttpResponder delegate, Iterable<? extends HandlerHook> handlerHooks,
                              HttpRequest httpRequest, HandlerInfo handlerInfo) {
    this.delegate = delegate;
    this.handlerHooks = handlerHooks;
    this.httpRequest = httpRequest;
    this.handlerInfo = handlerInfo;
  }


  
  public void sendJson(HttpResponseStatus status, Object object) {
    delegate.sendJson(status, object);
    runHook(status);
  }

  
  public void sendJson(HttpResponseStatus status, Object object, Type type) {
    delegate.sendJson(status, object, type);
    runHook(status);
  }

  
  public void sendJson(HttpResponseStatus status, Object object, Type type, Gson gson) {
    delegate.sendJson(status, object, type, gson);
    runHook(status);
  }

  
  public void sendString(HttpResponseStatus status, String data) {
    delegate.sendString(status, data);
    runHook(status);
  }

  
  public void sendStatus(HttpResponseStatus status) {
    delegate.sendStatus(status);
    runHook(status);
  }

  
  public void sendStatus(HttpResponseStatus status, Multimap<String, String> headers) {
    delegate.sendStatus(status, headers);
    runHook(status);
  }

  
  public void sendByteArray(HttpResponseStatus status, byte[] bytes, Multimap<String, String> headers) {
    delegate.sendByteArray(status, bytes, headers);
    runHook(status);
  }

  
  public void sendBytes(HttpResponseStatus status, ByteBuffer buffer, Multimap<String, String> headers) {
    delegate.sendBytes(status, buffer, headers);
    runHook(status);
  }

  
  public void sendError(HttpResponseStatus status, String errorMessage) {
    delegate.sendError(status, errorMessage);
    runHook(status);
  }

  
  public void sendChunkStart(HttpResponseStatus status, Multimap<String, String> headers) {
    this.status = status;
    delegate.sendChunkStart(status, headers);
  }

  
  public void sendChunk(ChannelBuffer content) {
    delegate.sendChunk(content);
  }

  
  public void sendChunkEnd() {
    delegate.sendChunkEnd();
    runHook(status);
  }

  
  public void sendContent(HttpResponseStatus status, ChannelBuffer content, String contentType,
                          Multimap<String, String> headers) {
    delegate.sendContent(status, content, contentType, headers);
    runHook(status);
  }

  
  public void sendFile(File file, Multimap<String, String> headers) {
    delegate.sendFile(file, headers);
    runHook(status);
  }

  private void runHook(HttpResponseStatus status) {
    for (HandlerHook hook : handlerHooks) {
      try {
        hook.postCall(httpRequest, status, handlerInfo);
      } catch (Throwable t) {
        LOG.error("Post handler hook threw exception: ", t);
      }
    }
  }
}
