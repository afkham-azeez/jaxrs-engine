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

import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;

/**
 * A class that encapsulates SSL Certificate Information
 */
class SSLHandlerFactory {
  private static final String protocol = "TLS";
  private final File keyStore;
  private final String keyStorePassword;
  private final String certificatePassword;
  private final SSLContext serverContext;

  public SSLHandlerFactory(File keyStore, String keyStorePassword, String certificatePassword) {
    this.keyStore = keyStore;
    this.keyStorePassword = keyStorePassword;
    this.certificatePassword = certificatePassword;
    if (this.keyStore == null) {
      throw new IllegalArgumentException("Certificate Path Not Configured");
    }
    if (this.keyStorePassword == null) {
      throw new IllegalArgumentException("KeyStore Password Not Configured");
    }

    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "SunX509";
    }

    try {
      KeyStore ks = KeyStore.getInstance("JKS");
      InputStream inputStream = new FileInputStream(keyStore);
      try {
        ks.load(inputStream, keyStorePassword.toCharArray());
      } finally {
        inputStream.close();
      }
      // Set up key manager factory to use our key store
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
      kmf.init(ks, (certificatePassword != null) ? certificatePassword.toCharArray() : keyStorePassword.toCharArray());

      // Initialize the SSLContext to work with our key managers.
      serverContext = SSLContext.getInstance(protocol);
      serverContext.init(kmf.getKeyManagers(), null, null);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to initialize the server-side SSLContext", e);
    }
  }

  public SslHandler create() {
    SSLEngine engine = serverContext.createSSLEngine();
    engine.setUseClientMode(false);
    return new SslHandler(engine);
  }
}
