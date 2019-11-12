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

import com.beust.jcommander.internal.Console;

public final class StringConsole implements Console
{
  private StringBuilder builder;

  public StringConsole()
  {
    this.builder = new StringBuilder(128);
  }

  public StringBuilder builder()
  {
    return this.builder;
  }

  @Override
  public void print(final String s)
  {
    this.builder.append(s);
  }

  @Override
  public void println(final String s)
  {
    this.builder.append(s);
    this.builder.append(System.lineSeparator());
  }

  @Override
  public char[] readPassword(final boolean b)
  {
    return new char[0];
  }
}
