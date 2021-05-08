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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A file watcher.
 */

public final class WBFilesWatcher implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WBFilesWatcher.class);

  private final Executor executor;
  private final ConcurrentHashMap<Path, Instant> fileTimes;
  private final Runnable onChange;
  private final AtomicBoolean done;

  private WBFilesWatcher(
    final Executor inExecutor,
    final ConcurrentHashMap<Path, Instant> inFileTimes,
    final Runnable inOnChange)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.fileTimes =
      Objects.requireNonNull(inFileTimes, "fileTimes");
    this.onChange =
      Objects.requireNonNull(inOnChange, "onChange");

    this.done = new AtomicBoolean(false);
    this.executor.execute(this::run);
  }

  /**
   * Create a file watcher.
   *
   * @param executor The executor upon which to execute operations
   * @param files    The watched files
   * @param onChange The method executed on file changes
   *
   * @return A file watcher
   */

  public static WBFilesWatcher create(
    final Executor executor,
    final List<Path> files,
    final Runnable onChange)
  {
    Objects.requireNonNull(executor, "executor");
    Objects.requireNonNull(files, "files");
    Objects.requireNonNull(onChange, "onChange");

    final ConcurrentHashMap<Path, Instant> fileTimes =
      new ConcurrentHashMap<>(files.size());

    for (final var file : files) {
      try {
        fileTimes.put(file, Files.getLastModifiedTime(file).toInstant());
      } catch (final IOException e) {
        fileTimes.put(file, Instant.now());
      }
    }

    return new WBFilesWatcher(executor, fileTimes, onChange);
  }

  private void run()
  {
    while (!this.done.get()) {
      boolean different = false;

      for (final var entry : this.fileTimes.entrySet()) {
        final var prevTime = entry.getValue();
        final var file = entry.getKey();

        Instant currTime;
        try {
          currTime = Files.getLastModifiedTime(file).toInstant();
        } catch (final IOException e) {
          currTime = prevTime;
        }

        different = different || currTime.isAfter(prevTime);
        entry.setValue(currTime);
      }

      if (different) {
        try {
          this.onChange.run();
        } catch (final Exception e) {
          LOG.error("ignored exception from file watcher runnable: ", e);
        }
      }

      try {
        Thread.sleep(1_000L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void close()
  {
    this.done.set(true);
  }
}
