package ucar.nc2.dt;

import ucar.nc2.dt.point.decode.MetarParseReport2;

import java.io.*;

public class TimeObsRaw {

  private static boolean showErrs = false;

  public static void main(String args[]) throws IOException {
    String url = "C:/data/metars/raw.txt";
    int count = 0, bad = 0;
    // DataInputStream fin = new DataInputStream(new BufferedInputStream(new FileInputStream(url), 10000));

    BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(url)), 10000);

    long start = System.currentTimeMillis();
    try {

      while (true) {
        String line = fin.readLine();
        if (null == line) break;

        // System.out.println(line);
        try {
          MetarParseReport2 parser = new MetarParseReport2();
          parser.parseReport(line);
          if (null == parser.getFields()) {
            if (showErrs) System.out.println("*** NULL " + line);
            bad++;
          }
        } catch (Exception e) {
          if (showErrs) System.out.println("*** EXCEPTION " + line);
          bad++;
        }
        count++;
      }


      long took = System.currentTimeMillis() - start;
      System.out.println("that took = " + took + " msecs");


    } finally {
      if (fin != null)
        fin.close();
    }

    System.out.println("successfully read " + count + " records " + bad + " bad");
  }
}
