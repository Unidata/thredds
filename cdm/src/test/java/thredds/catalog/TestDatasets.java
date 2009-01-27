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
import ucar.nc2.constants.FeatureType;

public class TestDatasets extends TestCase {

  public TestDatasets( String name) {
    super(name);
  }

  public void testDataType() {
    InvCatalogImpl cat = TestCatalogAll.open("InvCatalog.0.6.xml", true);

    InvDataset ds = cat.findDatasetByID("testSubset");
    assert (ds != null) : "cant find dataset 'testSubset'";
    assert ds.getDataType() == FeatureType.GRID;

    InvDataset co2 = ds.findDatasetByName("CO2");
    assert (co2 != null) : "cant find dataset 'CO2'";
    assert co2.getDataType() == FeatureType.IMAGE;

    InvDataset no2 = ds.findDatasetByName("NO2");
    assert (no2 != null) : "cant find dataset 'NO2'";
    assert no2.getDataType() == FeatureType.GRID;
  }

  public void testAccessPath() {
    InvCatalogImpl cat = TestCatalogAll.open("InvCatalog.0.6.xml", true);

    InvDataset ds = cat.findDatasetByID("CO2_Flux");
    assert (ds != null) : "cant find dataset 'CO2_Flux'";

    InvAccess a = ds.getAccess(ServiceType.DODS);
    assert a != null;
    assert a.getStandardUrlName().equals("http://motherlode.ucar.edu/cgi-bin/dods/1998/CO2.nc") : a.getStandardUrlName();

    a = ds.getAccess(ServiceType.NETCDF);
    assert a != null;
    assert a.getUnresolvedUrlName().equals("1998/CO2.nc") : a.getUnresolvedUrlName();
    assert a.getStandardUrlName().equals(TestCatalogAll.makeFilepath() +"1998/CO2.nc") : a.getStandardUrlName();

  }

  public void testExpandedAccess() {
    InvCatalogImpl cat = TestCatalogAll.open("InvCatalog.0.6.xml", true);

    InvDataset ds = cat.findDatasetByID("HasCompoundService");
    assert (ds != null) : "cant find dataset 'HasCompoundService'";

    java.util.List access = ds.getAccess();
    assert access.size() == 2 : access.size();

    InvAccess a = ds.getAccess(ServiceType.DODS);
    assert a != null;
    a = ds.getAccess(ServiceType.FTP);
    assert a != null;

  }

  public void testFullName() {
    InvCatalogImpl cat = TestCatalogAll.openAbsolute("http://lead.unidata.ucar.edu:8080/thredds/idd/obsData.xml", true);

    InvDataset ds = cat.findDatasetByID("NWS/RASS/1hour");
    assert (ds != null) : "cant find dataset";

    System.out.println(" fullName= "+ds.getFullName());
    System.out.println(" name= "+ds.getName());
  }

}