/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
