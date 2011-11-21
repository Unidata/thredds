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
import ucar.nc2.dataset.CoordinateAxis;

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

  //////////////////////////////////////////////////////////////////
  NcepHtmlScraper() throws IOException {
    String source = "http://www.nco.ncep.noaa.gov/pmb/docs/on388/table2.html";
    File input = new File("C:\\dev\\github\\thredds\\grib\\src\\main\\sources\\ncep\\on388.2011-11-18.htm");
    Document doc = Jsoup.parse(input, "UTF-8", "http://www.nco.ncep.noaa.gov/pmb/docs/on388/");
    //System.out.printf("%s%n", doc);

    Element body = doc.select("body").first();
    Elements tables = doc.select("table");
    Element table = doc.select("table").first();
    //for (Element e : tables) System.out.printf("%s%n=%n", e.text());
    //System.out.printf("%s%n=%n", table);
    List<Param> params = readTable(table);

    makeTableWgrib("NCEP table 2", source, "ncepGrib1.tab", params);
    makeTableXml("NCEP table 2", source, "ncepGrib1.xml", params);
  }

  private List<Param> readTable(Element table) {
    List<Param> result = new ArrayList <Param>();
     Elements rows = table.select("tr");
    for (Element row : rows) {
      Elements cols = row.select("td");
      System.out.printf(" %d=", cols.size());
      for (Element col : cols)
        System.out.printf("%s:", col.text());
      System.out.printf("%n");

      if (cols.size() == 4) {
        String snum = cols.get(0).text();
        try {
          int pnum = Integer.parseInt(snum);
          result.add(new Param(pnum, cols.get(1).text(),cols.get(2).text(),cols.get(3).text()));
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
      this.unit = unit;
      this.name = name;
    }
  }
  /////////////////////////////////////////////////////////
  String dirOut =  "C:\\dev\\github\\thredds\\grib\\src\\main\\resources\\resources\\grib1\\ncep\\";

  private void makeTableXml(String name, String source, String filename, List<Param> params) throws IOException {
    org.jdom.Element rootElem = new org.jdom.Element("parameterMap");
    org.jdom.Document doc = new org.jdom.Document(rootElem);
    rootElem.addContent( new org.jdom.Element("title").setText(name));
    rootElem.addContent( new org.jdom.Element("source").setText(source));

    for (Param p : params) {
      org.jdom.Element paramElem = new org.jdom.Element("parameter");
      paramElem.setAttribute("code", Integer.toString(p.pnum));
      paramElem.addContent( new org.jdom.Element("shortName").setText(p.name));
      paramElem.addContent( new org.jdom.Element("description").setText(p.desc));
      paramElem.addContent( new org.jdom.Element("units").setText(p.unit));
      rootElem.addContent(paramElem);
    }

    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    String x = fmt.outputString( doc);

    FileOutputStream fout = new FileOutputStream(dirOut+filename);
    fout.write(x.getBytes());
    fout.close();

    System.out.printf("%s%n", x);
  }

  private void makeTableWgrib(String name, String source, String filename, List<Param> params) throws IOException {
    Formatter f = new Formatter();
    f.format("# %s%n", name);
    f.format("# %s%n", source);
    for (Param p : params)
      f.format("%3d:%s:%s [%s]%n", p.pnum, p.name, p.desc, p.unit); // 1:PRES:Pressure [Pa]

    FileOutputStream fout = new FileOutputStream(dirOut+filename);
    fout.write(f.toString().getBytes());
    fout.close();

    System.out.printf("%s%n", f);
  }

  public static void main(String[] args) throws IOException {
    new NcepHtmlScraper();
  }
}
