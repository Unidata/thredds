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
