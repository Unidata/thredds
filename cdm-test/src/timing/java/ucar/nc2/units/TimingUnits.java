/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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

