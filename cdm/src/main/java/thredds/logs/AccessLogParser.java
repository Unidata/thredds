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

package thredds.logs;

import ucar.nc2.units.DateFormatter;

import java.util.regex.*;
import java.util.Date;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Read TDS access logs
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class AccessLogParser implements LogReader.LogParser {

  ///////////////////////////////////////////////////////
  // log reading

  // sample
  // 1                2   3                           4                                                                                                5   6  7     8               9
  // 128.117.140.75 - - [02/May/2008:00:46:26 -0600] "HEAD /thredds/dodsC/model/NCEP/DGEX/CONUS_12km/DGEX_CONUS_12km_20080501_1800.grib2.dds HTTP/1.1" 200 - "null" "Java/1.6.0_05" 21
  //

  // 30/Sep/2009:23:50:47 -0600
  private SimpleDateFormat formatFrom = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

  private static Pattern regPattern =
          Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+) - (.*) \\[(.*)\\] \"(.*)\" (\\d+) ([\\-\\d]+) \"(.*)\" \"(.*)\" (\\d+)");

  public LogReader.Log nextLog(BufferedReader dataIS) throws IOException {
    String line = dataIS.readLine();
    if (line == null) return null;

    try {
      //System.out.println("\n"+line);
      Matcher m = regPattern.matcher(line);
      if (m.matches()) {
        LogReader.Log log = new LogReader.Log();
        log.ip = m.group(1).intern();
        log.date = convertDate( m.group(3));
        String request = m.group(4).intern();
        log.returnCode = parse(m.group(5));
        log.sizeBytes = parseLong(m.group(6));
        log.referrer = m.group(7);
        log.client = m.group(8);
        log.msecs = parseLong(m.group(9));

        String[] reqss = request.split(" ");
        if (reqss.length == 3) {
          log.verb = reqss[0].intern();
          log.path = reqss[1].intern();
          log.http = reqss[2].intern();
        }

        return log;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Cant parse " + line);
    }
    return null;
  }

  private long convertDate(String accessDateFormat) {
    // 30/Sep/2009:23:50:47 -0600
    try {
      Date d = formatFrom.parse(accessDateFormat);
      return d.getTime(); // formatTo.toDateTimeStringISO(d);
    } catch (Throwable t) {
      System.out.printf("Bad date format = %s err = %s%n", accessDateFormat, t.getMessage());
    }
    return -1;
  }

  private int parse(String s) {
    if (s.equals("-")) return 0;
    return Integer.parseInt(s);
  }

  private long parseLong(String s) {
    if (s.equals("-")) return 0;
    return Long.parseLong(s);
  }

}
