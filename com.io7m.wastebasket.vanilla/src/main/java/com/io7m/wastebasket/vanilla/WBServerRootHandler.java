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

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class WBServerRootHandler extends AbstractHandler
{
  WBServerRootHandler()
  {

  }

  private static String version()
  {
    final var pack = WBServerRootHandler.class.getPackage();
    if (pack != null) {
      final var name = pack.getImplementationVersion();
      if (name != null) {
        return name;
      }
    }
    return "0.0.0";
  }

  private static final String VERSION_TEXT =
    String.format("Wastebasket %s\r\n", version());

  @Override
  public void handle(
    final String target,
    final Request baseRequest,
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");
    try (ServletOutputStream stream = response.getOutputStream()) {
      stream.write(VERSION_TEXT.getBytes(UTF_8));
    }
    baseRequest.setHandled(true);
  }
}
