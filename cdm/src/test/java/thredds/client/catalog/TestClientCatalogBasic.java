/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sanity test, just read the catalogs
 *
 * @author caron
 * @since 1/15/2015
 */
@RunWith(Parameterized.class)
public class TestClientCatalogBasic {

  @Parameterized.Parameters
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // Grib files, one from each model
    result.add(new Object[]{"enhancedCat.xml"});
    result.add(new Object[]{"InvCatalogBadDTD.xml"});
    result.add(new Object[]{"TestAlias.xml"});
    result.add(new Object[]{"testMetadata.xml"});
    result.add(new Object[]{"nestedServices.xml"});
    result.add(new Object[]{"TestHarvest.xml"});
    result.add(new Object[]{"TestFilter.xml"});
    result.add(new Object[]{"http://thredds-test.unidata.ucar.edu/thredds/catalog.xml"});
    result.add(new Object[]{"http://thredds-test.unidata.ucar.edu/thredds/catalog/nws/metar/ncdecoded/catalog.xml?dataset=nws/metar/ncdecoded/Metar_Station_Data_fc.cdmr"});
    return result;
  }


  Catalog cat;

  public TestClientCatalogBasic(String catFrag) throws IOException {
    cat = TestClientCatalog.open(catFrag);
  }

  @Test
  public void testCatalog() {
    for (Dataset ds : cat.getDatasets())
      testDatasets(ds);
  }

  public void testDatasets(Dataset d) {
    testAccess(d);
    testProperty(d);
    testDocs(d);
    testMetadata(d);
    testContributors(d);
    testKeywords(d);
    testProjects(d);
    testPublishers(d);
    testVariables(d);

    for (Dataset ds : d.getDatasets()) {
      testDatasets(ds);
    }

  }

  public void testAccess(Dataset d) {
    for (Access a : d.getAccess()) {
      assert a.getService() != null;
      assert a.getUrlPath() != null;
      assert a.getDataset().equals(d);
      testService(a.getService());
    }
  }

  public void testProperty(Dataset d) {
    for (Property p : d.getProperties()) {
      System.out.printf("%s%n", p);
    }
  }

  public void testDocs(Dataset d) {
    for (Documentation doc : d.getDocumentation()) {
      System.out.printf("%s%n", doc);
    }
  }


  public void testService(Service s) {
    List<Service> n = s.getNestedServices();
    if (n == null) return;
    if (s.getType() == ServiceType.Compound)
      assert n.size() > 0;
    else
      assert n.size() == 0;
  }


  public void testMetadata(Dataset d) {
    for (ThreddsMetadata.MetadataOther m : d.getMetadataOther()) {
      System.out.printf("%s%n", m.getXlinkHref());
    }
  }

  public void testContributors(Dataset d) {
    for (ThreddsMetadata.Contributor m : d.getContributors()) {
      System.out.printf("%s%n", m.getName());
    }
  }

  public void testKeywords(Dataset d) {
    for (ThreddsMetadata.Vocab m : d.getKeywords()) {
      System.out.printf("%s%n", m.getText());
    }
  }

  public void testProjects(Dataset d) {
    for (ThreddsMetadata.Vocab m : d.getProjects()) {
      System.out.printf("%s%n", m.getText());
    }
  }

  public void testPublishers(Dataset d) {
    for (ThreddsMetadata.Source m : d.getPublishers()) {
      System.out.printf("%s%n", m.getName());
    }
  }

  public void testVariables(Dataset d) {
    for (ThreddsMetadata.VariableGroup m : d.getVariables()) {
      System.out.printf("%s%n", m.getVocabulary());
    }
  }
}
