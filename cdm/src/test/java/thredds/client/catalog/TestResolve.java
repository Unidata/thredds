/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package thredds.client.catalog;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/** Test relative URL resolution. */
public class TestResolve {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String base="http://www.unidata.ucar.edu/";
  String urlString = "TestResolvURI.1.0.xml";
  
  @Test
  public void testResolve() throws IOException {
    Catalog cat = ClientCatalogUtil.open(urlString);
    assert cat != null;

    Service s = cat.findService( "ACD");
    assert s != null;
    logger.debug("ACD service= {}", s);

    assert getAccessURL(cat, "nest11").equals("http://www.acd.ucar.edu/dods/testServer/flux/CO2.nc");
    assert getAccessURL(cat, "nest12").equals(base+"netcdf/data/flux/NO2.nc") :
      getAccessURL(cat, "nest12")+" != "+ClientCatalogUtil.makeFilepath()+"netcdf/data/flux/NO2.nc";

    assert getMetadataURL(cat, "nest1", "NETCDF").equals("any.xml");
    assert getMetadataURL(cat, "nest1", "ADN").equals("http://you/corrupt.xml");

    String docUrl = getDocURL( cat, "nest1", "absolute");
    assert docUrl.equals("http://www.unidata.ucar.edu/") : docUrl;

    docUrl = getDocURL(cat, "nest1", "relative");
    assert docUrl.equals(base+"any.xml") : docUrl;

    assert getCatref( cat.getDatasets(), "ETA data").equals("http://www.unidata.ucar.edu/projects/thredds/xml/InvCatalog5.part2.xml");
    assert getCatref( cat.getDatasets(), "BETA data").equals("/xml/InvCatalog5.part2.xml");
  }

  private String getAccessURL(Catalog cat, String name) {
    Dataset ds = cat.findDatasetByID(name);
    List list = ds.getAccess();
    assert list != null;
    assert list.size() > 0;
    Access a = (Access) list.get(0);
    logger.debug("{} = {}", name, a.getStandardUrlName());
    return a.getStandardUrlName();
  }

  private String getMetadataURL(Catalog cat, String name, String mtype) {
    Dataset ds = cat.findDatasetByID(name);
    List<ThreddsMetadata.MetadataOther> list = ds.getMetadata(mtype);
    assert list != null;
    assert list.size() > 0;
    ThreddsMetadata.MetadataOther m = list.get(0);
    assert m != null;
    logger.debug("{} = {}", name, m.getXlinkHref());
    assert m.getXlinkHref() != null;
    return m.getXlinkHref();
  }

  private String getDocURL(Catalog cat, String name, String title) {
    Dataset ds = cat.findDatasetByID(name);
    List<Documentation> list = ds.getDocumentation();
    assert list != null;
    assert list.size() > 0;
    for (Documentation elem : list) {
      if (elem.hasXlink() && elem.getXlinkTitle().equals(title)) {
        logger.debug("{} {} = {}", name, title, elem.getURI());
        return elem.getURI().toString();
      }
    }
    return null;
  }

  private String getCatref(List list, String name) {
    for (int i=0; i<list.size(); i++) {
      Dataset elem = (Dataset) list.get(i);
      logger.debug("elemname = {}", elem.getName());
      if (elem.getName().equals(name)) {
        assert elem instanceof CatalogRef;
        CatalogRef catref = (CatalogRef) elem;
        logger.debug("{} = {}", name, catref.getXlinkHref());
        return catref.getXlinkHref();
      }
    }
    return null;
  }
}
