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
package ucar.nc2.iosp.bufr.tables;

import org.jdom.input.SAXBuilder;
import org.jdom.Element;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;

import ucar.unidata.util.StringUtil;

/**
 * @author caron
 * @since Jul 12, 2008
 */
public class CodeFlagTables {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CodeFlagTables.class);
  static Map<Short, CodeFlagTables> tableMap;

  static public CodeFlagTables getTable(short id) {
    if (tableMap == null) init();
    return tableMap.get(id);
  }

  static public boolean hasTable(short id) {
    if (tableMap == null) init();
    CodeFlagTables result = tableMap.get(id);
    return result != null;
  }

  static void init() {
    tableMap = new HashMap<Short, CodeFlagTables>(100);
    String filename = BufrTables.RESOURCE_PATH + "wmo/Code-FlagTables.xml";
    InputStream is = CodeFlagTables.class.getResourceAsStream(filename);

    try {
      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document tdoc = builder.build(is);
      org.jdom.Element root = tdoc.getRootElement();

      for (Element elem : (List<Element>) root.getChildren("table")) {

        String kind = elem.getAttributeValue("kind");
        if ((kind == null) || !kind.equals("code")) {
          continue;
        }

        List<Element> cElems = (List<Element>) elem.getChildren("code");
        if (cElems.size() == 0) {
          continue;
        }

        String name = elem.getAttributeValue("name");
        String desc = elem.getAttributeValue("desc");
        CodeFlagTables ct = new CodeFlagTables(name, desc);
        tableMap.put(ct.fxy, ct);
        // System.out.printf(" added %s == %s %n", ct.id, desc);

        for (Element cElem : cElems) {
          String valueS = cElem.getAttributeValue("value").trim();
          String text = cElem.getText();
          if (text.toLowerCase().startsWith("reserved"))
            continue;
          else if (text.toLowerCase().startsWith("not used"))
            continue;
          else {
            try {
              int value = Integer.parseInt(valueS);
              ct.addValue(value, text);
            } catch (NumberFormatException e) {
              log.warn("NumberFormatException on '"+valueS+"' for CodeTable "+name+" in "+filename);
            }
          }
        }
      }

    } catch (Exception e) {
      log.error("Can't read BUFR code table "+filename, e);
    }
  }

    public static void main(String arg[]) {
      init();
      for (Short key : tableMap.keySet()) {
        CodeFlagTables t = tableMap.get(key);
        System.out.printf("%s %n",t.fxy());
      }
    }

  ////////////////////////////////////////////////
  private short fxy;
  private String name;
  private Map<Integer,String> map;

  private CodeFlagTables(String id, String name) {
    this.fxy = getFxy(id);
    this.name = StringUtil.replace( name, ' ', "_");
    map = new HashMap<Integer,String>(20);
  }

  private short getFxy(String name) {
    try {
      String[] tok = name.split(" ");
      int f = (tok.length > 0) ? Integer.parseInt(tok[0]) : 0;
      int x = (tok.length > 1) ? Integer.parseInt(tok[1]) : 0;
      int y = (tok.length > 2) ? Integer.parseInt(tok[2]) : 0;
      return (short) ((f << 14) + (x << 8) + (y));
    } catch (NumberFormatException e) {
      log.warn("Illegal table name="+name);
      return 0;
    }
  }  

  public String getName() { return name; }
  public Map<Integer,String> getMap() { return map; }

  private void addValue(int value, String text) {
    map.put(value,text);  
  }

  String fxy() {
    int f = fxy >> 16;
    int x = (fxy & 0xff00) >> 8;
    int y = (fxy & 0xff);

    return f +"-"+x+"-"+y;
  }

}
