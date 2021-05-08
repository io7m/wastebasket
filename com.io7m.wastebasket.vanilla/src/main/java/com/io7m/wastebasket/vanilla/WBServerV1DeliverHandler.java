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
import com.io7m.wastebasket.api.WBBlobID;
import com.io7m.wastebasket.api.WBBlobStoreType;
import com.io7m.wastebasket.api.WBPassKey;
import com.io7m.wastebasket.api.WBServerConfiguration;
import com.io7m.wastebasket.api.WBUserDatabaseType;
import com.io7m.wastebasket.api.WBUserName;
import org.apache.commons.io.input.BoundedInputStream;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 * A V1 deliver handler.
 */

public final class WBServerV1DeliverHandler extends AbstractHandler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WBServerV1DeliverHandler.class);
  private static final ZoneId UTC =
    ZoneId.of("UTC");

  private static final Pattern SLASHES = Pattern.compile("/+");
  private final WBServerConfiguration configuration;
  private final WBBlobStoreType database;
  private final WBUserDatabaseType userDatabase;
  private final WBAuditLogType auditLog;

  WBServerV1DeliverHandler(
    final WBServerConfiguration inConfiguration,
    final WBBlobStoreType inDatabase,
    final WBUserDatabaseType inUserDatabase,
    final WBAuditLogType inAuditLog)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.database =
      Objects.requireNonNull(inDatabase, "inDatabase");
    this.userDatabase =
      Objects.requireNonNull(inUserDatabase, "userDatabase");
    this.auditLog =
      Objects.requireNonNull(inAuditLog, "auditLog");
  }

  @Override
  public void handle(
    final String target,
    final Request baseRequest,
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    if (!Objects.equals(baseRequest.getMethod(), "POST")) {
      response.sendError(SC_METHOD_NOT_ALLOWED, "Must use POST");
      return;
    }

    final var userName = baseRequest.getHeader("X-UserName");
    final var password = baseRequest.getHeader("X-PassKey");
    if (userName == null || password == null) {
      response.sendError(
        SC_UNAUTHORIZED,
        "Must specify a username and passkey");
      return;
    }

    if (!this.userDatabase.authenticate(
      WBUserName.of(userName),
      WBPassKey.of(password))) {
      response.sendError(SC_UNAUTHORIZED, "Authentication failed");
      return;
    }

    final long sizeProvided = baseRequest.getContentLengthLong();
    final long sizeLimit = this.configuration.dataSizeLimit();
    if (Long.compareUnsigned(sizeProvided, sizeLimit) >= 0) {
      response.sendError(SC_REQUEST_ENTITY_TOO_LARGE, "Data is too large");
      return;
    }

    final var idText =
      SLASHES.matcher(baseRequest.getPathInfo())
        .replaceFirst("");

    this.writeData(
      baseRequest,
      request,
      response,
      userName,
      sizeProvided,
      sizeLimit,
      WBBlobID.of(idText));
  }

  private void writeData(
    final Request baseRequest,
    final HttpServletRequest request,
    final HttpServletResponse response,
    final String userName,
    final long sizeProvided,
    final long sizeLimit,
    final WBBlobID id)
    throws IOException
  {
    this.auditLog.dataWritten(
      OffsetDateTime.now(UTC),
      userName,
      baseRequest.getRemoteAddr(),
      baseRequest.getRemotePort(),
      id.value(),
      sizeProvided
    );

    if (LOG.isInfoEnabled()) {
      LOG.info(
        "write {} {}:{} {} {}",
        userName,
        baseRequest.getRemoteAddr(),
        Integer.valueOf(baseRequest.getRemotePort()),
        id.value(),
        Long.toUnsignedString(sizeProvided));
    }

    try (DigestOutputStream outputStream = this.database.open(id)) {
      try (ServletInputStream inputStream = request.getInputStream()) {
        try (BoundedInputStream bounded = new BoundedInputStream(
          inputStream,
          sizeLimit)) {
          bounded.transferTo(outputStream);
        }
      }
      outputStream.flush();

      final var digest = outputStream.getMessageDigest();
      try (OutputStream servletOut = response.getOutputStream()) {
        servletOut.write("SHA-256: ".getBytes(UTF_8));
        servletOut.write(Hex.encode(digest.digest()));
        servletOut.write("\r\n".getBytes(UTF_8));
        servletOut.flush();
      }

      response.setStatus(200);
      baseRequest.setHandled(true);
    } catch (final FileAlreadyExistsException e) {
      LOG.error("file already exists: ", e);
      response.sendError(SC_CONFLICT, "ID already used");
      return;
    } catch (final NoSuchAlgorithmException e) {
      LOG.error("no such algorithm: ", e);
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Unsupported JVM");
      return;
    }
  }
}
