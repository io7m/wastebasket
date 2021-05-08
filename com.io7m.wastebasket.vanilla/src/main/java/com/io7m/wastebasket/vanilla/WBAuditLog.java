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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * The default implementation of the audit log interface.
 */

public final class WBAuditLog implements WBAuditLogType
{
  private final BufferedWriter writer;

  private WBAuditLog(
    final BufferedWriter inWriter)
  {
    this.writer = Objects.requireNonNull(inWriter, "writer");
  }

  /**
   * Create an audit log.
   *
   * @param file The log file
   *
   * @return An audit log
   *
   * @throws IOException On I/O errors
   */

  public static WBAuditLogType create(
    final Path file)
    throws IOException
  {
    return new WBAuditLog(Files.newBufferedWriter(file, UTF_8, CREATE, APPEND));
  }

  @Override
  public void dataWritten(
    final OffsetDateTime time,
    final String user,
    final String address,
    final int port,
    final String id,
    final long size)
    throws IOException
  {
    final var eventBuilder = new StringBuilder(128);
    eventBuilder.append(time.toString());
    eventBuilder.append('|');

    eventBuilder.append("DATA_WRITTEN_1_0");
    eventBuilder.append('|');

    eventBuilder.append(user);
    eventBuilder.append('|');

    eventBuilder.append(address);
    eventBuilder.append(':');
    eventBuilder.append(port);
    eventBuilder.append('|');

    eventBuilder.append(id);
    eventBuilder.append('|');

    eventBuilder.append(Long.toUnsignedString(size));
    eventBuilder.append('|');

    synchronized (this.writer) {
      this.writer.write(eventBuilder.toString());
      this.writer.flush();
    }
  }

  @Override
  public void close()
    throws IOException
  {
    synchronized (this.writer) {
      this.writer.flush();
      this.writer.close();
    }
  }
}
