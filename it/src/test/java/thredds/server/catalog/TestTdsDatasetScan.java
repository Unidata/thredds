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
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import ucar.nc2.units.DateRange;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.List;

@Category(NeedsCdmUnitTest.class)
public class TestTdsDatasetScan {
  @Test
  public void testSort() throws IOException {
    Catalog cat = TestTdsLocal.open("catalog/scanCdmUnitTests/tds/ncep/catalog.xml");

    Dataset last = null;
    for (Dataset ds : cat.getDatasets()) {
      if (last != null)
        assert ds.getName().compareTo(last.getName()) > 0;
      last = ds;
    }
  }

  @Test
  public void testLatest() throws IOException {
    Catalog cat = TestTdsLocal.open("catalog/testGFSfmrc/files/latest.xml");
    List<Dataset> dss = cat.getDatasets();
    assert (dss.size() == 1);

    Dataset ds = dss.get(0);
    assert ds.hasAccess();
    assert ds.getDatasets().size() == 0;

    assert ds.getID() != null;
    assert ds.getDataSize() > 0.0;

    List<thredds.client.catalog.ThreddsMetadata.Vocab> keywords = ds.getKeywords();
    Assert.assertEquals("Number of keywords", 2, keywords.size());

    DateRange dr = ds.getTimeCoverage();
    Assert.assertNotNull("TimeCoverage", dr);
  }

  @Test
  public void testHarvest() throws IOException {
    Catalog cat = TestTdsLocal.open("catalog/testEnhanced/catalog.xml");
    assert cat != null;
    List<Dataset> topList = cat.getDatasets();
    assert topList.size() == 1;
    Dataset top = topList.get(0);
    assert top != null;
    assert top.isHarvest();

    List<Dataset> dss = top.getDatasets();
    assert (dss.size() > 0);
    Dataset nested = dss.get(0);
    assert !nested.isHarvest();

    cat = TestTdsLocal.open("/catalog.xml");
    Dataset ds = cat.findDatasetByID("testDataset");
    assert ds != null;
    assert !ds.isHarvest();
  }

  @Test
  public void testNestedDirs() throws IOException {
    Catalog cat = TestTdsLocal.open("catalog/station/profiler/wind/06min/catalog.xml");

    List<Dataset> topList = cat.getDatasets();
    assert topList.size() == 1;
    Dataset top = topList.get(0);
    assert top != null;
    List<Dataset> children = top.getDatasets();
    Assert.assertEquals(3, children.size()); // latest + 2

  }


}
