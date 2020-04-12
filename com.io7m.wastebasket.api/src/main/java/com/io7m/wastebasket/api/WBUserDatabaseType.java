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
import java.security.GeneralSecurityException;

/**
 * The interface exposed by user databases.
 *
 * A user database contains a list of usernames and associated passwords.
 */

public interface WBUserDatabaseType extends Closeable
{
  /**
   * Check if {@code user} exists and has password {@code password}.
   *
   * @param user     The user
   * @param password The password
   *
   * @return {@code true} iff authentication succeeded
   */

  boolean authenticate(
    WBUserName user,
    WBPassKey password);

  /**
   * Add a user to the database.
   *
   * @param user     The user
   * @param password The password
   *
   * @throws IOException              On I/O errors
   * @throws GeneralSecurityException On other security errors
   */

  void userAdd(
    WBUserName user,
    WBPassKey password)
    throws IOException, GeneralSecurityException;

  /**
   * Remove a user from the database.
   *
   * @param user The user
   *
   * @throws IOException On I/O errors
   */

  void userDelete(
    WBUserName user)
    throws IOException;
}
