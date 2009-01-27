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
package ucar.nc2.util.xml;

import org.jdom.input.SAXBuilder;
import org.jdom.*;

import java.io.IOException;
import java.util.List;

import ucar.unidata.util.Format;

public class TimeJdomReading {

  TimeJdomReading(String filename) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(filename);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    Element root = doc.getRootElement();
    List<Element> metars = root.getChildren("metar");
    for (Element metar : metars) {
      List<Element> data = metar.getChildren("data");
      for (Element datum : data) {
        String name = datum.getAttributeValue("name");
        String val = datum.getText();
        MetarField fld = MetarField.fields.get(name);
        if (null == fld)
          fld = new MetarField(name);
        fld.sum(val);
      }
      count++;
    }

    System.out.println("Read from NetCDF; # metars= " + count);
    double took = .001 * (System.currentTimeMillis() - start);
    System.out.println(" that took = " + took + " sec; "+ Format.d(count/took,0)+" metars/sec");
    System.out.println(" memory= "+Runtime.getRuntime().totalMemory());

    for (MetarField f : MetarField.fields.values())
      System.out.println(" "+f.name+ " = "+f.sum);

  }

  public static void main(String args[]) throws IOException {
    String dir = "C:/doc/metarEncoding/save/";
    new TimeJdomReading(dir+"xmlC.xml");
  }
}
