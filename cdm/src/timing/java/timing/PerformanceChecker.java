/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package timing;

import java.util.*;

/**
 * The PerformanceChecker tries to run the task as often as possible
 * in the allotted time.  It then returns the number of times that
 * the task was called.  To make sure that the timer stops the test
 * timeously, we check that the difference between start and end
 * time is less than EPSILON.  After it has tried unsuccessfully for
 * MAXIMUM_ATTEMPTS times, it throws an exception.
 *
 * @author Heinz Kabutz
 * @since 2006/03/27
 */
public class PerformanceChecker {
  /**
   * Whether the test should continue running.  Will expire after
   * some time specified in testTime.  Needs to be volatile for
   * visibility.
   */

  private volatile boolean expired = false;
  /**
   * The number of milliseconds that each test should run
   */
  private final long testTime;
  /**
   * The task to execute for the duration of the test run.
   */
  private final Runnable task;
  /**
   * Accuracy of test.  It must finish within 20ms of the testTime
   * otherwise we retry the test.  This could be configurable.
   */

  public static final int EPSILON = 20;
  /**
   * Number of repeats before giving up with this test.
   */
  private static final int MAXIMUM_ATTEMPTS = 3;

  /**
   * Set up the number of milliseconds that the test should run, and
   * the task that should be executed during that time.  The task
   * should ideally run for less than 10ms at most, otherwise you
   * will get too many retry attempts.
   *
   * @param testTime the number of milliseconds that the test should
   *                 execute.
   * @param task     the task that should be executed repeatedly
   *                 until the time is used up.
   */
  public PerformanceChecker(long testTime, Runnable task) {
    this .testTime = testTime;
    this .task = task;
  }

  /**
   * Start the test, and after the set time interrupt the test and
   * return the number of times that we were able to execute the
   * run() method of the task.
   */

  public long start() {
    long numberOfLoops;
    long start;
    int runs = 0;
    do {
      if (++runs > MAXIMUM_ATTEMPTS) {
        throw new IllegalStateException("Test not accurate");
      }
      expired = false;
      start = System.currentTimeMillis();
      numberOfLoops = 0;
      Timer timer = new Timer();
      timer.schedule(
        new TimerTask() {
          public void run() {
            expired = true;
          }
        }, testTime);

      while (!expired) {
        task.run();
        numberOfLoops++;
      }
      start = System.currentTimeMillis() - start;
      timer.cancel();
    } while (Math.abs(start - testTime) > EPSILON);

    collectGarbage();
    return numberOfLoops;
  }

  /**
   * After every test run, we collect garbage by calling System.gc()
   * and sleeping for a short while to make sure that the garbage
   * collector has had a chance to collect objects.
   */

  private void collectGarbage() {
    for (int i = 0; i < 3; i++) {
      System.gc();
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
}
  
