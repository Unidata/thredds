/*
* Copyright 1998-2011 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.wmo;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Read and process WMO common code and flag tables.
 *
 * <pre>
  Table C-1 : GRIB-1, BUFR-3
    F1 F2 for alphanumeric codes
    F3 F3 F3 for alphanumeric codes
    Code table 0 in GRIB Edition 1/Code table 0 01 033 for BUFR Edition 3
    Octet 5 in Section1 of GRIB Edition 1/Octet 6 in Section 1 of BUFR Edition 3

  Table C-11 : GRIB-2, BUFR-4
    BUFR 0 01 035
    CREX Edition 2, ooooo in Group Poooooppp in Section 1
    GRIB Editon 2, Octets 6-7 in Section 1
    BUFR Edition 4, Octets 5-6 in Section 1

  Table C-12: Sub-centres of originating centres defined by entries in  C-1 or C-11
     BUFR  0 01 034
     BUFR Edition 3, Octet  5 in Section 1
     BUFR Edition 4, Octets 7-8 in Section 1
     GRIB Edition 1, Octet 26 in Section 1
     GRIB Edition 2, Octets 8-9 in Section 1
     CREX Edition 2, ppp in Group Poooooppp in Section 1

 * </pre>
 * @author caron
 * @since 3/29/11
 */
public class CommonCodeTable implements Comparable<CommonCodeTable> {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CommonCodeTable.class);
  static private final Map<Integer, CommonCodeTable> tableMap = new HashMap<Integer, CommonCodeTable>();


/*
<Exp_CommonTableC01_E>
<No>11</No>
<CodeFigureForF1F2>09</CodeFigureForF1F2>
<CodeFigureForF3F3F3>009</CodeFigureForF3F3F3>
<Octet5GRIB1_Octet6BUFR3>9</Octet5GRIB1_Octet6BUFR3>
<OriginatingGeneratingCentres_E>US National Weather Service - Other</OriginatingGeneratingCentres_E>
<Status>Operational</Status>
</Exp_CommonTableC01_E>

<Exp_CommonTableC02_E>
<No>1</No>
<DateOfAssignment_E>Not applicable</DateOfAssignment_E>
<CodeFigureForrara>00</CodeFigureForrara>
<CodeFigureForBUFR>0</CodeFigureForBUFR>
<RadiosondeSoundingSystemUsed_E>Reserved</RadiosondeSoundingSystemUsed_E>
<Status>Operational</Status>
</Exp_CommonTableC02_E>

<Exp_CommonTableC11_E>
<No>16</No>
<CREX2>00013</CREX2>
<GRIB2_BUFR4>13</GRIB2_BUFR4>
<OriginatingGeneratingCentre_E>)</OriginatingGeneratingCentre_E>
<Status>Operational</Status>
</Exp_CommonTableC11_E>

<Exp_CommonTableC12_E>
<No>8</No>
<CodeFigure_OriginatingCentres>2</CodeFigure_OriginatingCentres>
<Name_OriginatingCentres_E>Melbourne</Name_OriginatingCentres_E>
<CodeFigure_SubCentres>219</CodeFigure_SubCentres>
<Name_SubCentres_E>Townsville</Name_SubCentres_E>
<Status>Operational</Status>
</Exp_CommonTableC12_E>
*/

  public enum Table {
    C01(1, 1, new String[]{"CodeFigureForF1F2", "CodeFigureForF3F3F3", "OriginatingGeneratingCentres_E"}),
    C02(2, 1, new String[]{"CodeFigureForBUFR", null, "RadiosondeSoundingSystemUsed_E"}),
    C11(11,1, new String[]{"GRIB2_BUFR4", null, "OriginatingGeneratingCentre_E"}),
    C12(12,2, new String[]{"CodeFigure_OriginatingCentres", "Name_OriginatingCentres_E", "CodeFigure_SubCentres", "Name_SubCentres_E"}),
    ;

    String[] elems;
    int num, type;

    Table(int num, int type, String[] elems) {
      this.num = num;
      this.type = type;
      this.elems = elems;
    }

    public String getResourceName() {
      return "/resources/wmo/CommonTable_" + this.name() + "_Mar2011_E.xml";
    }

    public String getRootElemName() {
      return "Exp_CommonTable" + this.name() + "_E";
    }

    public int getTableNo() {
      return num;
    }

    public int getTableType() {
      return type;
    }

    String[] getElemNames() {
      return elems;
    }
  }

  static private CommonCodeTable readCommonCodes(Table version) throws IOException {
    InputStream ios = null;
    try {
      Class c = CommonCodeTable.class;
      ios = c.getResourceAsStream(version.getResourceName());
      if (ios == null) {
        throw new IllegalStateException("CommonCodeTable cant open " + version.getResourceName());
      }

      org.jdom.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        doc = builder.build(ios);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }
      Element root = doc.getRootElement();
      String previousValue = null;
      CommonCodeTable ct = new CommonCodeTable(version.name(), version.getTableType());
      String[] elems = version.getElemNames();

      List<Element> featList = root.getChildren(version.getRootElemName());
      for (Element elem : featList) {
        if (version.type == 1) {
          String line = elem.getChildTextNormalize("No");
          String code = elem.getChildTextNormalize(elems[0]);
          String code2 = (elems[1] != null) ? elem.getChildTextNormalize(elems[1]) : null;
          String value = elem.getChildTextNormalize(elems[2]);
          Element statusElem = elem.getChild("Status");
          String status = (statusElem == null) ? null : statusElem.getTextNormalize();
          if (value != null && value.equals(")")) value = previousValue;
          ct.add(line, code, code2, value, status);
          previousValue = value;

        } else {
          String line = elem.getChildTextNormalize("No");
          String code = elem.getChildTextNormalize(elems[0]);
          String value = elem.getChildTextNormalize(elems[1]);
          String code2 = elem.getChildTextNormalize(elems[2]);
          String value2 = elem.getChildTextNormalize(elems[3]);
          Element statusElem = elem.getChild("Status");
          String status = (statusElem == null) ? null : statusElem.getTextNormalize();
          ct.add(line, code, value, code2, value2, status);
        }
      }

      ios.close();
      return ct;

    } finally {
      if (ios != null)
        ios.close();
    }
  }

  public static CommonCodeTable getTable(int tableNo) {
    CommonCodeTable cct = tableMap.get(tableNo);

     if (cct == null) {
       Table want = null;
       for (Table t : Table.values())
         if (t.num == tableNo) {
           want = t;
           break;
         }
       if (want == null)
         throw new IllegalStateException("Unknown wmo common code table number= "+tableNo);

       try {
         cct = readCommonCodes(want);
         tableMap.put(tableNo, cct);
       } catch (IOException e) {
         throw new IllegalStateException("Cant open wmo common code table "+want);
       }
     }

    return cct;
  }

  public static String getTableValue(int tableNo, int code) {
    CommonCodeTable cct = getTable(tableNo);
    if (cct == null) {

    }
    TableEntry te =  cct.get(code);
    if (te == null) return null;
    return te.value;
  }

  public static String getTableValue(int tableNo, int code, int code2) {
    CommonCodeTable cct = getTable(tableNo);
    TableEntry te =  cct.get(code, code2);
    if (te == null) return null;
    return te.value;
  }

  ////////////////////////////////////////////////////////////////////////////////////

  public String tableName;
  public int type;
  public List<TableEntry> entries = new ArrayList<TableEntry>();

  CommonCodeTable(String name, int type) {
    this.tableName = name;
    this.type = type;
  }

  void add(String line, String code, String code2, String value, String status) {
    entries.add(new TableEntry(line, code, code2, value, status));
  }

  void add(String line, String code, String value, String code2, String value2, String status) {
    entries.add(new TableEntry(line, code, value, code2, value2, status));
  }

  // look replace with hash or array
  TableEntry get(int code) {
    for (TableEntry p : entries) {
      if (p.code == code) return p;
    }
    return null;
  }

  TableEntry get(int code, int code2) {
    for (TableEntry p : entries) {
      if ((p.code == code) && (p.code2 == code2))return p;
    }
    return null;
  }

  @Override
  public int compareTo(CommonCodeTable o) {
    return tableName.compareTo(o.tableName);
  }

  String state() {
    Set<Integer> set = new HashSet<Integer>();
    int max = 0;
    int dups = 0;
    for (TableEntry entry : entries) {
      if (entry.comment != null) continue;
      if (entry.code > max) max = entry.code;
      if (set.contains(entry.code)) dups++;
      else set.add(entry.code);
    }
    return "density= "+entries.size() + "/" + max+"; dups= "+dups;
  }

  public class TableEntry implements Comparable<TableEntry> {
    public int line, code, code2;
    public String value, status, comment;

    int parse(String s) {
      if (s == null) return -1;
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e2) {
        return -2;
      }
    }

    TableEntry(String line, String code1, String code2, String value, String status) {
      this.line = Integer.parseInt(line);
      this.code = parse(code1);
      if (this.code < 0)
        this.code = parse(code2);
      if (this.code < 1)
        comment = value;
      else {
        this.value = value;
        this.status = status;
      }
    }

    TableEntry(String line, String code1, String value1, String code2, String value2, String status) {
      this.line = Integer.parseInt(line);
      this.code = parse(code1);
      this.code2 = parse(code2);
      this.value = value2;
      this.comment = value1;
      this.status = status;
    }

    @Override
    public int compareTo(TableEntry o) {
      if (type == 1)
        return code - o.code;
      else {
        int diff = code - o.code;
        return (diff == 0) ?  code2 - o.code2 : diff;
      }
    }

    @Override
    public String toString() {
      if (comment != null)
        return "TableEntry{" +
              ", line=" + line +
              ", comment=" + comment +
                '}';

      else return "TableEntry{" +
              ", line=" + line +
              ", code=" + code +
              ", value='" + value + '\'' +
              ", status='" + status + '\'' +
              '}';
    }
  }

  public static void main(String arg[]) throws IOException {
    CommonCodeTable ct = readCommonCodes(Table.C01);
    for (TableEntry entry : ct.entries)
      System.out.printf("%s%n", entry);
    System.out.printf("%n%s%n", ct.state());
  }
}

