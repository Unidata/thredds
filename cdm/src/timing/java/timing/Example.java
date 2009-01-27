// $Id: Example.java 51 2006-07-12 17:13:13Z caron $
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

import timing.*;

import java.util.Random;

public class Example {
  private static final int RUNS = 10;
  private static final long TEST_TIME = 100;

  public static void main(String[] args) throws Exception {

    // throw one away to warm up the JVM
    test(1);

    test(1);
    test(10);
    test(100);
    test(1000);
    test(10000);
    test(100000);
  }

  private static void test(int length) {
    PerformanceHarness harness = new PerformanceHarness();
    Average arrayClone = harness.calculatePerf(
        new PerformanceChecker(TEST_TIME, new ArrayCloneTest(length)), RUNS);

    Average arrayNewAndCopy = harness.calculatePerf(
        new PerformanceChecker(TEST_TIME, new ArrayNewAndCopyTest(length)), RUNS);

    Average arrayNewAndCopy2 = harness.calculatePerf(
        new PerformanceChecker(TEST_TIME, new ArrayNewAndCopyTest2(length)), RUNS);

    System.out.println("Length=" + length);
    System.out.println(" Clone avg= "+ arrayClone.mean()+ " dev= "+arrayClone.stddev());
    System.out.println(" Copy  avg= "+ arrayNewAndCopy.mean()+ " dev= "+arrayNewAndCopy.stddev());
    System.out.println(" Copy/Clone  avg= "+ arrayNewAndCopy.mean() / arrayClone.mean());
    System.out.println(" Copy2  avg= "+ arrayNewAndCopy2.mean()+ " dev= "+arrayNewAndCopy2.stddev());
    System.out.println(" Copy2/Clone  avg= "+ arrayNewAndCopy2.mean() / arrayClone.mean());
    System.out.println();
  }

  private static class ArrayCloneTest implements Runnable {
    private final byte[] byteValue;

    public ArrayCloneTest(int length) {
      byteValue = new byte[length];
      // always the same set of bytes...
      new Random(0).nextBytes(byteValue);
    }

    public void run() {
      byte[] result = (byte[]) byteValue.clone();
    }
  }

  private static class ArrayNewAndCopyTest implements Runnable {
    private final byte[] byteValue;

    public ArrayNewAndCopyTest(int length) {
      byteValue = new byte[length];
      // always the same set of bytes...
      new Random(0).nextBytes(byteValue);
    }

    public void run() {
      byte[] b = new byte[byteValue.length];
      System.arraycopy(byteValue, 0, b, 0, byteValue.length);
    }
  }

  private static class ArrayNewAndCopyTest2 implements Runnable {
    private final byte[] byteValue;

    public ArrayNewAndCopyTest2(int length) {
      byteValue = new byte[length];
      // always the same set of bytes...
      new Random(0).nextBytes(byteValue);
    }

    public void run() {
      Class cls = byteValue.getClass();
      if (cls.isArray()) {
        if (!cls.getComponentType().isAssignableFrom(Object.class)) {
          byte[] b = new byte[byteValue.length];
          System.arraycopy(byteValue, 0, b, 0, byteValue.length);
          return;
        }
      }
      throw new RuntimeException();
    }
  }
}

