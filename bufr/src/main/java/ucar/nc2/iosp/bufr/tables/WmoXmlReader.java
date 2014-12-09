/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp.bufr.tables;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.wmo.Util;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Read WMO BUFR XML formats
 *
 * @author John
 * @since 8/10/11
 */
public class WmoXmlReader {
  public enum Version {
    BUFR_14_1_0, BUFR_14_2_0, BUFR_15_1_1, BUFR_16_0_0, BUFR_WMO;

    String[] getElemNamesB() {
      if (this == BUFR_14_1_0) {
        return new String[]{"BC_TableB_BUFR14_1_0_CREX_6_1_0", "ElementName_E"};

      } else if (this == BUFR_14_2_0) {
        return new String[]{"Exporting_BCTableB_E", "ElementName"};

      } else if (this == BUFR_15_1_1) {
        return new String[]{"Exp_JointTableB_E", "ElementName_E"};

      } else if (this == BUFR_16_0_0) {
        return new String[]{"Exp_BUFRCREXTableB_E", "ElementName_E"};

      } else if (this == BUFR_WMO) {   // from now on this is the element name
        return new String[]{null, "ElementName_en"};

      }
      return null;
    }

    String[] getElemNamesD() {
      if (this == BUFR_14_1_0) {
        return new String[]{"B_TableD_BUFR14_1_0_CREX_6_1_0", "ElementName1_E"};

      } else if (this == BUFR_14_2_0) {
        return new String[]{"Exporting_BUFRTableD_E", "ElementName1"};

      } else if (this == BUFR_15_1_1) {
        return new String[]{"Exp_BUFRTableD_E", "ElementName_E"};

      } else if (this == BUFR_16_0_0) {
        return new String[]{"Exp_BUFRTableD_E", "ElementName_E"};

      } else if (this == BUFR_WMO) {
        return new String[]{null, "ElementName_en"};

      }
      return null;
    }
  }

  /*
  14.1
  <BC_TableB_BUFR14_1_0_CREX_6_1_0>
    <SNo>1</SNo>
    <Class>00</Class>
    <FXY>000001</FXY>
    <ElementName_E>Table A: entry</ElementName_E>
    <ElementName_F>Table A : entr?e</ElementName_F>
    <ElementName_R>??????? ?: ???????</ElementName_R>
    <ElementName_S>Tabla A: elemento</ElementName_S>
    <BUFR_Unit>CCITT IA5</BUFR_Unit>
    <BUFR_Scale>0</BUFR_Scale>
    <BUFR_ReferenceValue>0</BUFR_ReferenceValue>
    <BUFR_DataWidth_Bits>24</BUFR_DataWidth_Bits>
    <CREX_Unit>Character</CREX_Unit>
    <CREX_Scale>0</CREX_Scale>
    <CREX_DataWidth>3</CREX_DataWidth>
    <Status>Operational</Status>
    <NotesToTable_E>Notes: (see)#BUFR14_1_0_CREX6_1_0_Notes.doc#BC_Cl000</NotesToTable_E>
</BC_TableB_BUFR14_1_0_CREX_6_1_0>

14.2
<Exporting_BCTableB_E>
  <No>2</No>
  <ClassNo>00</ClassNo>
  <ClassName>BUFR/CREX table entries</ClassName>
  <FXY>000002</FXY>
  <ElementName>Table A: data category description, line 1 </ElementName>
  <BUFR_Unit>CCITT IA5 </BUFR_Unit>
  <BUFR_Scale>0</BUFR_Scale>
  <BUFR_ReferenceValue>0</BUFR_ReferenceValue>
  <BUFR_DataWidth_Bits>256</BUFR_DataWidth_Bits>
  <CREX_Unit>Character</CREX_Unit>
  <CREX_Scale>0</CREX_Scale>
  <CREX_DataWidth>32</CREX_DataWidth>
  <Status>Operational</Status>
</Exporting_BCTableB_E>

15.1
<Exp_JointTableB_E>
  <No>1</No>
  <ClassNo>00</ClassNo>
  <ClassName_E>BUFR/CREX table entries</ClassName_E>
  <FXY>000001</FXY>
  <ElementName_E>Table A: entry</ElementName_E>
  <BUFR_Unit>CCITT IA5</BUFR_Unit>
  <BUFR_Scale>0</BUFR_Scale>
  <BUFR_ReferenceValue>0</BUFR_ReferenceValue>
  <BUFR_DataWidth_Bits>24</BUFR_DataWidth_Bits>
  <CREX_Unit>Character</CREX_Unit>
  <CREX_Scale>0</CREX_Scale>
  <CREX_DataWidth_Char>3</CREX_DataWidth_Char>
  <Status>Operational</Status>
</Exp_JointTableB_E>

16.0
<Exp_BUFRCREXTableB_E>
  <No>681</No>
  <ClassNo>13</ClassNo>
  <ClassName_E>Hydrographic and hydrological elements</ClassName_E>
  <FXY>013060</FXY>
  <ElementName_E>Total accumulated precipitation</ElementName_E>
  <BUFR_Unit>kg m-2</BUFR_Unit>
  <BUFR_Scale>1</BUFR_Scale>
  <BUFR_ReferenceValue>-1</BUFR_ReferenceValue>
  <BUFR_DataWidth_Bits>17</BUFR_DataWidth_Bits>
  <CREX_Unit>kg m-2</CREX_Unit>
  <CREX_Scale>1</CREX_Scale>
  <CREX_DataWidth_Char>5</CREX_DataWidth_Char>
  <Status>Operational</Status>
</Exp_BUFRCREXTableB_E>

<BUFRCREX_17_0_0_TableB_en>
<No>8</No>
<ClassNo>00</ClassNo>
<ClassName_en>BUFR/CREX table entries</ClassName_en>
<FXY>000008</FXY>
<ElementName_en>BUFR Local table version number</ElementName_en>
<Note_en>(see Note 4)</Note_en>
<BUFR_Unit>CCITT IA5</BUFR_Unit>
<BUFR_Scale>0</BUFR_Scale>
<BUFR_ReferenceValue>0</BUFR_ReferenceValue>
<BUFR_DataWidth_Bits>16</BUFR_DataWidth_Bits>
<CREX_Unit>Character</CREX_Unit>
<CREX_Scale>0</CREX_Scale>
<CREX_DataWidth_Char>2</CREX_DataWidth_Char>
<Status>Operational</Status>
</BUFRCREX_17_0_0_TableB_en>

<BUFRCREX_22_0_1_TableB_en>
<No>1018</No>
<ClassNo>21</ClassNo>
<ClassName_en>BUFR/CREX Radar data</ClassName_en>
<FXY>021073</FXY>
<ElementName_en>Satellite altimeter instrument mode</ElementName_en>
<BUFR_Unit>Flag table</BUFR_Unit>
<BUFR_Scale>0</BUFR_Scale>
<BUFR_ReferenceValue>0</BUFR_ReferenceValue>
<BUFR_DataWidth_Bits>9</BUFR_DataWidth_Bits>
<CREX_Unit>Flag table</CREX_Unit>
<CREX_Scale>0</CREX_Scale>
<CREX_DataWidth_Char>3</CREX_DataWidth_Char>
<Status>Operational</Status>
</BUFRCREX_22_0_1_TableB_en>
   */

  static void readWmoXmlTableB(InputStream ios, TableB b) throws IOException {
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(ios);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    Element root = doc.getRootElement();

    String[] elems = null;
    for (Version v : Version.values()) {
      elems = v.getElemNamesB();
      List<Element> featList = root.getChildren(elems[0]);
      if (featList != null && featList.size() > 0) {
        break;
      }
    }

    // if not found using element name, assume its BUFR_WMO
    if (elems == null) {
      elems = Version.BUFR_WMO.getElemNamesB();
    }

    List<Element> featList = root.getChildren();
    for (Element elem : featList) {
      Element ce = elem.getChild(elems[1]);
      if (ce == null) continue;

      String name = Util.cleanName(elem.getChildTextNormalize(elems[1]));
      String units = cleanUnit(elem.getChildTextNormalize("BUFR_Unit"));
      int x = 0, y = 0, scale = 0, reference = 0, width = 0;

      String fxy = null;
      String s = null;
      try {
        fxy = elem.getChildTextNormalize("FXY");
        int xy = Integer.parseInt(cleanNumber(fxy));
        x = xy / 1000;
        y = xy % 1000;

      } catch (NumberFormatException e) {
        System.out.printf(" key %s name '%s' fails parsing %n", fxy, name);
      }

      try {
        s = elem.getChildTextNormalize("BUFR_Scale");
        scale = Integer.parseInt(cleanNumber(s));
      } catch (NumberFormatException e) {
        System.out.printf(" key %s name '%s' has bad scale='%s'%n", fxy, name, s);
      }

      try {
        s = elem.getChildTextNormalize("BUFR_ReferenceValue");
        reference = Integer.parseInt(cleanNumber(s));
      } catch (NumberFormatException e) {
        System.out.printf(" key %s name '%s' has bad reference='%s' %n", fxy, name, s);
      }

      try {
        s = elem.getChildTextNormalize("BUFR_DataWidth_Bits");
        width = Integer.parseInt(cleanNumber(s));
      } catch (NumberFormatException e) {
        System.out.printf(" key %s name '%s' has bad width='%s' %n", fxy, name, s);
      }

      b.addDescriptor((short) x, (short) y, scale, reference, width, name, units, null);
    }
    ios.close();
  }

  static String cleanNumber(String s) {
    return StringUtil2.remove(s, ' ');
  }

  public static String cleanUnit(String unit) {
    String result = StringUtil2.remove(unit, 176);
    return StringUtil2.replace(result, (char) 65533, "2"); // seems to be a superscript 2 in some language
  }

  /*
  <B_TableD_BUFR14_1_0_CREX_6_1_0>
    <SNo>2647</SNo>
    <Category>10</Category>
    <FXY1>310013</FXY1>
    <ElementName1_E>(AVHRR (GAC) report)</ElementName1_E>
    <FXY2>004005</FXY2>
    <ElementName2_E>Minute</ElementName2_E>
    <Remarks_E>Minute</Remarks_E>
    <Status>Operational</Status>
  </B_TableD_BUFR14_1_0_CREX_6_1_0>

14.2.0
  <Exporting_BUFRTableD_E>
    <No>2901</No>
    <Category>10</Category>
    <CategoryOfSequences>Vertical sounding sequences (satellite data)</CategoryOfSequences>
    <FXY1>310025</FXY1>
    <ElementName1>(SSMIS Temperature data record)</ElementName1>
    <FXY2>004006</FXY2>
    <Status>Operational</Status>
  </Exporting_BUFRTableD_E>

  15.1.1
  <Exp_BUFRTableD_E>
    <No>102</No>
    <Category>01</Category>
    <CategoryOfSequences_E>Location and identification sequences</CategoryOfSequences_E>
    <FXY1>301034</FXY1>
    <Title_E>(Buoy/platform - fixed)</Title_E>
    <FXY2>001005</FXY2>
    <ElementName_E>Buoy/platform identifier</ElementName_E>
    <ExistingElementName_E>Buoy/platform identifier</ExistingElementName_E>
    <Status>Operational</Status>
  </Exp_BUFRTableD_E>

  16.0.0
  <Exp_BUFRTableD_E>
    <No>402</No>
    <Category>02</Category>
    <CategoryOfSequences_E>Meteorological sequences common to surface data</CategoryOfSequences_E>
    <FXY1>302001</FXY1>
    <FXY2>010051</FXY2>
    <ElementName_E>Pressure reduced to mean sea level</ElementName_E>
    <ExistingElementName_E>Pressure reduced to mean sea level</ExistingElementName_E>
    <Status>Operational</Status>
  </Exp_BUFRTableD_E>

  <BUFR_19_1_1_TableD_en>
  <No>4</No>
  <Category>00</Category>
  <CategoryOfSequences_en>BUFR table entries sequences</CategoryOfSequences_en>
  <FXY1>300003</FXY1>
  <Title_en>(F, X, Y of descriptor to be added or defined)</Title_en>
  <FXY2>000011</FXY2>
  <ElementName_en>X descriptor to be added or defined</ElementName_en>
  <Status>Operational</Status>
  </BUFR_19_1_1_TableD_en>

  <BUFR_22_0_1_TableD_en>
  <No>5874</No>
  <Category>15</Category>
  <CategoryOfSequences_en>Oceanographic report sequences</CategoryOfSequences_en>
  <FXY1>315004</FXY1>
  <Title_en>(XBT temperature profile data sequence)</Title_en>
  <FXY2>025061</FXY2>
  <ElementName_en>Software identification and version number</ElementName_en>
  <Status>Operational</Status>
  </BUFR_22_0_1_TableD_en>

   */
  static void readWmoXmlTableD(InputStream ios, TableD tableD) throws IOException {
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(ios);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    int currSeqno = -1;
    TableD.Descriptor currDesc = null;

    Element root = doc.getRootElement();

    String[] elems = null;
    for (Version v : Version.values()) {
      elems = v.getElemNamesD();
      List<Element> featList = root.getChildren(elems[0]);
      if (featList != null && featList.size() > 0) {
        break;
      }
    }

    if (elems == null) {
      elems = Version.BUFR_WMO.getElemNamesD();
    }

    List<Element> featList = root.getChildren();
    for (Element elem : featList) {
      Element ce = elem.getChild(elems[1]);
      if (ce == null) continue;

      String seqs = elem.getChildTextNormalize("FXY1");
      int seq = Integer.parseInt(seqs);

      if (currSeqno != seq) {
        int y = seq % 1000;
        int w = seq / 1000;
        int x = w % 100;
        String seqName = Util.cleanName(elem.getChildTextNormalize(elems[1]));
        currDesc = tableD.addDescriptor((short) x, (short) y, seqName, new ArrayList<Short>());
        currSeqno = seq;
      }

      String fnos = elem.getChildTextNormalize("FXY2");
      int fno = Integer.parseInt(fnos);
      int y = fno % 1000;
      int w = fno / 1000;
      int x = w % 100;
      int f = w / 100;
      int fxy = (f << 14) + (x << 8) + y;
      currDesc.addFeature((short) fxy);
    }
    ios.close();
  }

}

