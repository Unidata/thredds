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
