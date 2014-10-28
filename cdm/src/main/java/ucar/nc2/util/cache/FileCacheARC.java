/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.cache;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keep cache of open FileCacheable objects, for example NetcdfFile.
 * The FileCacheable object typically contains a RandomAccessFile object that wraps a system resource like a file handle.
 * These are left open when the FileCacheable is in the cache. The maximum number of these is bounded, though not strictly.
 * A cleanup routine reduces cache size to a minimum number. This cleanup is called periodically in a background thread,
 * and also when the maximum cache size is reached.
 * <ol>
 * <li>The FileCacheable object must not be modified.
 * <li>The hashKey must uniquely define the FileCacheable object.
 * <li>The location must be usable in a FileFactory.open().
 * <li>If the FileCacheable is acquired from the cache (ie already open), getLastModified() is used to see if it has changed,
 *     and is discarded if so.
 * <li>Make sure you call shutdown() when exiting the program, in order to shut down the cleanup thread.
 * </ol>
 * <p/>
 * Normal usage is through the NetcdfDataset interface:
 * <pre>
 * NetcdfDataset.initNetcdfFileCache(...); // on application startup
 * ...
 * NetcdfFile ncfile = null;
 * try {
 *   ncfile = NetcdfDataset.acquireFile(location, cancelTask);
 *   ...
 * } finally {
 *   if (ncfile != null) ncfile.close();
 * }
 * ...
 * NetcdfDataset.shutdown();  // when terminating the application
 * </pre>
 * All methods are thread safe.
 * Cleanup is done automatically in a background thread, using LRU algorithm.
 *
 *
 * @author caron
 * @since 10/28/2014
 */
@ThreadSafe
public class FileCacheARC extends FileCache {

  public FileCacheARC(String name, int minElementsInMemory, int softLimit, int hardLimit, int period) {
    super(name, minElementsInMemory, softLimit, hardLimit, period);
  }
}