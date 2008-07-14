/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.bufr;

import org.jdom.input.SAXBuilder;
import org.jdom.Element;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import ucar.unidata.util.StringUtil;

/**
 * @author caron
 * @since Jul 12, 2008
 */
public class CodeTable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CodeTable.class);
  static private Map<String, CodeTable> tableMap;

  static public CodeTable getTable(String id) {
    if (tableMap == null) init();
    return tableMap.get(id);
  }

  static public boolean hasTable(String id) {
    if (tableMap == null) init();
    return tableMap.get(id) != null;
  }

  static void init() {
    tableMap = new HashMap<String, CodeTable>(100);
    String filename = "C:/docs/bufr/wmo/Code-FlagTables-11-2007.trans2.xml";

    try {
      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document tdoc = builder.build(filename);
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
        CodeTable ct = new CodeTable(name, desc);
        tableMap.put(ct.id, ct);
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
              log.warn("BAD VALUE "+valueS+" for table "+name);
            }
          }
        }
      }

    } catch (Exception e) {
      log.error("Can't read BUFR code table "+filename, e);
    }
  }

  private String id, name;
  private Map<Integer,String> map;

  private CodeTable(String id, String name) {
    this.id = normal(id);
    this.name = StringUtil.replace( name, ' ', "_");
    map = new HashMap<Integer,String>(20);
  }

  private String normal( String id) {
    String[] tok = id.split(" ");
    if (tok.length != 3)
      log.warn("Illegal table name="+id);

    else try {
      int f = Integer.parseInt(tok[0]);
      int x = Integer.parseInt(tok[1]);
      int y = Integer.parseInt(tok[2]);
      return f+"-"+x+"-"+y;
    } catch (NumberFormatException e) {
      log.warn("Illegal table name="+id);
    }
    return "";
  }

  public String getName() { return name; }
  public Map<Integer,String> getMap() { return map; }

  private void addValue(int value, String text) {
    map.put(value,text);  
  }

}
