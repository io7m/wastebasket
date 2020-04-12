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

package com.io7m.wastebasket.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.wastebasket.api.WBBlobStoreType;
import com.io7m.wastebasket.api.WBServerConfiguration;
import com.io7m.wastebasket.api.WBUserDatabaseType;
import com.io7m.wastebasket.vanilla.WBAuditLog;
import com.io7m.wastebasket.vanilla.WBBlobStore;
import com.io7m.wastebasket.vanilla.WBFilesWatcher;
import com.io7m.wastebasket.vanilla.WBServerMain;
import com.io7m.wastebasket.vanilla.WBUserDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Parameters(commandDescription = "Start the server")
public final class CommandServer extends CommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CommandServer.class);

  // CHECKSTYLE:OFF

  @Parameter(
    names = "--tls-ca-certificate",
    required = true,
    description = "The file containing the CA certificate")
  Path tlsCACert;

  @Parameter(
    names = "--tls-certificate",
    required = true,
    description = "The file containing the server certificate")
  Path tlsCert;

  @Parameter(
    names = "--tls-key",
    required = true,
    description = "The file containing the server private key")
  Path tlsKey;

  @Parameter(
    names = "--user-database",
    required = true,
    description = "The file containing the user database")
  Path userDatabase;

  @Parameter(
    names = "--audit-log",
    required = true,
    description = "The file containing the audit log")
  Path auditLog;

  @Parameter(
    names = "--data-directory",
    required = true,
    description = "The directory that will contain data deliveries")
  Path dataDirectory;

  @Parameter(
    names = "--bind-port",
    required = false,
    description = "The port number upon which to listen for requests")
  int bindPort = 443;

  @Parameter(
    names = "--bind-address",
    required = false,
    description = "The address upon which to listen for requests")
  String bindAddress = "127.0.0.1";

  @Parameter(
    names = "--data-size-limit",
    required = false,
    description = "The maximum permitted size of data deliveries in bytes")
  long dataSizeLimit = 10_000_000L;

  @Parameter(
    names = "--thread-count",
    required = false,
    description = "The number of threads to use for serving clients")
  int threadCount = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

  CommandServer()
  {

  }

  // CHECKSTYLE:ON

  @Override
  public Void call()
    throws Exception
  {
    super.call();

    final var configuration =
      WBServerConfiguration.builder()
        .setTlsCAFile(this.tlsCACert)
        .setTlsCertFile(this.tlsCert)
        .setTlsKeyFile(this.tlsKey)
        .setBindPort(this.bindPort)
        .setBindAddress(this.bindAddress)
        .setUserDatabase(this.userDatabase)
        .setDataDirectory(this.dataDirectory)
        .setDataSizeLimit(this.dataSizeLimit)
        .setServerThreads(this.threadCount)
        .build();

    final ExecutorService userExecutor =
      Executors.newFixedThreadPool(1, runnable -> {
        final var thread = new Thread(runnable);
        thread.setName(String.format(
          "com.io7m.wastebasket.users-%d",
          Long.valueOf(thread.getId())));
        return thread;
      });

    final ExecutorService watcherExecutor =
      Executors.newFixedThreadPool(1, runnable -> {
        final var thread = new Thread(runnable);
        thread.setName(String.format(
          "com.io7m.wastebasket.files-%d",
          Long.valueOf(thread.getId())));
        return thread;
      });

    try (var auditLogger = WBAuditLog.create(this.auditLog)) {
      final WBBlobStoreType blobStore = WBBlobStore.create(this.dataDirectory);

      try (WBUserDatabaseType users =
             WBUserDatabase.create(userExecutor, this.userDatabase)) {
        final WBServerMain server =
          WBServerMain.create(
            configuration,
            blobStore,
            users,
            auditLogger);

        LOG.info("process ID: {}", Long.valueOf(ProcessHandle.current().pid()));
        server.start();

        final Runnable onCertificatesChanged = () -> {
          try {
            LOG.info("certificates changed; reloading TLS");
            server.reload();
          } catch (final GeneralSecurityException | IOException e) {
            LOG.error("failed to reload server: ", e);
          }
        };

        try (WBFilesWatcher ignored =
               WBFilesWatcher.create(
                 watcherExecutor,
                 List.of(
                   this.tlsCACert,
                   this.tlsCert,
                   this.tlsKey
                 ),
                 onCertificatesChanged)) {
          try {
            server.join();
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }

    watcherExecutor.shutdown();
    userExecutor.shutdown();
    return null;
  }
}
