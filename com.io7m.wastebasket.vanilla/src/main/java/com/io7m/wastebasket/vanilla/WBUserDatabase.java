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

import com.io7m.wastebasket.api.WBPassKey;
import com.io7m.wastebasket.api.WBUserDatabaseType;
import com.io7m.wastebasket.api.WBUserName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

public final class WBUserDatabase implements WBUserDatabaseType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WBUserDatabase.class);

  private final AtomicBoolean done = new AtomicBoolean(false);
  private final Executor watchExecutor;
  private final Path file;
  private volatile Properties properties;

  private WBUserDatabase(
    final Executor inWatchExecutor,
    final Path inFile)
  {
    this.watchExecutor =
      Objects.requireNonNull(inWatchExecutor, "watchExecutor");
    this.file =
      Objects.requireNonNull(inFile, "file");

    this.properties = new Properties();
    this.watchExecutor.execute(this::run);
  }

  private void run()
  {
    while (!this.done.get()) {
      boolean failed = false;
      final Properties newProperties = new Properties();
      try (InputStream stream = Files.newInputStream(this.file)) {
        newProperties.load(stream);
      } catch (final IOException e) {
        LOG.error("i/o error: ", e);
        failed = true;
      }

      if (!failed) {
        this.properties = newProperties;
      }

      try {
        Thread.sleep(5_000L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public static WBUserDatabaseType create(
    final Executor watchExecutor,
    final Path file)
  {
    return new WBUserDatabase(watchExecutor, file);
  }

  @Override
  public void close()
  {
    this.done.set(true);
  }

  @Override
  public boolean authenticate(
    final WBUserName user,
    final WBPassKey password)
  {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(password, "password");

    final var passwordData = (String) this.properties.get(user.value());
    if (passwordData == null) {
      return false;
    }

    final var segments = List.of(passwordData.split(":"));
    if (segments.size() != 4) {
      LOG.error("could not parse password data for {}", user);
      return false;
    }

    final String algo = segments.get(0);
    switch (algo) {
      case "pbkdf2": {
        try {
          final byte[] salt = Hex.decode(segments.get(1));
          final int param = Integer.parseInt(segments.get(2));

          final PBEKeySpec spec =
            new PBEKeySpec(
              password.value().toCharArray(),
              salt,
              param,
              128);
          final SecretKeyFactory skf =
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

          final var storedHash = Hex.decode(segments.get(3));
          final var receivedHash = skf.generateSecret(spec).getEncoded();
          return (slowEquals(storedHash, receivedHash));
        } catch (final Exception e) {
          LOG.error("error checking password: ", e);
          return false;
        }
      }
      default: {
        LOG.error("unsupported algorithm {}", algo);
        return false;
      }
    }
  }

  @Override
  public void userAdd(
    final WBUserName user,
    final WBPassKey password)
    throws GeneralSecurityException, IOException
  {
    final var secureRandom = new SecureRandom();

    final byte[] salt = new byte[16];
    secureRandom.nextBytes(salt);

    final int iterationCount = 10_000;
    final PBEKeySpec spec =
      new PBEKeySpec(
        password.value().toCharArray(),
        salt,
        iterationCount,
        128);
    final SecretKeyFactory skf =
      SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    final var receivedHash =
      skf.generateSecret(spec).getEncoded();

    final Properties loadedProps = new Properties();
    try (InputStream stream = Files.newInputStream(this.file)) {
      loadedProps.load(stream);
    } catch (final NoSuchFileException e) {
      LOG.info("{} does not exist; creating a new database", this.file);
    }

    loadedProps.setProperty(
      user.value(),
      String.format(
        "%s:%s:%d:%s",
        "pbkdf2",
        Hex.toHexString(salt),
        Integer.valueOf(iterationCount),
        Hex.toHexString(receivedHash)
      ));

    this.replaceDatabase(loadedProps);
  }

  private void replaceDatabase(
    final Properties newProperties)
    throws IOException
  {
    final Path userDatabaseTmp =
      this.file.resolveSibling(this.file.getFileName() + ".tmp");

    try (OutputStream outputStream =
           Files.newOutputStream(userDatabaseTmp, CREATE_NEW, WRITE)) {
      newProperties.store(outputStream, "");
    }

    LOG.info(
      "replacing user database {} -> {}",
      userDatabaseTmp,
      this.file);

    Files.move(
      userDatabaseTmp,
      this.file,
      ATOMIC_MOVE,
      REPLACE_EXISTING);
  }

  @Override
  public void userDelete(
    final WBUserName user)
    throws IOException
  {
    final Properties loadedProps = new Properties();
    try (InputStream stream = Files.newInputStream(this.file)) {
      loadedProps.load(stream);
    }

    loadedProps.remove(user.value());
    this.replaceDatabase(loadedProps);
  }

  private static boolean slowEquals(
    final byte[] a,
    final byte[] b)
  {
    int diff = a.length ^ b.length;
    for (int index = 0; index < a.length && index < b.length; ++index) {
      diff |= (int) a[index] ^ (int) b[index];
    }
    return diff == 0;
  }
}
