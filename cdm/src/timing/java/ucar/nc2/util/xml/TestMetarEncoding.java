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

package ucar.nc2.util.xml;

import timing.Stat;

import java.io.File;
import java.util.Calendar;
import java.util.Timer;

import ucar.nc2.util.IO;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestMetarEncoding extends java.util.TimerTask {
  boolean show = false;

  String[] acceptStrings = new String[] {"raw", "csv", "xml", "netcdf", "netcdfStream"};
  int count = 0;

  String server = "http://motherlode.ucar.edu:9080/thredds/ncss/metars?";
  String vars = "variables=some&var=air_pressure_at_sea_level&var=air_temperature&var=dew_point_temperature&var=hectoPascal_ALTIM&var=precipitation_amount_hourly&var=visibility_in_air&var=weather&var=wind_from_direction&var=wind_peak_speed&var=wind_speed";

  String bbox = "&spatial=bb&north=90&west=-10&east=50&south=35";
  String timePoint = "&temporal=point&time=2007-12-05T01%3A00%3A00Z";

  String timeRange = "&time_start=2007-12-06T00%3A00%3A00Z&time_end=2007-12-06T23%3A59%3A59Z";

  String testA = "http://motherlode.ucar.edu:8080/thredds/ncss/metars?variables=all&spatial=all&time_start=2007-09-29T00%3A00%3A00Z&time_end=2007-09-29T23%3A59%3A59Z&accept=netcdf";


  // A: time series at one station
  private void doTestA(String stn, String accept, timing.Stat stat_time, timing.Stat stat_size) {
    String url = server + vars+ "&stn=" + stn + "&accept=" + accept;
    String filenameOut = makeFilename( accept, "A");
    File f = new File(filenameOut);
    if (show) System.out.println("Read = " + url + " to " + filenameOut);

    long start = System.currentTimeMillis();
    String result = IO.readURLtoFile(url, f);
    double took = .001 * (System.currentTimeMillis() - start);
    if (show) {
      System.out.println(result);
      System.out.println(" that took = " + took + "sec");
      System.out.println(" file size = " + f.length());
    }
    stat_time.sample(took); // secs
    stat_size.sample(f.length()/1000);  // kb
  }

  // B: bb for one time point
  private void doTestB(String accept, timing.Stat stat_time, timing.Stat stat_size) {
    String url = server + vars + bbox + timePoint + "&accept=" + accept;
    String filenameOut = makeFilename( accept, "B");
    File f = new File(filenameOut);
    if (show) System.out.println("Read = " + url + " to " + filenameOut);

    long start = System.currentTimeMillis();
    String result = IO.readURLtoFile(url, f);
    double took = .001 * (System.currentTimeMillis() - start);
    if (show) {
      System.out.println(result);
      System.out.println(" that took = " + took + "sec");
      System.out.println(" file size = " + f.length());
    }
    stat_time.sample(took);
    stat_size.sample(f.length()/1000);
  }

  // C: complete 1 day worth of metars.
  private void doTestC(String accept, timing.Stat stat_time, timing.Stat stat_size) {
    String url = server + vars + timeRange + "&accept=" + accept;
    String filenameOut = makeFilename( accept, "C");
    File f = new File(filenameOut);
    if (show) System.out.println("Read = " + url + " to " + filenameOut);

    long start = System.currentTimeMillis();
    String result = IO.readURLtoFile(url, f);
    double took = .001 * (System.currentTimeMillis() - start);
    if (show) {
      System.out.println(result);
      System.out.println(" that took = " + took + "sec");
      System.out.println(" file size = " + f.length());
    }
    stat_time.sample(took);
    stat_size.sample(f.length()/1000);
  }

  private String makeFilename( String accept, String run) {
    String suffix = accept.startsWith("netcdf") ? "nc" : accept;
    return "C:/temp/metars/" + accept + run + "."+suffix;
  }

  private String makeTimeRange(String base) {
    return "&time_start=" + base + "T00%3A00%3A00Z&time_end=" + base + "T23%3A59%3A59Z";
  }

  public void runA() {
    for (String name : acceptStrings)
      doTestA("LOWW", name, Stat.factory(name+"_A_time"), Stat.factory(name+"_A_size"));

    System.out.println("\nResults after "+count+" runs:");
    for (String name : acceptStrings)
      System.out.println(" "+Stat.factory(name+"_A_time")+"; "+Stat.factory(name+"_A_size"));
  }

  public void runB() {
    for (String name : acceptStrings)
      doTestB(name, Stat.factory(name+"_B_time"), Stat.factory(name+"_B_size"));

    System.out.println("\nResults after "+count+" runs:");
    for (String name : acceptStrings)
      System.out.println(" "+Stat.factory(name+"_B_time")+"; "+Stat.factory(name+"_B_size"));
  }

  public void runC() {
    for (String name : acceptStrings)
      doTestC(name, Stat.factory(name+"_C_time"), Stat.factory(name+"_C_size"));

    System.out.println("\nResults after "+count+" runs:");
    for (String name : acceptStrings)
      System.out.println(" "+Stat.factory(name+"_C_time")+"; "+Stat.factory(name+"_C_size"));
  }

    public void run() {
      count++;
      runA();
      runB();
      runC();
    }

  static public void main(String[] args) {
    Calendar c = Calendar.getInstance(); // contains current startup time
    //c.add(Calendar.SECOND, 30); // starting in 30 seconds
    Timer timer = new Timer();
    timer.schedule(new TestMetarEncoding(), c.getTime(), (long) 1000 * 120); // delay 2 min between runs
  }

}
