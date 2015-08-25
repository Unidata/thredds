/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util;

import ucar.nc2.constants.CDM;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.util.*;
import java.net.URL;

/**
 * Utility class to read and parse a fixed length table.
 * Each line of the table becomes a "Record". Each Record has a set of Fields described by the format string.
 * <p/>
 * <pre>
 * List<TableParser.Record> recs = TableParser.readTable(is, "3,15,46,54,60d,67d,73d", 50000);
 * for (TableParser.Record record : recs) {
 * Station s = new Station();
 * s.id = "K" + record.get(0);
 * s.name = record.get(2) + " " + record.get(3);
 * s.lat = (Double) record.get(4) * .01;
 * s.lon = (Double) record.get(5) * .01;
 * s.elev = (Double) record.get(6);
 * <p/>
 * stationTableHash.put(s.id, s);
 * if (showStations) System.out.println(" station= " + s);
 * }
 * <p/>
 * Example Table:
 * TLX      000001 OKLAHOMA_CITY/Norman             OK US  3532  -9727   370  0 NWS
 * AMA      000313 AMARILLO/Amarillo                TX US  3523 -10170  1093  0 NWS
 * HGX      000378 HOUSTON/GALVESTON/Dickinson      TX US  2947  -9507     5  0 NWS
 * MLB      000302 MELBOURNE/Melbourne              FL US  2810  -8065    11  0 NWS
 * <p/>
 * format:
 * "3,15,54,60d,67d,73d"
 * <p/>
 * grammer:
 * format = {field,}
 * field = endPos type
 * endPos = ending pos in the line, 0 based, exclusive, ie [start, end)
 * type = i=integer, d=double, L=long else String
 * field[0] goes from [0, endPos[0])
 * field[i] goes from [endPos[i-1] to endPos[i])
 * <p/>
 * </pre>
 *
 * @author caron
 */
/*
    ClassLoader cl = Level2VolumeScan.class.getClassLoader();
    InputStream is = cl.getResourceAsStream("resources/nj22/tables/nexrad.tbl");

    List<TableParser.Record> recs = TableParser.readTable(is, "3,15,46, 54,60d,67d,73d", 50000);
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

          1         2         3         4         5         6         7         8         9         10        11        12
0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

 */
public class TableParser {
  static private final boolean debug = false;

  /**
   * Reads a URL or file in as a table.
   *
   * @param urlString starts with http, read URL contenets, else read file.
   * @param format    describe format of each line.
   * @param maxLines  maximum number of lines to parse, set to < 0 to read all
   * @return List of TableParser.Record
   * @throws IOException           on read error
   * @throws NumberFormatException on parse number error
   * @see #readTable(InputStream ios, String format, int maxLines)
   */
  static public List<Record> readTable(String urlString, String format, int maxLines) throws IOException, NumberFormatException {

    InputStream ios;
    if (urlString.startsWith("http:")) {
      URL url = new URL(urlString);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(urlString);
    }

    return readTable(ios, format, maxLines);
  }

  /**
   * Reads an input stream, containing lines of ascii in fixed width format.
   * Breaks each line into a set of Fields (space or comma delimited) which may be String, integer or double.
   *
   * @param ios      the input stream, will be closed
   * @param format   describe format of each line.
   * @param maxLines maximum number of lines to parse, set to < 0 to read all
   * @return List of TableParser.Record
   * @throws IOException           on read error
   * @throws NumberFormatException on parse number error
   */
  static public List<Record> readTable(InputStream ios, String format, int maxLines) throws IOException, NumberFormatException {
    List<Record> result;
    try {
      TableParser parser = new TableParser(format);
      result = parser.readAllRecords(ios, maxLines);
    } finally {
      ios.close();
    }
    return result;
  }


  /////////////////////////////////////////////////////////////////////////////////////////////////

  private List<Field> fields = new ArrayList<>();
  public TableParser(String format) throws IOException, NumberFormatException {
    int start = 0;
    StringTokenizer stoker = new StringTokenizer(format, " ,");
    while (stoker.hasMoreTokens()) {
      String tok = stoker.nextToken();
      // see what type
      Class type = String.class;
      char last = tok.charAt(tok.length() - 1);
      if (last == 'i') type = int.class;
      if (last == 'd') type = double.class;
      if (last == 'L') type = long.class;
      if (type != String.class) tok = tok.substring(0, tok.length() - 1);

      int end = Integer.parseInt(tok);
      fields.add(new Field(start, end, type));
      start = end;
    }
  }

  private String comment = "#";
  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<Record> readAllRecords(InputStream ios, int maxLines) throws IOException, NumberFormatException {

    List<Record> records = new ArrayList<>();

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, CDM.utf8Charset));
    int count = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith(comment)) continue;
      if (line.trim().length() == 0) continue;
      if (debug) System.out.printf("%s%n", line);
      Record r = Record.make(line, fields);
      if (r != null)
        records.add(r);
      count++;
    }

    return records;
  }

  public Field getField(int fldno) {
    return fields.get(fldno);
  }

  public int getNumberOfFields() {
    return fields.size();
  }


  /**
   * Describes one field in the record.
   */
  public static class Field {
    int start, end;
    Class type;

    boolean hasScale = false;
    float scale;

    Field(int start, int end, Class type) {
      this.start = start;
      this.end = end;
      this.type = type;
    }

    protected Field() {}

    public Object parse(String line) throws NumberFormatException {
      return parse(line, this.start, this.end);
    }

    public Object parse(String line, int offset) throws NumberFormatException {
      return parse(line, this.start+offset, this.end+offset);      
    }

    protected Object parse(String line, int start, int end) throws NumberFormatException {
      String svalue;
      if (start >= line.length())
        svalue = "";
      else if (end >= line.length())
        svalue = line.substring(start);
      else
        svalue = line.substring(start, end);
      //System.out.printf("  [%d,%d) = %s %n",start, end, svalue);

      if (type == String.class)
        return svalue;

      try {
        svalue = StringUtil2.remove(svalue, ' ');
        boolean isBlank = (svalue.trim().length() == 0);
        if (type == double.class)
          return isBlank ? 0.0 : new Double(svalue);
        if (type == int.class) {
          Integer result = isBlank ? 0 : new Integer(svalue);
          if (hasScale)
            return result * scale;
          else
            return result;
        }
        if (type == long.class)
          return isBlank ? 0L : new Long(svalue);

      } catch (NumberFormatException e) {
        System.out.printf("  [%d,%d) = <%s> %n", start, end, svalue);
        throw e;
      }

      return null;
    }

    public void setScale(float scale) {
      this.scale = scale;
      hasScale = true;
    }

  }

  public DerivedField addDerivedField(Field from, Transform transform, Class type ) {
    DerivedField fld =  new DerivedField(from, transform, type);
    fields.add(fld);
    return fld;
  }

  public static class DerivedField extends Field {
    Field from;
    Transform transform;

    DerivedField(Field from, Transform transform, Class type) {
      this.from = from;
      this.transform = transform;
      this.type = type;
    }

    protected Object parse(String line, int start, int end) throws NumberFormatException {
      Object org = from.parse(line);
      return transform.derive(org);
    }
  }

  public interface Transform {
    Object derive(Object org);
  }

  /**
   * A set of values for one line.
   */
  static public class Record {
    List<Object> values = new ArrayList<>();

    static Record make(String line, List fields) {
      try {
        Record r = new Record();
        for (Object field : fields) {
          Field f = (Field) field;
          r.values.add(f.parse(line));
        }
        return r;
      } catch (NumberFormatException e) {
        System.out.printf("Bad line=%s %n", line);
        return null;
      }
    }

    public int nfields() {
      return values.size();
    }

    /**
     * Get the kth value of this record. Will be a String, Double, or Integer.
     *
     * @param k which one
     * @return object
     */
    public Object get(int k) {
      return values.get(k);
    }

    public void toString(Formatter f) {
      for (Object s : values)
        f.format(" %s,", s.toString());
      f.format("%n");
    }
  }

}
