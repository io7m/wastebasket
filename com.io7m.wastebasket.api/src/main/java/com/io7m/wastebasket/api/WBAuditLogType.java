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

import java.io.Closeable;
import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * The audit log interface.
 *
 * An audit log contains events that are important with regards to security.
 */

public interface WBAuditLogType extends Closeable
{
  /**
   * At {@code time}, the user {@code user} using a computer at {@code address:port},
   * wrote data of size {@code size} with {@code id}. The presence of this
   * event does not necessarily mean that the data still exists or that the
   * data was actually successfully written; it only means that everything
   * leading up to the data actually being written to the disk happened without
   * errors.
   *
   * @param time    The write time
   * @param user    The user
   * @param address The address
   * @param port    The port
   * @param id      The ID
   * @param size    The size
   *
   * @throws IOException On I/O errors
   */

  void dataWritten(
    OffsetDateTime time,
    String user,
    String address,
    int port,
    String id,
    long size)
    throws IOException;
}
