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

package com.io7m.wastebasket.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import java.nio.file.Path;
import org.immutables.value.Value;

/**
 * Configuration information for servers.
 */

@ImmutablesStyleType
@Value.Immutable
public interface WBServerConfigurationType
{
  /**
   * @return The local port to which to bind. Must be in the range [1, 65535].
   */

  @Value.Default
  default int bindPort()
  {
    return 443;
  }

  /**
   * @return The maximum size in bytes of data that can be received from clients
   */

  @Value.Default
  default long dataSizeLimit()
  {
    return 10_000_000L;
  }

  /**
   * @return The number of server threads to use
   */

  @Value.Default
  default int serverThreads()
  {
    return Runtime.getRuntime().availableProcessors() * 2;
  }

  /**
   * @return The address to which to bind
   */

  String bindAddress();

  /**
   * @return The user database file
   */

  Path userDatabase();

  /**
   * @return The directory that will be used to store received data
   */

  Path dataDirectory();

  /**
   * @return The CA certificate file for TLS
   */

  Path tlsCAFile();

  /**
   * @return The server private key file for TLS
   */

  Path tlsKeyFile();

  /**
   * @return The server certificate file for TLS
   */

  Path tlsCertFile();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final var count = this.serverThreads();
    if (count < 1 || count > 0x7fff_fffe) {
      throw new IllegalArgumentException("Thread count must be positive");
    }
  }
}
