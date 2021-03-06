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

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Functions over passkeys.
 */

public final class WBPassKeys
{
  /**
   * The pattern that defines valid passkeys.
   */

  public static final Pattern VALID_NAME =
    Pattern.compile("[a-f0-9]{64}");

  private WBPassKeys()
  {

  }

  /**
   * @param text The input text
   *
   * @return {@code true} if the input text represents a valid ID
   */

  public static boolean isValid(
    final String text)
  {
    return VALID_NAME.matcher(Objects.requireNonNull(text, "text"))
      .matches();
  }

  /**
   * Check if the input text is valid.
   *
   * @param text The input text
   *
   * @return text
   *
   * @throws IllegalArgumentException If the text is not a valid ID
   * @see #isValid(String)
   */

  public static String checkValid(
    final String text)
  {
    if (isValid(text)) {
      return text;
    }
    throw new IllegalArgumentException(
      String.format("Not a valid passkey (must match %s)", VALID_NAME));
  }
}
