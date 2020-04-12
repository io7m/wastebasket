/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for
 * any purpose with or without fee is hereby granted, provided that the
 * above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 * WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR
 * BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES
 * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
 * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
 * ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
 * SOFTWARE.
 */

package com.io7m.wastebasket.vanilla;

import com.io7m.wastebasket.api.WBAuditLogType;
import com.io7m.wastebasket.api.WBBlobStoreType;
import com.io7m.wastebasket.api.WBServerConfiguration;
import com.io7m.wastebasket.api.WBUserDatabaseType;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WBServerMain
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WBServerMain.class);

  private final Server server;
  private final HttpConfiguration httpsConfig;
  private WBServerConfiguration configuration;

  private WBServerMain(
    final WBServerConfiguration inConfiguration,
    final Server inServer,
    final HttpConfiguration inHttpsConfig)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.server =
      Objects.requireNonNull(inServer, "server");
    this.httpsConfig =
      Objects.requireNonNull(inHttpsConfig, "httpsConfig");
  }

  public static WBServerMain create(
    final WBServerConfiguration configuration,
    final WBBlobStoreType blobStore,
    final WBUserDatabaseType users,
    final WBAuditLogType auditLog)
    throws GeneralSecurityException, IOException
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(blobStore, "blobStore");
    Objects.requireNonNull(users, "users");
    Objects.requireNonNull(auditLog, "auditLog");

    final var threadPool =
      new QueuedThreadPool(configuration.serverThreads(), 1);
    final var server =
      new Server(threadPool);

    final var bindPort = configuration.bindPort();
    final HttpConfiguration httpsConfig = new HttpConfiguration();
    httpsConfig.setSecurePort(bindPort);
    httpsConfig.setSendServerVersion(false);
    httpsConfig.setSendXPoweredBy(false);

    createConnectors(configuration, server, httpsConfig);

    final var contextRoot = new ContextHandler("/");
    contextRoot.setHandler(new WBServerRootHandler());
    final var contextV1Deliver = new ContextHandler("/v1/deliver");
    contextV1Deliver.setHandler(
      new WBServerV1DeliverHandler(configuration, blobStore, users, auditLog));

    final var contexts = new ContextHandlerCollection();
    contexts.setHandlers(new Handler[] {
      contextRoot,
      contextV1Deliver,
    });

    server.setErrorHandler(new WBServerErrorHandler());
    server.setHandler(contexts);
    return new WBServerMain(configuration, server, httpsConfig);
  }

  private static void createConnectors(
    final WBServerConfiguration inConfiguration,
    final Server inServer,
    final HttpConfiguration httpsConfig)
    throws GeneralSecurityException, IOException
  {
    final var sslContext =
      WBSSLContexts.create().createContext(inConfiguration);

    final var sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setSslContext(sslContext);

    final SslConnectionFactory sslConnectionFactory =
      new SslConnectionFactory(
        sslContextFactory,
        HttpVersion.HTTP_1_1.asString());

    final HttpConnectionFactory httpConnectionFactory =
      new HttpConnectionFactory(httpsConfig);

    final ServerConnector sslConnector =
      new ServerConnector(inServer, sslConnectionFactory, httpConnectionFactory);

    final var bindAddress = inConfiguration.bindAddress();
    final var bindPort = inConfiguration.bindPort();

    sslConnector.setReuseAddress(true);
    sslConnector.setHost(bindAddress);
    sslConnector.setPort(bindPort);

    for (final Connector connector : inServer.getConnectors()) {
      try {
        connector.stop();
      } catch (final Exception e) {
        LOG.error("could not close connector: ", e);
      }
    }

    inServer.setConnectors(new Connector[] {
      sslConnector,
    });
  }

  public void start()
    throws Exception
  {
    LOG.info(
      "server starting on https://{}:{}/",
      this.configuration.bindAddress(),
      Integer.valueOf(this.configuration.bindPort()));
    this.server.start();
  }

  public void reload()
    throws GeneralSecurityException, IOException
  {
    LOG.info("reloading TLS configuration");
    createConnectors(this.configuration, this.server, this.httpsConfig);
  }

  public void join()
    throws InterruptedException
  {
    this.server.join();
  }
}
