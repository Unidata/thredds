/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1.tables;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 11/21/11
 */
public class NcepHtmlScraper {
  private boolean debug = false;
  private boolean show = false;
  private int[] tableVersions = new int[]{2, 0, 128, 129, 130, 131, 133, 140, 0, 0, 141};

  //////////////////////////////////////////////////////////////////
  NcepHtmlScraper() throws IOException {
    String source = "http://www.nco.ncep.noaa.gov/pmb/docs/on388/table2.html";
    File input = new File("C:\\dev\\github\\thredds\\grib\\src\\main\\sources\\ncep\\on388.2011-11-18.htm");
    Document doc = Jsoup.parse(input, "UTF-8", "http://www.nco.ncep.noaa.gov/pmb/docs/on388/");
    //System.out.printf("%s%n", doc);

    int count = 0;
    for (Element e : doc.select("big"))
      System.out.printf("%d == %s%n=%n", count++, e.text());

    Element body = doc.select("body").first();
    Elements tables = body.select("table");
    for (int i = 0; i < tableVersions.length; i++) {
      if (tableVersions[i] == 0) continue;
      Element table = tables.select("table").get(i);
      List<Param> params = readTable(table);

      String name = "NCEP Table Version " + tableVersions[i];
      String filename = "ncepGrib1-" + tableVersions[i];
      makeTableWgrib(name, source, filename + ".tab", params);
      makeTableXml(name, source, filename + ".xml", params);
    }
  }

  private List<Param> readTable(Element table) {
    List<Param> result = new ArrayList<Param>();
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

  private class Param {
    int pnum;
    String desc, unit, name;

    private Param(int pnum, String desc, String unit, String name) {
      this.pnum = pnum;
      this.desc = desc;
      this.unit = StringUtil2.cleanup(unit);
      this.name = StringUtil2.cleanup(name);
    }
  }

  /////////////////////////////////////////////////////////
  String dirOut = "C:\\dev\\github\\thredds\\grib\\src\\main\\resources\\resources\\grib1\\ncep\\";

  private void makeTableXml(String name, String source, String filename, List<Param> params) throws IOException {
    org.jdom.Element rootElem = new org.jdom.Element("parameterMap");
    org.jdom.Document doc = new org.jdom.Document(rootElem);
    rootElem.addContent(new org.jdom.Element("title").setText(name));
    rootElem.addContent(new org.jdom.Element("source").setText(source));

    for (Param p : params) {
      org.jdom.Element paramElem = new org.jdom.Element("parameter");
      paramElem.setAttribute("code", Integer.toString(p.pnum));
      paramElem.addContent(new org.jdom.Element("shortName").setText(p.name));
      paramElem.addContent(new org.jdom.Element("description").setText(p.desc));
      paramElem.addContent(new org.jdom.Element("units").setText(p.unit));
      rootElem.addContent(paramElem);
    }

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    String x = fmt.outputString(doc);

    FileOutputStream fout = new FileOutputStream(dirOut + filename);
    fout.write(x.getBytes());
    fout.close();

    if (show) System.out.printf("%s%n", x);
  }

  private void makeTableWgrib(String name, String source, String filename, List<Param> params) throws IOException {
    Formatter f = new Formatter();
    f.format("# %s%n", name);
    f.format("# %s%n", source);
    for (Param p : params)
      f.format("%3d:%s:%s [%s]%n", p.pnum, p.name, p.desc, p.unit); // 1:PRES:Pressure [Pa]

    FileOutputStream fout = new FileOutputStream(dirOut + filename);
    fout.write(f.toString().getBytes());
    fout.close();

    if (show) System.out.printf("%s%n", f);
  }

  public static void main(String[] args) throws IOException {
    new NcepHtmlScraper();
  }
}
