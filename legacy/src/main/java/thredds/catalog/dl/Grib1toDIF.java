/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: Grib1toDIF.java 48 2006-07-12 16:15:40Z caron $

package thredds.catalog.dl;

import ucar.nc2.constants.CDM;

import java.util.HashMap;
import java.util.Map;
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

  private Map<String, String> hash = new HashMap<>();
  private int maxLines = 1000;

  private Grib1toDIF() throws IOException {
    Class c = getClass();
    String resourceName = "/resources/thredds/dl/GRIB-GCMD.csv";
    InputStream ios = c.getResourceAsStream(resourceName);
    if (ios == null)
      throw new IOException("Cant find resource= "+resourceName);

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, CDM.utf8Charset));
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
    return hash.get(paramNo);
  }
}
