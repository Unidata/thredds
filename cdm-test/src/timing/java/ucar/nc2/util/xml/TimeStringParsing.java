/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.xml;

import org.jdom2.Element;

import java.io.*;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.DataType;

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

    List<Element> ncList = aggElem.getChildren("netcdf", thredds.client.catalog.Catalog.ncmlNS);
    for (Element netcdfElemNested : ncList) {
      String location = netcdfElemNested.getAttributeValue("location");

      List<Element> cacheElemList = netcdfElemNested.getChildren("cache", thredds.client.catalog.Catalog.ncmlNS);
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
