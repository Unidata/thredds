/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Read NCEP html files to extract the GRIB-2 tables.
 *
 * @author caron
 * @since 1/7/12
 */
public class NcepHtmlScraper  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //String dirOut = "C:\\dev\\github\\thredds\\grib\\src\\main\\sources\\ncep\\temp\\";
  private static final String dirOut = "C:/tmp/ncep/";

  static private final boolean debugParam = false;
  static private final boolean debug = false;
  static private final boolean show = false;


  //////////////////////////////////////////////////////////////////
  // http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml

  void parseTopDoc() throws IOException {
    String source = "http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml";
    Document doc = Jsoup.parse(new URL(source), 5 * 1000); // 5 sec timeout
    //System.out.printf("%s%n", doc);

    Elements links = doc.select("a[href]");
    for (Element link : links) {
      //System.out.printf("%s", link);
      Node sib = link.nextSibling();
      String title = null;
      if (sib != null) {
        String sibt = sib.toString();
        title = StringUtil2.remove(sibt, "-").trim();
        //System.out.printf(" == '%s'", title);
      }
      if (link.text().equals("Table 4.2")) {
        //System.out.printf(" == ");
        parseTable42(link.attr("abs:href"), link.text(), title);

      } else {
        if (link.text().startsWith("Table 4")) {
          //System.out.printf(" == ");
          parseCodeTable(link.attr("abs:href"), link.text(), title);
        }
      }
      //System.out.printf("%n");
    }
  }

  void parseCodeTable(String url, String tableName, String title) throws IOException {
    System.out.printf("parseCodeTable url=%s tableName=%s title=%s%n", url, tableName, title);
    Document doc = Jsoup.parse(new URL(url), 5 * 1000); // 5 sec timeout
    //System.out.printf("%s%n", doc);

    if (title == null) title = "NCEP GRIB-2 Code Table";
    // System.out.printf("%s%n", title);

    Element table = doc.select("table").first();
    List<Code> stuff = new ArrayList<>();
    Elements rows = table.select("tr");
    for (Element row : rows) {
      Elements cols = row.select("td");
      if (debug) {
        System.out.printf(" #cols=%d: ", cols.size());
        for (Element col : cols)
          System.out.printf("%s:", col.text());
        System.out.printf("%n");
      }

      if (cols.size() >= 2) {
        String snum = StringUtil2.cleanup(cols.get(0).text()).trim();
        String desc = StringUtil2.cleanup(cols.get(1).text()).trim();
        if (snum.contains("Reserved") || desc.contains("Reserved") ) {
          if (debug) System.out.printf("*** Skip Reserved %s%n", row.text());
          continue;
        }

        try {
          int pnum = Integer.parseInt(snum);
          if (debug) System.out.printf("val %d == %s%n", pnum, desc);
          stuff.add(new Code(pnum, desc));
        } catch (NumberFormatException e) {
          System.out.printf("*** Cant parse %s == %s%n", snum, row.text());
        }

      }
    }

    String filename = StringUtil2.removeWhitespace(tableName);
    writeCodeTableXml(filename, title, url, tableName, stuff);
  }

  private static class Code {
    int no;
    String desc;

    private Code(int no, String desc) {
      this.no = no;
      this.desc = desc;
    }
  }
  //     writeCodeTableXml(filename, title, url, tableName, stuff);

  private void writeCodeTableXml(String filename, String title, String source, String tableName, List<Code> stuff) throws IOException {
    org.jdom2.Element rootElem = new org.jdom2.Element("codeTable");
    org.jdom2.Document doc = new org.jdom2.Document(rootElem);
    rootElem.addContent(new org.jdom2.Element("table").setText(tableName));
    rootElem.addContent(new org.jdom2.Element("title").setText(title));
    rootElem.addContent(new org.jdom2.Element("source").setText(source));

    for (Code p : stuff) {
      org.jdom2.Element paramElem = new org.jdom2.Element("parameter");
      paramElem.setAttribute("code", Integer.toString(p.no));
      paramElem.addContent(new org.jdom2.Element("description").setText(p.desc));
      rootElem.addContent(paramElem);
    }

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    String x = fmt.outputString(doc);

    try (FileOutputStream fout = new FileOutputStream(dirOut + filename+".xml")) {
      fout.write(x.getBytes(CDM.utf8Charset));
    }

    if (show) System.out.printf("%s%n", x);
  }

  ///////////////////////////////////////////////////////////////////////

  void parseTable42(String url, String tableName, String title) throws IOException {
    System.out.printf("parseTable42 url=%s tableName=%s title=%s%n", url, tableName, title);
    Document doc = Jsoup.parse(new URL(url), 5 * 1000); // 5 sec timeout
    //System.out.printf("%s%n", doc);

    Elements links = doc.select("a[href]");
    for (Element link : links) {
      //System.out.printf("link = %s%n", link);
      //for (Node sib : link.siblingNodes()) System.out.printf("  %s%n", sib);
      //System.out.printf("%n");
      parseParamTable(link.attr("abs:href"), link.text());
    }

  }

  void parseParamTable(String url, String title) throws IOException {
    String match = "grib2_table4";
    if (!url.contains(match)) return;

    System.out.printf("parseParamTable url=%s title=%s%n", url, title);
    Document doc = Jsoup.parse(new URL(url), 5 * 1000); // 5 sec timeout
    //System.out.printf("%s%n", doc);

    if (title == null) title = "NCEP GRIB-2 Param Table";
    // System.out.printf("%s%n", title);

    Element table = doc.select("table").first();
    assert table != null;

    List<Param> stuff = new ArrayList<>();
    Elements rows = table.select("tr");
    for (Element row : rows) {
      Elements cols = row.select("td");
      if (debugParam) {
        System.out.printf(" #cols=%d: ", cols.size());
        for (Element col : cols)
          System.out.printf("%s:", col.text());
        System.out.printf("%n");
      }

      if (cols.size() == 4) {
        String snum = StringUtil2.cleanup(cols.get(0).text()).trim();
        String desc = StringUtil2.cleanup(cols.get(1).text()).trim();
        if (snum.contains("Reserved") || desc.contains("Reserved") || desc.contains("Missing") ) {
          if (debugParam) System.out.printf("*** Skip Reserved %s%n", row.text());
          continue;
        }

        try {
          int pnum = Integer.parseInt(snum);
          String units = cols.get(2).text();
          String abbrev = cols.get(3).text();
          if (debugParam) System.out.printf("val %d == %s %s %s%n", pnum, desc, units, abbrev);
          stuff.add(new Param(pnum, desc, units, abbrev));

        } catch (NumberFormatException e) {
          System.out.printf("*** Cant parse %s == %s%n", snum, row.text());
        }

      } else if (cols.size() == 3) {
        String snum = StringUtil2.cleanup(cols.get(0).text()).trim();
        String desc = StringUtil2.cleanup(cols.get(1).text()).trim();
        if (snum.contains("Reserved") || desc.contains("Reserved") || desc.contains("Missing") ) {
          if (debugParam) System.out.printf("*** Skip Reserved %s%n", row.text());
          continue;
        }

        try {
          int pnum = Integer.parseInt(snum);
          String units = cols.get(2).text();
          if (debugParam) System.out.printf("val %d == %s %s%n", pnum, desc, units);
          stuff.add(new Param(pnum, desc, units, null));

        } catch (NumberFormatException e) {
          System.out.printf("*** Cant parse %s == %s%n", snum, row.text());
        }
      }

    }

    // grib2_table4-2-0-0.shtml
    int pos = url.indexOf(match) + match.length();
    int lastPos = url.lastIndexOf('.');
    String filename = "Table4" + url.substring(pos, lastPos);
    filename = StringUtil2.removeWhitespace(filename);
    filename = StringUtil2.substitute(filename,"-", ".");
    writeParamTableXml(filename, title, url, filename, stuff);
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


  //     writeCodeTableXml(filename, title, url, tableName, stuff);

  private void writeParamTableXml(String filename, String title, String source, String tableName, List<Param> stuff) throws IOException {
    org.jdom2.Element rootElem = new org.jdom2.Element("parameterMap");
    org.jdom2.Document doc = new org.jdom2.Document(rootElem);
    rootElem.addContent(new org.jdom2.Element("table").setText(tableName));
    rootElem.addContent(new org.jdom2.Element("title").setText(title));
    rootElem.addContent(new org.jdom2.Element("source").setText(source));

    for (Param p : stuff) {
      org.jdom2.Element paramElem = new org.jdom2.Element("parameter");
      paramElem.setAttribute("code", Integer.toString(p.pnum));
      paramElem.addContent(new org.jdom2.Element("shortName").setText(p.name));
      paramElem.addContent(new org.jdom2.Element("description").setText(p.desc));
      paramElem.addContent(new org.jdom2.Element("units").setText(p.unit));
      rootElem.addContent(paramElem);
    }

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    String x = fmt.outputString(doc);

    try (FileOutputStream fout = new FileOutputStream(dirOut + filename+".xml")) {
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

  // C:\dev\github\thredds\grib\src\main\resources\resources\grib2\ncep

  public static void main(String[] args) throws IOException {
    File dir = new File(dirOut);
    if (!dir.mkdirs()) System.out.printf("mkdir %s failed %n", dir.getPath());
    NcepHtmlScraper scraper = new NcepHtmlScraper();
    scraper.parseTopDoc();
  }
}

