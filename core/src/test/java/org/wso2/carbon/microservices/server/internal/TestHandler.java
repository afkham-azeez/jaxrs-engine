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
import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;

import javax.ws.rs.*;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Test handler.
 */
@SuppressWarnings("UnusedParameters")
@Path("/test/v1")
public class TestHandler implements HttpHandler {

  @Path("resource")
  @GET
  public void testGet(HttpRequest request, HttpResponder responder) {
    JsonObject object = new JsonObject();
    object.addProperty("status", "Handled get in resource end-point");
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("tweets/{id}")
  @GET
  public void testGetTweet(HttpRequest request, HttpResponder responder, @PathParam("id") String id) {
    JsonObject object = new JsonObject();
    object.addProperty("status", String.format("Handled get in tweets end-point, id: %s", id));
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("tweets/{id}")
  @PUT
  public void testPutTweet(HttpRequest request, HttpResponder responder, @PathParam("id") String id) {
    JsonObject object = new JsonObject();
    object.addProperty("status", String.format("Handled put in tweets end-point, id: %s", id));
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("facebook/{id}/message")
  @DELETE
  public void testNoMethodRoute(HttpRequest request, HttpResponder responder, @PathParam("id") String id) {

  }

  @Path("facebook/{id}/message")
  @PUT
  public void testPutMessage(HttpRequest request, HttpResponder responder, @PathParam("id") String id) {
    String message = String.format("Handled put in tweets end-point, id: %s. ", id);
    try {
      String data = getStringContent(request);
      message = message.concat(String.format("Content: %s", data));
    } catch (IOException e) {
      //This condition should never occur
      Assert.fail();
    }
    JsonObject object = new JsonObject();
    object.addProperty("result", message);
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("facebook/{id}/message")
  @POST
  public void testPostMessage(HttpRequest request, HttpResponder responder, @PathParam("id") String id) {
    String message = String.format("Handled post in tweets end-point, id: %s. ", id);
    try {
      String data = getStringContent(request);
      message = message.concat(String.format("Content: %s", data));
    } catch (IOException e) {
      //This condition should never occur
      Assert.fail();
    }
    JsonObject object = new JsonObject();
    object.addProperty("result", message);
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("/user/{userId}/message/{messageId}")
  @GET
  public void testMultipleParametersInPath(HttpRequest request, HttpResponder responder,
                                           @PathParam("userId") String userId,
                                           @PathParam("messageId") int messageId) {
    JsonObject object = new JsonObject();
    object.addProperty("result", String.format("Handled multiple path parameters %s %d", userId, messageId));
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("/message/{messageId}/user/{userId}")
  @GET
  public void testMultipleParametersInDifferentParameterDeclarationOrder(HttpRequest request, HttpResponder responder,
                                                                         @PathParam("userId") String userId,
                                                                         @PathParam("messageId") int messageId) {
    JsonObject object = new JsonObject();
    object.addProperty("result", String.format("Handled multiple path parameters %s %d", userId, messageId));
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("/NotRoutable/{id}")
  @GET
  public void notRoutableParameterMismatch(HttpRequest request,
                                           HttpResponder responder, @PathParam("userid") String userId) {
    JsonObject object = new JsonObject();
    object.addProperty("result", String.format("Handled Not routable path %s ", userId));
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("/NotRoutable/{userId}/message/{messageId}")
  @GET
  public void notRoutableMissingParameter(HttpRequest request, HttpResponder responder,
                                          @PathParam("userId") String userId, String messageId) {
    JsonObject object = new JsonObject();
    object.addProperty("result", String.format("Handled Not routable path %s ", userId));
    responder.sendJson(HttpResponseStatus.OK, object);
  }

  @Path("/exception")
  @GET
  public void exception(HttpRequest request, HttpResponder responder) {
    throw new IllegalArgumentException("Illegal argument");
  }

  private String getStringContent(HttpRequest request) throws IOException {
    return IOUtils.toString(new ChannelBufferInputStream(request.getContent()));
  }

  @Path("/multi-match/**")
  @GET
  public void multiMatchAll(HttpRequest request, HttpResponder responder) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-*");
  }

  @Path("/multi-match/{param}")
  @GET
  public void multiMatchParam(HttpRequest request, HttpResponder responder, @PathParam("param") String param) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-param-" + param);
  }

  @Path("/multi-match/foo")
  @GET
  public void multiMatchFoo(HttpRequest request, HttpResponder responder) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-get-actual-foo");
  }

  @Path("/multi-match/foo")
  @PUT
  public void multiMatchParamPut(HttpRequest request, HttpResponder responder) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-put-actual-foo");
  }

  @Path("/multi-match/{param}/bar")
  @GET
  public void multiMatchParamBar(HttpRequest request, HttpResponder responder, @PathParam("param") String param) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-param-bar-" + param);
  }

  @Path("/multi-match/foo/{param}")
  @GET
  public void multiMatchFooParam(HttpRequest request, HttpResponder responder, @PathParam("param") String param) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-get-foo-param-" + param);
  }

  @Path("/multi-match/foo/{param}/bar")
  @GET
  public void multiMatchFooParamBar(HttpRequest request, HttpResponder responder, @PathParam("param") String param) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-foo-param-bar-" + param);
  }

  @Path("/multi-match/foo/bar/{param}")
  @GET
  public void multiMatchFooBarParam(HttpRequest request, HttpResponder responder, @PathParam("param") String param) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-foo-bar-param-" + param);
  }

  @Path("/multi-match/foo/{param}/bar/baz")
  @GET
  public void multiMatchFooParamBarBaz(HttpRequest request, HttpResponder responder,
                                       @PathParam("param") String param) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-foo-param-bar-baz-" + param);
  }

  @Path("/multi-match/foo/bar/{param}/{id}")
  @GET
  public void multiMatchFooBarParamId(HttpRequest request, HttpResponder responder,
                                      @PathParam("param") String param, @PathParam("id") String id) {
    responder.sendString(HttpResponseStatus.OK, "multi-match-foo-bar-param-" + param + "-id-" + id);
  }

  @Path("/stream/upload")
  @PUT
  public BodyConsumer streamUpload(HttpRequest request, HttpResponder responder) {
    final int fileSize = 200 * 1024 * 1024;
    return new BodyConsumer() {
      ByteBuffer offHeapBuffer = ByteBuffer.allocateDirect(fileSize);

      @Override
      public void chunk(ChannelBuffer request, HttpResponder responder) {
        offHeapBuffer.put(request.array());
      }

      @Override
      public void finished(HttpResponder responder) {
        int bytesUploaded = offHeapBuffer.position();
        responder.sendString(HttpResponseStatus.OK, "Uploaded:" + bytesUploaded);
      }

      @Override
      public void handleError(Throwable cause) {
        offHeapBuffer = null;
      }

    };
  }

  @Path("/stream/upload/fail")
  @PUT
  public BodyConsumer streamUploadFailure(HttpRequest request, HttpResponder responder)  {
    final int fileSize = 200 * 1024 * 1024;

    return new BodyConsumer() {
      int count = 0;
      ByteBuffer offHeapBuffer = ByteBuffer.allocateDirect(fileSize);

      @Override
      public void chunk(ChannelBuffer request, HttpResponder responder) {
        Preconditions.checkState(count == 1, "chunk error");
        offHeapBuffer.put(request.array());
      }

      @Override
      public void finished(HttpResponder responder) {
        int bytesUploaded = offHeapBuffer.position();
        responder.sendString(HttpResponseStatus.OK, "Uploaded:" + bytesUploaded);
      }

      @Override
      public void handleError(Throwable cause) {
        offHeapBuffer = null;
      }
    };
  }

  @Path("/aggregate/upload")
  @PUT
  public void aggregatedUpload(HttpRequest request, HttpResponder response) {
    ChannelBuffer content = request.getContent();
    int bytesUploaded = content.readableBytes();
    response.sendString(HttpResponseStatus.OK, "Uploaded:" + bytesUploaded);
  }


  @Path("/uexception")
  @GET
  public void testException(HttpRequest request, HttpResponder responder) {
    throw Throwables.propagate(new RuntimeException("User Exception"));
  }

  @Override
  public void init(HandlerContext context) {}

  @Override
  public void destroy(HandlerContext context) {}
}
