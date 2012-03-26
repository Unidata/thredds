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
package thredds.catalog;

import junit.framework.*;
import java.util.*;

/** Test reletive URL resolution. */

public class TestResolve1 extends TestCase {
  private static boolean showValidation = false;

  public TestResolve1( String name) {
    super(name);
  }

  String base="http://www.unidata.ucar.edu/";
  String urlString = "TestResolvURI.1.0.xml";
  public void testResolve() {
    InvCatalogImpl cat = TestCatalogAll.open(urlString, true);

    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    System.out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
    System.out.println(" validation output=\n" + buff);
    try {
      cat.writeXML(System.out);
    } catch (java.io.IOException ioe) { assert false; }

    InvService s = cat.findService( "ACD");
    assert s != null;
    System.out.println("ACD service= "+s);

    assert getAccessURL(cat, "nest11").equals("http://www.acd.ucar.edu/dods/testServer/flux/CO2.nc");
    assert getAccessURL(cat, "nest12").equals(base+"netcdf/data/flux/NO2.nc") :
      getAccessURL(cat, "nest12")+" != "+TestCatalogAll.makeFilepath()+"netcdf/data/flux/NO2.nc";

    assert getMetadataURL(cat, "nest1", MetadataType.NETCDF).equals("any.xml");
    assert getMetadataURL(cat, "nest1", MetadataType.ADN).equals("http://you/corrupt.xml");

    assert getDocURL( cat, "nest1", "absolute").equals("http://www.unidata.ucar.edu/");
    assert getDocURL( cat, "nest1", "relative").equals(base+"any.xml");

    assert getCatref( cat.getDatasets(), "ETA data").equals("http://www.unidata.ucar.edu/projects/thredds/xml/InvCatalog5.part2.xml");
    assert getCatref( cat.getDatasets(), "BETA data").equals("/xml/InvCatalog5.part2.xml");
  }

  private String getAccessURL(InvCatalogImpl cat, String name) {
    InvDataset ds = cat.findDatasetByID(name);
    List list = ds.getAccess();
    assert list != null;
    assert list.size() > 0;
    InvAccess a = (InvAccess) list.get(0);
    System.out.println(name+" = "+a.getStandardUrlName());
    return a.getStandardUrlName();
  }

  private String getMetadataURL(InvCatalogImpl cat, String name, MetadataType mtype) {
    InvDataset ds = cat.findDatasetByID(name);
    List list = ds.getMetadata(mtype);
    assert list != null;
    assert list.size() > 0;
    InvMetadata m = (InvMetadata) list.get(0);
    assert m != null;
    System.out.println(name+" = "+m.getXlinkHref());
    assert m.getXlinkHref() != null;
    return m.getXlinkHref();
  }

  private String getDocURL(InvCatalogImpl cat, String name, String title) {
    InvDataset ds = cat.findDatasetByID(name);
    List list = ds.getDocumentation();
    assert list != null;
    assert list.size() > 0;
    for (int i=0; i<list.size(); i++) {
      InvDocumentation elem = (InvDocumentation) list.get(i);
      if (elem.hasXlink() && elem.getXlinkTitle().equals( title)) {
        System.out.println(name+" "+title+" = "+elem.getURI());
        return elem.getURI().toString();
      }
    }
    return null;
  }

  private String getCatref(List list, String name) {
    for (int i=0; i<list.size(); i++) {
      InvDataset elem = (InvDataset) list.get(i);
      System.out.println("elemname= "+elem.getName());
      if (elem.getName().equals(name)) {
        assert elem instanceof InvCatalogRef;
        InvCatalogRef catref = (InvCatalogRef) elem;
        System.out.println(name+" = "+catref.getXlinkHref());
        return catref.getXlinkHref();
      }
    }
    return null;
  }

}
