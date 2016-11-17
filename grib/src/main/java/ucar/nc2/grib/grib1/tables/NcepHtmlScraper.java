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

package ucar.nc2.grib.grib1.tables;

import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribLevelType;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Read  NCEP html files to extract the GRIB tables.
 *
 * @author caron
 * @since 11/21/11
 */
public class NcepHtmlScraper {
  static private final boolean debug = false;
  static private final boolean show = false;

  String dirOut;

  public void setDirOut(String dirOut) {
    this.dirOut = dirOut;
  }

 //////////////////////////////////////////////////////////////////
  // http://www.nco.ncep.noaa.gov/pmb/docs/on388/tablea.html
  // LOOK the table is hand edited to add the units (!)
  void parseTable3() throws IOException {
    String url = "http://www.nco.ncep.noaa.gov/pmb/docs/on388/table3.html";
    Document doc = Jsoup.parse(new URL(url), 5 * 1000); // 5 sec timeout
    System.out.printf("%s%n", doc);

    Set<String> abbrevSet = new HashSet<>();
    Element table = doc.select("table").get(2);
    List<GribLevelType> stuff = new ArrayList<>();
    Elements rows = table.select("tr");
    for (Element row : rows) {
      Elements cols = row.select("td");
      if (debug) {
        System.out.printf(" %d=", cols.size());
        for (Element col : cols)
          System.out.printf("%s:", col.text());
        System.out.printf("%n");
      }

      if (cols.size() == 3) {
        String snum = StringUtil2.cleanup(cols.get(0).text()).trim();
        try {
          int pnum = Integer.parseInt(snum);
          String desc = StringUtil2.cleanup(cols.get(1).text()).trim();
          String abbrev = StringUtil2.cleanup(cols.get(2).text()).trim();
          if (desc.startsWith("Reserved")) {
            System.out.printf("*** Skip Reserved %s%n", row.text());
            continue;
          } else {
            System.out.printf("%d == %s, %s%n", pnum, desc, abbrev);
            if (abbrevSet.contains(abbrev))
              System.out.printf("DUPLICATE ABBREV %s%n", abbrev);
            else
            stuff.add(new GribLevelType(pnum, desc, abbrev, null, null, false, false));
          }
          //result.add(new Param(pnum, desc, cols.get(2).text(), cols.get(3).text()));
        } catch (NumberFormatException e) {
          System.out.printf("*** Cant parse %s == %s%n", snum, row.text());
        }

      }
    }
    writeTable3Xml("NCEP GRIB-1 Table 3", url, "ncepTable3.xml", stuff);
  }

  private void writeTable3Xml(String name, String source, String filename, List<GribLevelType> stuff) throws IOException {
     org.jdom2.Element rootElem = new org.jdom2.Element("table3");
     org.jdom2.Document doc = new org.jdom2.Document(rootElem);
     rootElem.addContent(new org.jdom2.Element("title").setText(name));
     rootElem.addContent(new org.jdom2.Element("source").setText(source));

     for (GribLevelType p : stuff) {
       org.jdom2.Element paramElem = new org.jdom2.Element("parameter");
       paramElem.setAttribute("code", Integer.toString(p.getCode()));
       paramElem.addContent(new org.jdom2.Element("description").setText(p.getDesc()));
       paramElem.addContent(new org.jdom2.Element("abbrev").setText(p.getAbbrev()));
       String units = p.getUnits();
       if (units == null) units = handcodedUnits(p.getCode());
       if (units != null) paramElem.addContent(new org.jdom2.Element("units").setText(units));
       if (p.getDatum() != null) paramElem.addContent(new org.jdom2.Element("datum").setText(p.getDatum()));
       if (p.isPositiveUp()) paramElem.addContent(new org.jdom2.Element("isPositiveUp").setText("true"));
       if (p.isLayer()) paramElem.addContent(new org.jdom2.Element("isLayer").setText("true"));
       rootElem.addContent(paramElem);
     }

     XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
     String x = fmt.outputString(doc);

     try (FileOutputStream fout = new FileOutputStream(dirOut + filename)) {
       fout.write(x.getBytes(CDM.utf8Charset));
     }

     if (show) System.out.printf("%s%n", x);
   }

  private String handcodedUnits(int code) {
    switch (code) {
      case 216 :
      case 217 :
      case 237:
      case 238:
        return "m";
      case 235 :
        return ".1 degC";
    }
    return null;
  }

   /*  Grib1LevelTypeTable will go away soon
   void writeWmoTable3() throws IOException {

     Set<String> abbrevSet = new HashSet<String>();
     List<Grib1Tables.LevelType> stuff = new ArrayList<Grib1Tables.LevelType>();
     for (int code = 1; code < 255; code++) {
       String desc = Grib1LevelTypeTable.getLevelDescription(code);
       if (desc.startsWith("Unknown")) continue;
       String abbrev = Grib1LevelTypeTable.getNameShort(code);
       String units = Grib1LevelTypeTable.getUnits(code);
       String datum = Grib1LevelTypeTable.getDatum(code);

       if (abbrevSet.contains(abbrev))
         System.out.printf("DUPLICATE ABBREV %s%n", abbrev);

       Grib1Tables.LevelType level = new Grib1Tables.LevelType(code, desc, abbrev, units, datum);
       level.isLayer = Grib1LevelTypeTable.isLayer(code);
       level.isPositiveUp = Grib1LevelTypeTable.isPositiveUp(code);
       stuff.add(level);
     }

     writeTable3Xml("WMO GRIB-1 Table 3", "Unidata transcribe WMO306_Vol_I.2_2010_en.pdf", "wmoTable3.xml", stuff);
   }  */

  //////////////////////////////////////////////////////////////////
  // http://www.nco.ncep.noaa.gov/pmb/docs/on388/tablea.html

  void parseTableA() throws IOException {
    String source = "http://www.nco.ncep.noaa.gov/pmb/docs/on388/tablea.html";
    String base = "http://www.nco.ncep.noaa.gov/pmb/docs/on388/";
//    File input = new File("C:\\dev\\github\\thredds\\grib\\src\\main\\sources\\ncep\\ON388.TableA.htm");
    //Document doc = Jsoup.parse(input, "UTF-8", base);
    //System.out.printf("%s%n", doc);
    Document doc = Jsoup.parse(new URL(source), 10 * 1000);

    Element table = doc.select("table").first();
    if (table == null) return;
    List<Stuff> stuff = new ArrayList<>();
    Elements rows = table.select("tr");
    for (Element row : rows) {
      Elements cols = row.select("td");
      if (debug) {
        System.out.printf(" %d=", cols.size());
        for (Element col : cols)
          System.out.printf("%s:", col.text());
        System.out.printf("%n");
      }

      if (cols.size() == 2) {
        String snum = StringUtil2.cleanup(cols.get(0).text()).trim();
        try {
          int pnum = Integer.parseInt(snum);
          String desc = StringUtil2.cleanup(cols.get(1).text()).trim();
          if (desc.startsWith("Reserved")) {
            System.out.printf("*** Skip Reserved %s%n", row.text());
          } else {
            System.out.printf("%d == %s%n", pnum, desc);
            stuff.add(new Stuff(pnum, desc));
          }
          //result.add(new Param(pnum, desc, cols.get(2).text(), cols.get(3).text()));
        } catch (NumberFormatException e) {
          System.out.printf("*** Cant parse %s == %s%n", snum, row.text());
        }

      }
    }
    writeTableAXml("NCEP GRIB-1 Table A", source, "ncepTableA.xml", stuff);
  }

  private static class Stuff {
    int no;
    String desc;

    private Stuff(int no, String desc) {
      this.no = no;
      this.desc = desc;
    }
  }

  private void writeTableAXml(String name, String source, String filename, List<Stuff> stuff) throws IOException {
    org.jdom2.Element rootElem = new org.jdom2.Element("tableA");
    org.jdom2.Document doc = new org.jdom2.Document(rootElem);
    rootElem.addContent(new org.jdom2.Element("title").setText(name));
    rootElem.addContent(new org.jdom2.Element("source").setText(source));

    for (Stuff p : stuff) {
      org.jdom2.Element paramElem = new org.jdom2.Element("parameter");
      paramElem.setAttribute("code", Integer.toString(p.no));
      paramElem.addContent(new org.jdom2.Element("description").setText(p.desc));
      rootElem.addContent(paramElem);
    }

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    String x = fmt.outputString(doc);

    try (FileOutputStream fout = new FileOutputStream(dirOut + filename)) {
      fout.write(x.getBytes(CDM.utf8Charset));
    }

    if (show) System.out.printf("%s%n", x);
  }



  //////////////////////////////////////////////////////////////////
  private int[] tableVersions = new int[]{2, 0, 128, 129, 130, 131, 133, 140, 0, 0, 141};

  void parseTable2() throws IOException {
    String source = "http://www.nco.ncep.noaa.gov/pmb/docs/on388/table2.html";
    //File input = new File("C:\\dev\\github\\thredds\\grib\\src\\main\\sources\\ncep\\on388.2011-11-18.htm");
    //Document doc = Jsoup.parse(input, "UTF-8", "http://www.nco.ncep.noaa.gov/pmb/docs/on388/");
    //System.out.printf("%s%n", doc);
    Document doc = Jsoup.parse(new URL(source), 10*1000);

    int count = 0;
    for (Element e : doc.select("big"))
      System.out.printf("%d == %s%n=%n", count++, e.text());

    Element body = doc.select("body").first();
    if (body == null) return;

    Elements tables = body.select("table");
    for (int i = 0; i < tableVersions.length; i++) {
      if (tableVersions[i] == 0) continue;
      Element table = tables.select("table").get(i);
      List<Param> params = readTable2(table);

      String name = "NCEP Table Version " + tableVersions[i];
      String filename = "ncepGrib1-" + tableVersions[i];
      writeTable2Wgrib(name, source, filename + ".tab", params);
      writeTable2Xml(name, source, filename + ".xml", params);
    }
  }

  private List<Param> readTable2(Element table) {
    List<Param> result = new ArrayList<>();
    Elements rows = table.select("tr");
    for (Element row : rows) {
      Elements cols = row.select("td");
      if (debug) {
        System.out.printf(" %d=", cols.size());
        for (Element col : cols)
          System.out.printf("%s:", col.text());
        System.out.printf("%n");
      }

      if (cols.size() == 4) {
        String snum = StringUtil2.cleanup(cols.get(0).text()).trim();
        try {
          int pnum = Integer.parseInt(snum);
          String desc = StringUtil2.cleanup(cols.get(1).text()).trim();
          if (desc.startsWith("Reserved")) {
            System.out.printf("*** Skip Reserved %s%n", row.text());
            continue;
          }
          result.add(new Param(pnum, desc, cols.get(2).text(), cols.get(3).text()));
        } catch (NumberFormatException e) {
          System.out.printf("*** Cant parse %s == %s%n", snum, row.text());
        }

      }

    }
    return result;
  }

  private static class Param {
    int pnum;
    String desc, unit, name;

    private Param(int pnum, String desc, String unit, String name) {
      this.pnum = pnum;
      this.desc = desc;
      this.unit = StringUtil2.cleanup(unit);
      this.name = StringUtil2.cleanup(name);
    }
  }

  private void writeTable2Xml(String name, String source, String filename, List<Param> params) throws IOException {
    org.jdom2.Element rootElem = new org.jdom2.Element("parameterMap");
    org.jdom2.Document doc = new org.jdom2.Document(rootElem);
    rootElem.addContent(new org.jdom2.Element("title").setText(name));
    rootElem.addContent(new org.jdom2.Element("source").setText(source));

    for (Param p : params) {
      org.jdom2.Element paramElem = new org.jdom2.Element("parameter");
      paramElem.setAttribute("code", Integer.toString(p.pnum));
      paramElem.addContent(new org.jdom2.Element("shortName").setText(p.name));
      paramElem.addContent(new org.jdom2.Element("description").setText(p.desc));
      paramElem.addContent(new org.jdom2.Element("units").setText(p.unit));
      rootElem.addContent(paramElem);
    }

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    String x = fmt.outputString(doc);

    try (FileOutputStream fout = new FileOutputStream(dirOut + filename)) {
      fout.write(x.getBytes(CDM.utf8Charset));
    }

    if (show) System.out.printf("%s%n", x);
  }

  private void writeTable2Wgrib(String name, String source, String filename, List<Param> params) throws IOException {
    Formatter f = new Formatter();
    f.format("# %s%n", name);
    f.format("# %s%n", source);
    for (Param p : params)
      f.format("%3d:%s:%s [%s]%n", p.pnum, p.name, p.desc, p.unit); // 1:PRES:Pressure [Pa]

    try (FileOutputStream fout = new FileOutputStream(dirOut + filename)) {
      fout.write(f.toString().getBytes(CDM.utf8Charset));
    }

    if (show) System.out.printf("%s%n", f);
  }

  public static void main(String[] args) throws IOException {
    NcepHtmlScraper scraper = new NcepHtmlScraper();
    /////////////////////////////////////////////////////////
    // dirOut - temp directory to hold new grib1 resources
    // String dirOut = "C:\\dev\\github\\thredds\\grib\\src\\main\\resources\\resources\\grib1\\ncep\\";
    scraper.setDirOut("/Users/sarms/Desktop/ncep/grib1/");
    scraper.parseTable2();
    scraper.parseTableA();
    scraper.parseTable3();
  }
}
