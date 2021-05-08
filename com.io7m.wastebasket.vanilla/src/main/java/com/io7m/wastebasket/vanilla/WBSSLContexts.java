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

import com.io7m.wastebasket.api.WBServerConfiguration;
import de.dentrassi.crypto.pem.PemKeyStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.Objects;

/**
 * Functions over SSL contexts.
 */

public final class WBSSLContexts
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WBSSLContexts.class);

  private WBSSLContexts()
  {

  }

  /**
   * @return An SSL context factory
   */

  public static WBSSLContexts create()
  {
    Security.addProvider(new PemKeyStoreProvider());
    return new WBSSLContexts();
  }

  private static void writeNewlines(
    final OutputStream concatenation)
    throws IOException
  {
    concatenation.write('\r');
    concatenation.write('\n');
    concatenation.write('\r');
    concatenation.write('\n');
  }

  /**
   * Create an SSL context.
   *
   * @param configuration The server configuration
   *
   * @return A new SSL context
   *
   * @throws GeneralSecurityException On errors
   * @throws IOException              On errors
   */

  public SSLContext createContext(
    final WBServerConfiguration configuration)
    throws GeneralSecurityException, IOException
  {
    Objects.requireNonNull(configuration, "configuration");

    final var fileKey =
      configuration.tlsKeyFile().toAbsolutePath();
    final var fileCert =
      configuration.tlsCertFile().toAbsolutePath();
    final var fileCACert =
      configuration.tlsCAFile().toAbsolutePath();

    LOG.info("Reloading TLS");
    LOG.info("TLS CA certificate: {}", fileCACert);
    LOG.info("TLS key:            {}", fileKey);
    LOG.info("TLS certificate:    {}", fileCert);

    final KeyStore keyStore = KeyStore.getInstance("PEM");
    try (var concatenation = new ByteArrayOutputStream()) {
      try (var stream = Files.newInputStream(fileKey)) {
        stream.transferTo(concatenation);
        writeNewlines(concatenation);
      }
      try (var stream = Files.newInputStream(fileCert)) {
        stream.transferTo(concatenation);
        writeNewlines(concatenation);
      }
      try (var stream = Files.newInputStream(fileCACert)) {
        stream.transferTo(concatenation);
        writeNewlines(concatenation);
      }

      try (var inputStream = new ByteArrayInputStream(concatenation.toByteArray())) {
        keyStore.load(inputStream, null);
      }
    }

    final var keyAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
    LOG.debug("key algorithm: {}", keyAlgorithm);

    LOG.debug("initializing key managers");
    final var keyManagers = KeyManagerFactory.getInstance(keyAlgorithm);
    keyManagers.init(keyStore, null);

    final var protocol = "TLSv1.3";
    LOG.debug("creating SSL context for protocol {}", protocol);
    final var context = SSLContext.getInstance(protocol);
    context.init(keyManagers.getKeyManagers(), null, null);
    return context;
  }
}
