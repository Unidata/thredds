// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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

