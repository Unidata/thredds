/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.util;

import java.io.*;
import java.util.*;
import java.net.URL;

/**
 * Utility class to read and parse a fixed length table.
 * Each line of the table becomes a "Record". Each Record has a set of Fields described by the format string.
 *
 <pre>
    List<TableParser.Record> recs = TableParser.readTable(is, "3,15,46,54,60d,67d,73d", 50000);
    for (TableParser.Record record : recs) {
      Station s = new Station();
      s.id = "K" + record.get(0);
      s.name = record.get(2) + " " + record.get(3);
      s.lat = (Double) record.get(4) * .01;
      s.lon = (Double) record.get(5) * .01;
      s.elev = (Double) record.get(6);

      stationTableHash.put(s.id, s);
      if (showStations) System.out.println(" station= " + s);
    }

Example Table:
TLX      000001 OKLAHOMA_CITY/Norman             OK US  3532  -9727   370  0 NWS
AMA      000313 AMARILLO/Amarillo                TX US  3523 -10170  1093  0 NWS
HGX      000378 HOUSTON/GALVESTON/Dickinson      TX US  2947  -9507     5  0 NWS
MLB      000302 MELBOURNE/Melbourne              FL US  2810  -8065    11  0 NWS

 format:
 "3,15,54,60d,67d,73d"

 grammer:
  format = {field,}
  field = endPos type
  endPos = ending pos in the line
  type = i=integer, d=double else String
  field[0] goes from 0 to endPos[0]
  field[i] goes from endPos[i-1]+1 to endPos[i]

</pre>
 *
 * @author caron
 */
public class TableParser {

  /**
   * Reads a URL or file in as a table.
   *
   * @param urlString starts with http, read URL contenets, else read file.
   * @see #readTable(InputStream ios, String format, int maxLines)
   * @param format describe format of each line.
   * @param maxLines maximum number of lines to parse, set to < 0 to read all
   * @return List of TableParser.Record
   * @throws IOException on read error
   * @throws NumberFormatException  on parse number error
   */
  static public List<Record> readTable(String urlString, String format, int maxLines) throws IOException, NumberFormatException {

    InputStream ios;
    if (urlString.startsWith("http:")) {
      URL url = new URL(urlString);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(urlString);
     }
    
    return readTable( ios, format, maxLines);
  }

  /**
   * Reads an input stream, containing lines of ascii in fixed width format.
   * Breaks each line into a set of Fields (space or comma delimited) which may be String, integer or double.
   *
   * @param ios the input stream
   * @param format describe format of each line.
   * @param maxLines maximum number of lines to parse, set to < 0 to read all
   * @return List of TableParser.Record
   * @throws IOException on read error
   * @throws NumberFormatException  on parse number error
   */
  static public List<Record> readTable(InputStream ios, String format, int maxLines) throws IOException, NumberFormatException {
    List<Field> fields = new ArrayList<Field>();

    int start = 0;
    StringTokenizer stoker = new StringTokenizer( format, " ,");
    while (stoker.hasMoreTokens()) {
      String tok = stoker.nextToken();
      // see what type
      Class type = String.class;
      char last = tok.charAt(tok.length()-1);
      if (last == 'i') type = int.class;
      if (last == 'd') type = double.class;
      if (type != String.class) tok = tok.substring(0, tok.length()-1);

      int end = Integer.parseInt( tok);
      fields.add( new Field( start, end, type));
      start = end+1;
    }

    List<Record> records = new ArrayList<Record>();

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      String line = dataIS.readLine();
      if (line == null) break;
      records.add(new Record( line, fields));
      count++;
    }

    dataIS.close();
    return records;
  }

  /**
   * Describes one field in the record.
   */
  static private class Field {
    int start, end;
    Class type;

    Field (int start, int end, Class type) {
      this.start = start;
      this.end = end;
      this.type = type;
    }

    Object parse( String line) throws NumberFormatException {
      String svalue = line.substring(start, end).trim();

      if (type == String.class)
        return svalue;

      try {
        if (type == double.class)
          return new Double( svalue);
        if (type == int.class)
          return new Integer( svalue);
      } catch (NumberFormatException e) {
        e.printStackTrace();

      }
      return null;
    }

  }

  /**
   * A set of values for one line.
   */
  static public class Record {
    private List<Object> values = new ArrayList<Object>();

    Record( String line, List fields) {
      for (Object field : fields) {
        Field f = (Field) field;
        values.add(f.parse(line));
      }
    }

    /**
     * Get the kth value of this record. Will be a String, Double, or Integer. 
     * @param k which one
     * @return object
     */
    public Object get(int k) { return values.get(k); }
  }

  ////////////////////////////////////////////////////////////////////////////////////
  static String testName = "C:/data/station/adde/STNDB.TXT";
  //static String testName = "M:/temp/STNDB.TXT";
  static String testName2 = "http://localhost:8080/test/STNDB.TXT";
  static String testName3 = "C:/dev/thredds/cdm/src/main/resources/resources/nj22/tables/nexrad.tbl";

  static public void main( String[] args) throws IOException {
    List recs = TableParser.readTable(testName3, "3,15,54,60d,67d,73d", 50000);
    for (int i = 0; i < recs.size(); i++) {
      Record record = (Record) recs.get(i);
      for (int j = 0; j < record.values.size(); j++) {
        Object s = record.values.get(j);
        System.out.print(" "+s.toString());
      }
      System.out.println();
    }
  }
}
