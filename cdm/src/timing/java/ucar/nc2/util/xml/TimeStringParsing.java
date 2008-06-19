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

package ucar.nc2.util.xml;

import org.jdom.Element;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import ucar.nc2.ncml.Aggregation;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.AggregationOuterDimension;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.unidata.util.Format;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 19, 2008
 */
public class TimeStringParsing {

  TimeStringParsing() throws IOException {

    String filename = "file:C:\\Documents and Settings\\caron\\.unidata\\cachePersist\\fileD-godas-singleAgg.ncml";
    Element aggElem = ucar.nc2.util.xml.Parse.readRootElement(filename);

    List<Element> ncList = aggElem.getChildren("netcdf", NcMLReader.ncNS);
    for (Element netcdfElemNested : ncList) {
      String location = netcdfElemNested.getAttributeValue("location");

      List<Element> cacheElemList = netcdfElemNested.getChildren("cache", NcMLReader.ncNS);
      for (Element cacheElemNested : cacheElemList) {
        String varName = cacheElemNested.getAttributeValue("varName");
        String sdata = cacheElemNested.getText();
        System.out.println("text size = "+sdata.length());

        long start = System.nanoTime();
        String[] vals = sdata.split(" ");
        double took =  .001 * .001 * .001 * (System.nanoTime() - start);
        System.out.println(" split took = " + took + " sec; ");

        start = System.nanoTime();
        Array data = Array.makeArray(DataType.DOUBLE, vals);
        took =  .001 * .001 * .001 * (System.nanoTime() - start);
        System.out.println(" makeArray took = " + took + " sec; ");
      }
    }

  }

  public static void main(String args[]) throws IOException {
    new TimeStringParsing();
  }


}
