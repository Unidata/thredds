// $Id: Grib2toDIF.java 48 2006-07-12 16:15:40Z caron $
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
package thredds.catalog.dl;

import java.util.HashMap;
import java.io.*;

/**
 * read in GRIB-2 to DIF csv file
 * @author john
 */
public class Grib2toDIF implements VocabTranslator {
  private static Grib2toDIF singleton;

  public static Grib2toDIF getInstance() throws IOException {
    if (singleton == null) {
      singleton = new Grib2toDIF(null);
    }
    return singleton;
  }

  private HashMap hash = new HashMap();
  private int maxLines = 1000;
  private boolean debug = false;

  public Grib2toDIF(String dir) throws IOException {
    String resourceName = "/resources/thredds/dl/GRIB2-GCMD.csv";
    InputStream ios;
    if (null != dir) {
      ios = new FileInputStream(dir+resourceName);
    } else {
      Class c = getClass();
      ios = c.getResourceAsStream(resourceName);
      if (ios == null)
        throw new IOException("Cant find resource= "+resourceName);
    }

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    String discipline = null, category = null, param = null;

    int maxTokens = 8;
    String[] tokens = new String[maxTokens];

    int count = 0;
    int countDiscipline = -1;
    int countCategory = 0;
    int countParam = 0;
    while (count < maxLines) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (debug) System.out.println("line=  "+line);

      int countTokens = 0;
      int place = 0;
      while (countTokens < maxTokens) {
        int pos = line.indexOf(',',place);
        if (pos >= 0)
          tokens[countTokens++] = line.substring(place,pos);
        else
          tokens[countTokens++] = line.substring(place);
        place=pos+1;
      }

      if (tokens[0].length() > 0) {
        discipline = tokens[0];
        countDiscipline++;
        countCategory = -1;
        countParam = -1;
      }

      if (tokens[1].length() > 0) {
        category = tokens[1];
        countCategory++;
        countParam = -1;
      }

      if (tokens[2].length() > 0) {
        param = tokens[2];
        countParam++;
      }

      String difParam = "Earth Science > "+tokens[7];
      if (!difParam.equalsIgnoreCase("n/a")) {
        String gribId = "2,"+countDiscipline+","+countCategory+","+countParam;
        hash.put( gribId, difParam);
        if (debug) System.out.println(" adding "+discipline+":"+category+":"+param+" = "+gribId+" = "+difParam);
      }

      count++;
    }

    dataIS.close();
  }


  public String translate(String from) {
    return (String) hash.get(from);
  }

  public static void main(String args[]) throws IOException {
    new Grib2toDIF("C:/dev/thredds/resourcespath");
  }

}
