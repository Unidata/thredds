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
import java.util.StringTokenizer;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * read in GRIB-1 to DIF csv file
 * @author john
 */
public class Grib1toDIF implements VocabTranslator {
  private static Grib1toDIF singleton;

  public static Grib1toDIF getInstance() throws IOException {
    if (singleton == null) {
      singleton = new Grib1toDIF();
    }
    return singleton;
  }

  private HashMap hash = new HashMap();
  private int maxLines = 1000;

  private Grib1toDIF() throws IOException {
    Class c = getClass();
    String resourceName = "/resources/thredds/dl/GRIB-GCMD.csv";
    InputStream ios = c.getResourceAsStream(resourceName);
    if (ios == null)
      throw new IOException("Cant find resource= "+resourceName);

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;
    while (count < maxLines) {
      String line = dataIS.readLine();
      if (line == null) break;

      StringTokenizer stoker = new StringTokenizer( line, ",");
      String category = stoker.nextToken().trim();
      String topic = stoker.nextToken().trim();
      String term = stoker.nextToken().trim();
      String var = stoker.nextToken().trim();
      String gribNo = stoker.nextToken().trim();

      String difParam = category +" > " +topic +" > " + term +" > " + var;
      hash.put( gribNo, difParam);
      count++;
    }

    dataIS.close();
  }


  public String translate(String fromId) {
    int pos = fromId.lastIndexOf(","); // the last number is the
    String paramNo = fromId.substring(pos+1).trim();
    return (String) hash.get(paramNo);
  }
}
