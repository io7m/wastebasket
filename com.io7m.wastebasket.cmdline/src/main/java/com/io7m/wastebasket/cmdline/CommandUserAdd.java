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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.wastebasket.api.WBPassKey;
import com.io7m.wastebasket.api.WBUserDatabaseType;
import com.io7m.wastebasket.api.WBUserName;
import com.io7m.wastebasket.vanilla.WBUserDatabase;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Properties;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

@Parameters(commandDescription = "Add a user to the user database")
public final class CommandUserAdd extends CommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CommandUserAdd.class);

  // CHECKSTYLE:OFF

  @Parameter(
    names = "--user-database",
    required = true,
    description = "The file containing the user database")
  Path userDatabase;

  @Parameter(
    names = "--user-name",
    required = true,
    description = "The user name")
  String userName;

  @Parameter(
    names = "--user-passkey",
    required = true,
    description = "The user passkey")
  String passKey;

  CommandUserAdd()
  {

  }

  // CHECKSTYLE:ON

  @Override
  public Void call()
    throws Exception
  {
    super.call();

    final WBUserName user =
      WBUserName.of(this.userName);
    final WBPassKey pass =
      WBPassKey.of(this.passKey);

    try (final WBUserDatabaseType users =
           WBUserDatabase.create(x -> { }, this.userDatabase)) {
      users.userAdd(user, pass);
    }

    return null;
  }
}
