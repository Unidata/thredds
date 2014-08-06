/* Copyright */
package thredds.server.catalog.tracker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Put a watch on directories for when catalogs change. not used yet
 *
 * @author caron
 * @since 6/9/2015
 */
public class CatalogWatcher implements Closeable {

  private final WatchService watcher;
  private final DatasetTracker tracker;
  private final boolean enable;
  private final Map<WatchKey, Path> keys;
  private boolean recursive = true;
  private boolean trace = true;

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  public CatalogWatcher(DatasetTracker tracker, boolean enable) throws IOException {
    this.tracker = tracker;
    this.enable = enable;
    this.watcher = FileSystems.getDefault().newWatchService();
    this.keys = new HashMap<>();

    // enable trace after initial registration
    this.trace = true;
  }

  /**
   * Register the given directory with the WatchService
   */
  public void register(Path dir) throws IOException {
    if (!enable) return;

    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    if (trace) {
      Path prev = keys.get(key);
      if (prev == null) {
        System.out.format("CatalogWatcher register: %s%n", dir);
      } else {
        if (!dir.equals(prev)) {
          System.out.format("update: %s -> %s%n", prev, dir);
        }
      }
    }
    keys.put(key, dir);
  }

  /**
   * Register the given directory, and all its sub-directories, with the
   * WatchService.
   */
  public void registerAll(final Path start) throws IOException {
    if (!enable) return;

    // register directory and sub-directories
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Process all events for keys queued to the watcher
   */
  public void processEvents() {
    if (!enable) return;

    for (; ; ) {

      // wait for key to be signalled
      WatchKey key;
      try {
        key = watcher.take();
      } catch (InterruptedException x) {
        return;
      }

      Path dir = keys.get(key);
      if (dir == null) {
        System.err.println("WatchKey not recognized!!");
        continue;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind kind = event.kind();

        // TBD - provide example of how OVERFLOW event is handled
        if (kind == OVERFLOW) {
          continue;
        }

        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();
        Path child = dir.resolve(name);

        // print out event
        System.out.format("%s: %s%n", event.kind().name(), child);

        // if directory is created, and watching recursively, then
        // register it and its sub-directories
        if (recursive && (kind == ENTRY_CREATE)) {
          try {
            if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
              registerAll(child);
            }
          } catch (IOException x) {
            // ignore to keep sample readbale
          }
        }
      }

      // reset key and remove from set if directory no longer accessible
      boolean valid = key.reset();
      if (!valid) {
        keys.remove(key);

        // all directories are inaccessible
        if (keys.isEmpty()) {
          break;
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    watcher.close();
  }
}
