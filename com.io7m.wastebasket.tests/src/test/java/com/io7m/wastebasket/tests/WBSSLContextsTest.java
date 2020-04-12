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

package com.io7m.wastebasket.tests;

import com.io7m.wastebasket.api.WBServerConfiguration;
import com.io7m.wastebasket.vanilla.WBSSLContexts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class WBSSLContextsTest
{
  private Path directory;
  private ExecutorService background;

  @BeforeEach
  public void testSetup()
    throws IOException
  {
    this.directory = TestDirectories.temporaryDirectory();
    this.background = Executors.newFixedThreadPool(1);
  }

  @AfterEach
  public void testTearDown()
  {
    this.background.shutdown();
  }

  private Path copyResource(
    final String resource)
    throws IOException
  {
    final var stream =
      WBSSLContextsTest.class.getResourceAsStream(
        "/com/io7m/wastebasket/tests/" + resource);

    final var target = this.directory.resolve(resource);
    Files.createDirectories(target.getParent());
    Files.copy(stream, target);
    return target;
  }

  @Test
  public void testCreate()
    throws Exception
  {
    final var configuration =
      WBServerConfiguration.builder()
        .setDataDirectory(this.directory.resolve("data"))
        .setUserDatabase(this.directory.resolve("users.db"))
        .setBindAddress("127.0.0.1")
        .setBindPort(8443)
        .setTlsCAFile(
          this.copyResource("pki/ca.crt"))
        .setTlsCertFile(
          this.copyResource("pki/issued/com.io7m.wastebasket.server.crt"))
        .setTlsKeyFile(
          this.copyResource("pki/private/com.io7m.wastebasket.server.key"))
        .build();

    final var contexts = WBSSLContexts.create();
    contexts.createContext(configuration);
  }
}
