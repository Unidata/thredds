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
package ucar.nc2.dt;

import ucar.nc2.dt.point.decode.MetarParseReport;
import ucar.nc2.util.Misc;

import java.io.*;
import java.util.Map;

public class TimeObsRaw {

  private static boolean showErrs = true;

  public static void main(String args[]) throws IOException {
    String url = "C:/TEMP/metars/save/rawC.raw";
    int count = 0, bad = 0;
    // DataInputStream fin = new DataInputStream(new BufferedInputStream(new FileInputStream(url), 10000));

    BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(url)), 10000);

    double sum = 0.0;
    long start = System.currentTimeMillis();
    try {

      while (true) {
        String line = fin.readLine();
        if (null == line) break;
        int pos = line.indexOf('=');
        if (pos > 0) line = line.substring(pos+2);

        // System.out.println(line);
        try {
          MetarParseReport parser = new MetarParseReport();
          if (!parser.parseReport( line)) {
            if (showErrs) System.out.println("*** failed on <" + line+">");
            bad++;
          }
          Map flds = parser.getFields();
          String valS = (String) flds.get("Temperature");
          if (valS != null) {
            double val = Double.parseDouble(valS);
            if (!Misc.closeEnough(val, -99999.0))
              sum += val; // LOOK kludge for missing data
          }

        } catch (Exception e) {
          if (showErrs) System.out.println("*** EXCEPTION " + line);
          bad++;
        }
        count++;
      }


      long took = System.currentTimeMillis() - start;
      System.out.println("that took = " + took + " msecs temperature sum= "+sum);


    } finally {
      if (fin != null)
        fin.close();
    }

    System.out.println("successfully read " + count + " records " + bad + " bad");
  }
}
