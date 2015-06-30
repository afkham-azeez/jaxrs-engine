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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Service;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Test the HttpServer.
 */
public class HttpServerTest {

  protected static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
  protected static final Gson GSON = new Gson();

  protected static NettyHttpService service;
  protected static URI baseURI;

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  @BeforeClass
  public static void setup() throws Exception {
    List<HttpHandler> handlers = Lists.newArrayList();
    handlers.add(new TestHandler());

    NettyHttpService.Builder builder = NettyHttpService.builder();
    builder.addHttpHandlers(handlers);
    builder.setHttpChunkLimit(75 * 1024);

    builder.modifyChannelPipeline(new Function<ChannelPipeline, ChannelPipeline>() {
      @Nullable
      @Override
      public ChannelPipeline apply(@Nullable ChannelPipeline channelPipeline) {
        channelPipeline.addAfter("decoder", "testhandler", new TestChannelHandler());
        return channelPipeline;
      }
    });

    service = builder.build();
    service.startAndWait();
    Service.State state = service.state();
    Assert.assertEquals(Service.State.RUNNING, state);

    int port = service.getBindAddress().getPort();
    baseURI = URI.create(String.format("http://localhost:%d", port));
  }

  @AfterClass
  public static void teardown() throws Exception {
    service.stopAndWait();
  }

  @Test
  public void testValidEndPoints() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/resource?num=10", HttpMethod.GET);
    Assert.assertEquals(200, urlConn.getResponseCode());
    String content = getContent(urlConn);

    Map<String, String> map = GSON.fromJson(content, STRING_MAP_TYPE);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Handled get in resource end-point", map.get("status"));
    urlConn.disconnect();

    urlConn = request("/test/v1/tweets/1", HttpMethod.GET);
    Assert.assertEquals(200, urlConn.getResponseCode());
    content = getContent(urlConn);
    map = GSON.fromJson(content, STRING_MAP_TYPE);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Handled get in tweets end-point, id: 1", map.get("status"));
    urlConn.disconnect();
  }


  @Test
  public void testSmallFileUpload() throws IOException {
    testStreamUpload(10);
  }

  @Test
  public void testLargeFileUpload() throws IOException {
    testStreamUpload(100 * 1024 * 1024);
  }


  protected void testStreamUpload(int size) throws IOException {
    //create a random file to be uploaded.
    File fname = tmpFolder.newFile();
    RandomAccessFile randf = new RandomAccessFile(fname, "rw");
    randf.setLength(size);
    randf.close();

    //test stream upload
    HttpURLConnection urlConn = request("/test/v1/stream/upload", HttpMethod.PUT);
    Files.copy(fname, urlConn.getOutputStream());
    Assert.assertEquals(200, urlConn.getResponseCode());
    urlConn.disconnect();
  }

  @Test
  public void testStreamUploadFailure() throws IOException {
    //create a random file to be uploaded.
    int size = 20 * 1024;
    File fname = tmpFolder.newFile();
    RandomAccessFile randf = new RandomAccessFile(fname, "rw");
    randf.setLength(size);
    randf.close();

    HttpURLConnection urlConn = request("/test/v1/stream/upload/fail", HttpMethod.PUT);
    Files.copy(fname, urlConn.getOutputStream());
    Assert.assertEquals(500, urlConn.getResponseCode());
    urlConn.disconnect();
  }

  @Test
  public void testChunkAggregatedUpload() throws IOException {
    //create a random file to be uploaded.
    int size = 69 * 1024;
    File fname = tmpFolder.newFile();
    RandomAccessFile randf = new RandomAccessFile(fname, "rw");
    randf.setLength(size);
    randf.close();

    //test chunked upload
    HttpURLConnection urlConn = request("/test/v1/aggregate/upload", HttpMethod.PUT);
    urlConn.setChunkedStreamingMode(1024);
    Files.copy(fname, urlConn.getOutputStream());
    Assert.assertEquals(200, urlConn.getResponseCode());

    Assert.assertEquals(size, Integer.parseInt(getContent(urlConn).split(":")[1].trim()));
    urlConn.disconnect();
  }

  @Test
  public void testChunkAggregatedUploadFailure() throws IOException {
    //create a random file to be uploaded.
    int size = 78 * 1024;
    File fname = tmpFolder.newFile();
    RandomAccessFile randf = new RandomAccessFile(fname, "rw");
    randf.setLength(size);
    randf.close();

    //test chunked upload
    HttpURLConnection urlConn = request("/test/v1/aggregate/upload", HttpMethod.PUT);
    urlConn.setChunkedStreamingMode(1024);
    Files.copy(fname, urlConn.getOutputStream());
    Assert.assertEquals(500, urlConn.getResponseCode());
    urlConn.disconnect();
  }

  @Test
  public void testPathWithMultipleMethods() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/tweets/1", HttpMethod.GET);
    Assert.assertEquals(200, urlConn.getResponseCode());
    urlConn.disconnect();

    urlConn = request("/test/v1/tweets/1", HttpMethod.PUT);
    writeContent(urlConn, "data");
    Assert.assertEquals(200, urlConn.getResponseCode());
    urlConn.disconnect();
  }


  @Test
  public void testNonExistingEndPoints() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/users", HttpMethod.POST);
    writeContent(urlConn, "data");
    Assert.assertEquals(404, urlConn.getResponseCode());
    urlConn.disconnect();
  }

  @Test
  public void testPutWithData() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/facebook/1/message", HttpMethod.PUT);
    writeContent(urlConn, "Hello, World");
    Assert.assertEquals(200, urlConn.getResponseCode());

    String content = getContent(urlConn);

    Map<String, String> map = GSON.fromJson(content, STRING_MAP_TYPE);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Handled put in tweets end-point, id: 1. Content: Hello, World", map.get("result"));
    urlConn.disconnect();
  }

  @Test
  public void testPostWithData() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/facebook/1/message", HttpMethod.POST);
    writeContent(urlConn, "Hello, World");
    Assert.assertEquals(200, urlConn.getResponseCode());

    String content = getContent(urlConn);

    Map<String, String> map = GSON.fromJson(content, STRING_MAP_TYPE);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Handled post in tweets end-point, id: 1. Content: Hello, World", map.get("result"));
    urlConn.disconnect();
  }

  @Test
  public void testNonExistingMethods() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/facebook/1/message", HttpMethod.GET);
    Assert.assertEquals(405, urlConn.getResponseCode());
    urlConn.disconnect();
  }

  @Test
  public void testKeepAlive() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/tweets/1", HttpMethod.PUT, true);
    writeContent(urlConn, "data");
    Assert.assertEquals(200, urlConn.getResponseCode());
    System.out.println(urlConn.getHeaderField(HttpHeaders.Names.CONNECTION));

    Assert.assertEquals("keep-alive", urlConn.getHeaderField(HttpHeaders.Names.CONNECTION));
    urlConn.disconnect();
  }

  @Test
  public void testMultiplePathParameters() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/user/sree/message/12", HttpMethod.GET);
    Assert.assertEquals(200, urlConn.getResponseCode());

    String content = getContent(urlConn);

    Map<String, String> map = GSON.fromJson(content, STRING_MAP_TYPE);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Handled multiple path parameters sree 12", map.get("result"));
    urlConn.disconnect();
  }

  //Test the end point where the parameter in path and order of declaration in method signature are different
  @Test
  public void testMultiplePathParametersWithParamterInDifferentOrder() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/message/21/user/sree", HttpMethod.GET);
    Assert.assertEquals(200, urlConn.getResponseCode());

    String content = getContent(urlConn);

    Map<String, String> map = GSON.fromJson(content, STRING_MAP_TYPE);
    Assert.assertEquals(1, map.size());
    Assert.assertEquals("Handled multiple path parameters sree 21", map.get("result"));
    urlConn.disconnect();
  }

  @Test
  public void testNotRoutablePathParamMismatch() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/NotRoutable/sree", HttpMethod.GET);
    Assert.assertEquals(500, urlConn.getResponseCode());
    urlConn.disconnect();
  }

  @Test
  public void testNotRoutableMissingPathParam() throws IOException {
    HttpURLConnection urlConn = request("/test/v1/NotRoutable/sree/message/12", HttpMethod.GET);
    Assert.assertEquals(500, urlConn.getResponseCode());
    urlConn.disconnect();
  }

  @Test
  public void testMultiMatchParamPut() throws Exception {
    HttpURLConnection urlConn = request("/test/v1/multi-match/bar", HttpMethod.PUT);
    Assert.assertEquals(405, urlConn.getResponseCode());
    urlConn.disconnect();
  }

  @Test
  public void testHandlerException() throws Exception {
    HttpURLConnection urlConn = request("/test/v1/uexception", HttpMethod.GET);
    Assert.assertEquals(500, urlConn.getResponseCode());
    Assert.assertEquals("Exception Encountered while processing request : User Exception",
                        IOUtils.toString(urlConn.getErrorStream()));
    urlConn.disconnect();
  }

  /**
   * Test that the TestChannelHandler that was added using the builder adds the correct header field and value.
   * @throws Exception
   */
  @Test
  public void testChannelPipelineModification() throws Exception {
    HttpURLConnection urlConn = request("/test/v1/tweets/1", HttpMethod.GET);
    Assert.assertEquals(200, urlConn.getResponseCode());
    Assert.assertEquals(urlConn.getHeaderField(TestChannelHandler.HEADER_FIELD), TestChannelHandler.HEADER_VALUE);
  }

  @Test
  public void testMultiMatchFoo() throws Exception {
    testContent("/test/v1/multi-match/foo", "multi-match-get-actual-foo");
  }

  @Test
  public void testMultiMatchAll() throws Exception {
    testContent("/test/v1/multi-match/foo/baz/id", "multi-match-*");
  }

  @Test
  public void testMultiMatchParam() throws Exception {
    testContent("/test/v1/multi-match/bar", "multi-match-param-bar");
  }

  @Test
  public void testMultiMatchParamBar() throws Exception {
    testContent("/test/v1/multi-match/id/bar", "multi-match-param-bar-id");
  }

  @Test
  public void testMultiMatchFooParamBar() throws Exception {
    testContent("/test/v1/multi-match/foo/id/bar", "multi-match-foo-param-bar-id");
  }

  @Test
  public void testMultiMatchFooBarParam() throws Exception {
    testContent("/test/v1/multi-match/foo/bar/id", "multi-match-foo-bar-param-id");
  }

  @Test
  public void testMultiMatchFooBarParamId() throws Exception {
    testContent("/test/v1/multi-match/foo/bar/bar/bar", "multi-match-foo-bar-param-bar-id-bar");
  }

  @Test
  public void testMultiMatchFooPut() throws Exception {
    testContent("/test/v1/multi-match/foo", "multi-match-put-actual-foo", HttpMethod.PUT);
  }

  protected void testContent(String path, String content) throws IOException {
    testContent(path, content, HttpMethod.GET);
  }

  protected void testContent(String path, String content, HttpMethod method) throws IOException {
    HttpURLConnection urlConn = request(path, method);
    Assert.assertEquals(200, urlConn.getResponseCode());
    Assert.assertEquals(content, getContent(urlConn));
    urlConn.disconnect();
  }

  protected HttpURLConnection request(String path, HttpMethod method) throws IOException {
    return request(path, method, false);
  }

  protected HttpURLConnection request(String path, HttpMethod method, boolean keepAlive) throws IOException {
    URL url = baseURI.resolve(path).toURL();
    HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
    if (method == HttpMethod.POST || method == HttpMethod.PUT) {
      urlConn.setDoOutput(true);
    }
    urlConn.setRequestMethod(method.getName());
    if (!keepAlive) {
      urlConn.setRequestProperty(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
    }

    return urlConn;
  }

  protected String getContent(HttpURLConnection urlConn) throws IOException {
    return new String(ByteStreams.toByteArray(urlConn.getInputStream()), Charsets.UTF_8);
  }

  protected void writeContent(HttpURLConnection urlConn, String content) throws IOException {
    urlConn.getOutputStream().write(content.getBytes(Charsets.UTF_8));
  }
}
