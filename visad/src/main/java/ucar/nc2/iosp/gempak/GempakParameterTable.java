/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.gempak;


import ucar.nc2.constants.CDM;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class to hold a lookup of gempak parameters
 *
 * @author IDV Development Team
 */
public class GempakParameterTable {

  /**
   * table to hold the  values
   */
  private HashMap<String, GempakParameter> paramMap = new HashMap<>(256);

  /**
   * table to hold the template values
   */
  private HashMap<String, GempakParameter> templateParamMap = new HashMap<>(20);

  /**
   * indices of breakpoints in the table
   */
  private static int[] indices = {
          0, 4, 38, 59, 72, 79, 90, 97
  };

  /**
   * lengths
   */
  private static int[] lengths = {
          4, 33, 21, 13, 7, 11, 6, 6
  };

  /**
   * Create a new table.
   */
  public GempakParameterTable() {
  }

    /*
ID# NAME                             UNITS                GNAM         SCALE   MISSING  HZREMAP DIRECTION
    */

  /**
   * Add parameters from the table
   *
   * @param tbl table location
   * @throws IOException problem reading table.
   */
  public void addParameters(String tbl) throws IOException {
    try (InputStream is = getInputStream(tbl)) {
      if (is == null) {
        throw new IOException("Unable to open " + tbl);
      }
      String content = readContents(is);   // LOOK this is silly - should just read one line at a time
      // List           lines   = StringUtil.split(content, "\n", false);
      String[] lines = content.split("\n");
      List<String[]> result = new ArrayList<>();
      for (String line : lines) {
        //String line  = (String) lines.get(i);
        String tline = line.trim();
        if (tline.length() == 0) {
          continue;
        }
        if (tline.startsWith("!")) {
          continue;
        }
        String[] words = new String[indices.length];
        for (int idx = 0; idx < indices.length; idx++) {
          if (indices[idx] >= tline.length()) {
            continue;
          }
          if (indices[idx] + lengths[idx] > tline.length()) {
            words[idx] = line.substring(indices[idx]);
          } else {
            words[idx] = line.substring(indices[idx],
                    indices[idx] + lengths[idx]);
          }
          //if (trimWords) {
          words[idx] = words[idx].trim();
          //}
        }
        result.add(words);
      }
      for (String[] aResult : result) {
        GempakParameter p = makeParameter(aResult);
        if (p != null) {
          if (p.getName().contains("(")) {
            templateParamMap.put(p.getName(), p);
          } else {
            paramMap.put(p.getName(), p);
          }
        }
      }
    }

  }

  /**
   * Make a parameter from the tokens
   *
   * @param words the tokens
   * @return a grid parameter (may be null)
   */
  private GempakParameter makeParameter(String[] words) {
    int num = 0;
    String description;
    if (words[0] != null) {
      num = (int) Double.parseDouble(words[0]);
    }
    if ((words[3] == null) || words[3].equals("")) {  // no param name
      return null;
    }
    String name = words[3];
    if (name.contains("-")) {
      int first = name.indexOf("-");
      int last = name.lastIndexOf("-");
      StringBuilder buf = new StringBuilder(name.substring(0, first));
      buf.append("(");
      for (int i = first; i <= last; i++) {
        buf.append("\\d");
      }
      buf.append(")");
      buf.append(name.substring(last + 1));
      name = buf.toString();
    }

    if ((words[1] == null) || words[1].equals("")) {
      description = words[3];
    } else {
      description = words[1];
    }
    String unit = words[2];
    if (unit != null) {
      unit = unit.replaceAll("\\*\\*", "");
      if (unit.equals("-")) {
        unit = "";
      }
    }
    int decimalScale;
    try {
      decimalScale = Integer.parseInt(words[4].trim());
    } catch (NumberFormatException ne) {
      decimalScale = 0;
    }

    return new GempakParameter(num, name, description, unit,
            decimalScale);
  }

  /**
   * Get the parameter for the given name
   *
   * @param name name of the parameter (eg:, TMPK);
   * @return corresponding parameter or null if not found in table
   */
  public GempakParameter getParameter(String name) {
    GempakParameter param = paramMap.get(name);
    if (param == null) {  // try the regex list
      Set<String> keys = templateParamMap.keySet();
      if (!keys.isEmpty()) {
        for (String key : keys) {
          Pattern p = Pattern.compile(key);
          Matcher m = p.matcher(name);
          if (m.matches()) {
            //System.out.println("found match " + key + " for " + name);
            String value = m.group(1);
            GempakParameter match = templateParamMap.get(key);
            param = new GempakParameter(match.getNumber(), name,
                    match.getDescription() + " (" + value
                            + " hour)", match.getUnit(),
                    match.getDecimalScale());
            paramMap.put(name, param);
            break;
          }
        }
      }
    }
    return param;
  }

  /**
   * Test
   *
   * @param args ignored
   * @throws IOException problem reading the table.
   */
  public static void main(String[] args) throws IOException {
    GempakParameterTable pt = new GempakParameterTable();
    //pt.addParameters("resources/nj22/tables/gempak/wmogrib3.tbl");
    pt.addParameters("resources/nj22/tables/gempak/params.tbl");
    if (args.length > 0) {
      String param = args[0];
      GempakParameter parm = pt.getParameter(param);
      if (parm != null) {
        System.out.println("Found " + param + ": " + parm);
      }
    }
  }


  /**
   * Read in the bytes from the given InputStream
   * and construct and return a String.
   * Closes the InputStream argument.
   *
   * @param is InputStream to read from
   * @return contents as a String
   * @throws IOException problem reading contents
   */
  private String readContents(InputStream is) throws IOException {
    return new String(readBytes(is), CDM.utf8Charset);
  }

  /**
   * Read the bytes in the given input stream.
   *
   * @param is The input stream
   * @return The bytes
   * @throws IOException On badness
   */
  private byte[] readBytes(InputStream is) throws IOException {
    int totalRead = 0;
    byte[] content = new byte[1000000];
    while (true) {
      int howMany = is.read(content, totalRead,
              content.length - totalRead);
      if (howMany < 0) {
        break;
      }
      if (howMany == 0) {
        continue;
      }
      totalRead += howMany;
      if (totalRead >= content.length) {
        byte[] tmp = content;
        int newLength = ((content.length < 25000000)
                ? content.length * 2
                : content.length + 5000000);
        content = new byte[newLength];
        System.arraycopy(tmp, 0, content, 0, totalRead);
      }
    }
    is.close();
    byte[] results = new byte[totalRead];
    System.arraycopy(content, 0, results, 0, totalRead);
    return results;
  }

  /**
   * Get the input stream to the given resource
   *
   * @param resourceName The resource name. May be a file, url,
   *                     java resource, etc.
   * @return The input stream to the resource
   */
  private InputStream getInputStream(String resourceName) throws IOException {

    // Try class loader to get resource
    ClassLoader cl = GempakParameterTable.class.getClassLoader();
    InputStream s = cl.getResourceAsStream(resourceName);
    if (s != null) {
      return s;
    }

    //Try the file system
    File f = new File(resourceName);
    if (f.exists()) {
        s = new FileInputStream(f);
    }
    if (s != null) {
      return s;
    }

    //Try it as a url
    Matcher m = Pattern.compile(" ").matcher(resourceName);
    String encodedUrl = m.replaceAll("%20");
    URL dataUrl = new URL(encodedUrl);
    URLConnection connection = dataUrl.openConnection();
    return connection.getInputStream();
  }


}

