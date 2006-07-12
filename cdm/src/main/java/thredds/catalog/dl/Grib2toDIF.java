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
