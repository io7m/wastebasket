/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.wastebasket.cmdline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Console;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main command-line program.
 */

public final class Main implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private final Map<String, CommandType> commands;
  private final JCommander commander;
  private final String[] args;
  private final StringConsole console;
  private int exit_code;

  /**
   * Construct a new main program.
   *
   * @param in_args Command-line arguments
   */

  public Main(final String[] in_args)
  {
    this.args = Objects.requireNonNull(in_args, "args");
    this.console = new StringConsole();

    final var r = new CommandRoot();
    final var cmd_server = new CommandServer();
    final var cmd_user_add = new CommandUserAdd();
    final var cmd_user_delete = new CommandUserDelete();

    this.commands = new HashMap<>(8);
    this.commands.put("server", cmd_server);
    this.commands.put("user-add", cmd_user_add);
    this.commands.put("user-delete", cmd_user_delete);

    this.commander = new JCommander(r);
    this.commander.setConsole(this.console);
    this.commander.setProgramName("wastebasket");
    this.commander.addCommand("server", cmd_server);
    this.commander.addCommand("user-add", cmd_user_add);
    this.commander.addCommand("user-delete", cmd_user_delete);
  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(final String[] args)
  {
    final var cm = new Main(args);
    cm.run();
    System.exit(cm.exitCode());
  }

  /**
   * @return The program exit code
   */

  public int exitCode()
  {
    return this.exit_code;
  }

  @Override
  public void run()
  {
    try {
      this.commander.parse(this.args);

      final var cmd = this.commander.getParsedCommand();
      if (cmd == null) {
        this.commander.usage();
        LOG.info("Arguments required.\n{}", this.console.builder().toString());
        return;
      }

      final var command = this.commands.get(cmd);
      command.call();
    } catch (final ParameterException e) {
      this.commander.usage();
      LOG.error("{}\n{}", e.getMessage(), this.console.builder().toString());
      this.exit_code = 1;
    } catch (final Exception e) {
      LOG.error("{}", e.getMessage(), e);
      this.exit_code = 1;
    }
  }
}
