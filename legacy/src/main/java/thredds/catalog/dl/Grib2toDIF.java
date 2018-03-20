/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: Grib2toDIF.java 48 2006-07-12 16:15:40Z caron $

package thredds.catalog.dl;

import ucar.nc2.constants.CDM;

import java.util.HashMap;
import java.io.*;
import java.util.Map;

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

  private Map<String, String> hash = new HashMap<>();
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

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, CDM.utf8Charset));

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
    return hash.get(from);
  }

  public static void main(String args[]) throws IOException {
    new Grib2toDIF("C:/dev/thredds/resourcespath");
  }

}
