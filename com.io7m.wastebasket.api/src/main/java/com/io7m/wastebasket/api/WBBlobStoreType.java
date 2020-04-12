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

import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;

/**
 * A blob store.
 */

public interface WBBlobStoreType
{
  /**
   * Open a new output stream for a blob with {@code id}.
   *
   * @param id The blob ID
   *
   * @return An output stream
   *
   * @throws IOException              On I/O errors
   * @throws NoSuchAlgorithmException If the JVM does not support the required digest
   */

  DigestOutputStream open(
    WBBlobID id)
    throws IOException, NoSuchAlgorithmException;
}
