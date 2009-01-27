// $Id: Grib1toDIF.java 48 2006-07-12 16:15:40Z caron $
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
