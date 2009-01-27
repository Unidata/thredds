// $Id: TimingUnits.java 51 2006-07-12 17:13:13Z caron $
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

package ucar.nc2.units;

import timing.*;
import ucar.units.UnitFormatManager;
import ucar.units.UnitFormat;

public class TimingUnits {
  private static final int RUNS = 10;
  private static final long TEST_TIME = 100;

  public static void main(String[] args) throws Exception {
    test(1);
    test(1);
  }

  private static void test(int length) {
    PerformanceHarness harness = new PerformanceHarness();

    Average dateUnit = harness.calculatePerf(
        new PerformanceChecker(TEST_TIME, new DateUnitTest("sec since 2006-01-01T12:23:00")), RUNS);

    Average udUnit = harness.calculatePerf(
        new PerformanceChecker(TEST_TIME, new UdunitsTest("sec since 2006-01-01T12:23")), RUNS);

    Average dateForm = harness.calculatePerf(
        new PerformanceChecker(TEST_TIME, new DateFormatterTest("2006-01-01T12:23")), RUNS);


    System.out.println(" date avg= "+ dateUnit.mean()+ " dev= "+dateUnit.stddev());
    System.out.println(" udUnit  avg= "+ udUnit.mean()+ " dev= "+udUnit.stddev());
    System.out.println(" udUnit/date  avg= "+ udUnit.mean() / dateUnit.mean());
    System.out.println(" dateForm  avg= "+ dateForm.mean()+ " dev= "+dateForm.stddev());
    System.out.println(" dateForm/udUnit  avg= "+ dateForm.mean() / udUnit.mean());
    System.out.println();
  }

  private static class DateUnitTest implements Runnable {
    private final String s;

    public DateUnitTest(String s) {
      this.s = s;
    }

    public void run() {
      try {
        new DateUnit(s);
      } catch (Exception e) {
        System.out.println(" error on parse"+e.getMessage());
      }
    }
  }

  private static class UdunitsTest implements Runnable {
    private static UnitFormat format = UnitFormatManager.instance();
    private final String s;

    public UdunitsTest(String s) {
      this.s = s;
    }

    public void run() {
      try {
        format.parse( s);
      } catch (Exception e) {
        System.out.println(" error on parse"+e.getMessage());
      }
    }
  }

  private static class DateFormatterTest implements Runnable {
    private static DateFormatter format = new DateFormatter();
    private final String s;

    public DateFormatterTest(String s) {
      this.s = s;
    }

    public void run() {
      try {
        format.getISODate(s);
      } catch (Exception e) {
        System.out.println(" error on parse"+e.getMessage());
      }
    }
  }

}

