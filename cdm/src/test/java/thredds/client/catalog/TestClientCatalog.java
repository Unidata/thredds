/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.tools.CatalogXmlWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.units.TimeUnit;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Unit tests for client catalogs
 *
 * @author caron
 * @since 1/16/2015
 */
public class TestClientCatalog {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testResolve() throws IOException {
    Catalog cat = ClientCatalogUtil.open("testCatref.xml");
    Assert.assertEquals(
            "catrefURI", ClientCatalogUtil.makeFilepath("test2.xml"), getCatrefURI(cat.getDatasets(), "catref"));

    String catrefURIn = getCatrefNestedURI(cat, "top", "catref-nested");
    assert catrefURIn.equals(ClientCatalogUtil.makeFilepath("test0.xml")) : catrefURIn;
  }

  private CatalogRef getCatrefNested(Catalog cat, String id, String catName) {
    Dataset ds = cat.findDatasetByID(id);
    assert ds != null;
    return getCatref(ds.getDatasets(), catName);
  }

  private CatalogRef getCatref(List<Dataset> list, String name) {
    for (Dataset ds : list) {
      if (ds.getName().equals(name)) {
        assert ds instanceof CatalogRef;
        CatalogRef catref = (CatalogRef) ds;
        logger.debug("{} = {} == {}", name, catref.getXlinkHref(), catref.getURI());
        return catref;
      }
    }
    return null;
  }

  private String getCatrefURI(List<Dataset> list, String name) {
    CatalogRef catref = getCatref(list, name);
    if (catref != null)
      return catref.getURI().toString();
    return null;
  }

  private String getCatrefNestedURI(Catalog cat, String id, String catName) {
    return getCatrefNested(cat, id, catName).getURI().toString();
  }


  @Test
  public void testDeferredRead() throws IOException {
    Catalog cat = ClientCatalogUtil.open("testCatref.xml");

    CatalogRef catref = getCatref(cat.getDatasets(), "catref");
    assert (!catref.isRead());

    catref = getCatrefNested(cat, "top", "catref-nested");
    assert (!catref.isRead());
  }

  ////////////////////////

  @Test
  public void testNested() throws IOException {
    Catalog cat = ClientCatalogUtil.open("nestedServices.xml");
    assert cat != null;

    Dataset ds = cat.findDatasetByID("top");
    assert ds != null;
    assert ds.getServiceDefault() != null : ds.getID();

    ds = cat.findDatasetByID("nest1");
    assert ds != null;
    assert ds.getServiceDefault() != null : ds.getID();

    ds = cat.findDatasetByID("nest2");
    assert ds != null;
    assert ds.getServiceDefault() != null : ds.getID();

    logger.debug("OK");
  }

  ////////////////////////////

  @Test
  public void testGC() throws Exception {
    Catalog cat = ClientCatalogUtil.open("MissingGCProblem.xml");
    assert cat != null;

    Dataset ds = cat.findDatasetByID("hasGC");
    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    assert null != gc;
    assert gc.getHeightStart() == 5.0 : gc.getHeightStart();
    assert gc.getHeightExtent() == 47.0 : gc.getHeightExtent();

    assert gc.getEastWestRange() == null;
    assert gc.getNorthSouthRange() == null;
  }

  @Test
  public void testTimeCoverage() throws Exception {
    Catalog cat = ClientCatalogUtil.open("TestTimeCoverage.xml");
    assert cat != null;

    Dataset ds = cat.findDatasetByID("test1");
    DateRange tc = ds.getTimeCoverage();
    assert null != tc;
    logger.debug("tc = {}", tc);
    assert tc.getEnd().isPresent();
    assert tc.getResolution() == null;
    assert tc.getDuration().equals(new TimeDuration("14 days"));

    ds = cat.findDatasetByID("test2");
    tc = ds.getTimeCoverage();
    assert null != tc;
    logger.debug("tc = {}", tc);
    CalendarDate got = tc.getStart().getCalendarDate();
    CalendarDate want = CalendarDateFormatter.isoStringToCalendarDate(null, "1999-11-16T12:00:00");
    assert got.equals(want);
    assert tc.getResolution() == null;
    TimeDuration gott = tc.getDuration();
    TimeDuration wantt = new TimeDuration("P3M");
    assert gott.equals(wantt);

    ds = cat.findDatasetByID("test3");
    tc = ds.getTimeCoverage();
    assert null != tc;
    logger.debug("tc = {}", tc);
    assert tc.getResolution() == null;
    assert tc.getDuration().equals(new TimeDuration("2 days"));

    ds = cat.findDatasetByID("test4");
    tc = ds.getTimeCoverage();
    assert null != tc;
    logger.debug("tc = {}", tc);
    TimeDuration r = tc.getResolution();
    assert r != null;
    TimeDuration r2 = new TimeDuration("3 hour");
    assert r.equals(r2);
    TimeDuration d = tc.getDuration();
    TimeUnit tu = d.getTimeUnit();
    assert tu.getUnitString().equals("days") : tu.getUnitString(); // LOOK should be 3 hours, or hours or ??

    ds = cat.findDatasetByID("test5");
    tc = ds.getTimeCoverage();
    assert null != tc;
    logger.debug("tc = {}", tc);

    CalendarDate start = tc.getStart().getCalendarDate();
    assert start.getCalendar() == Calendar.uniform30day;  // Using non-default calendar.

    // This date is valid in the uniform30day calendar. If we tried it with the standard calendar, we'd get an error:
    //     Illegal base time specification: '2017-02-30' Value 30 for dayOfMonth must be in the range [1,28]
    assert CalendarDateFormatter.toDateString(start).equals("2017-02-30");

    CalendarDate end = tc.getEnd().getCalendarDate();
    assert CalendarDateFormatter.toDateString(end).equals("2017-04-01");

    // In the uniform30day calendar, the difference between 2017-02-30 and 2017-04-01 is 31 days.
    assert end.getDifference(start, CalendarPeriod.Field.Day) == 31;
  }

  /////////////

  @Test
  public void testVariables() throws IOException {
    Catalog cat = ClientCatalogUtil.open("TestHarvest.xml");
    assert cat != null;

    Dataset ds = cat.findDatasetByID("solve1.dc8");
    assert ds != null;

    List<ThreddsMetadata.VariableGroup> list = ds.getVariables();
    assert list != null;
    assert list.size() >= 2;

    ThreddsMetadata.VariableGroup vars = getType(list, "CF-1.0");
    assert vars != null;
    checkVariable(vars, "wv", "Wind Speed");
    checkVariable(vars, "o3c", "Ozone Concentration");

    ThreddsMetadata.VariableGroup dif = getType(list, "DIF");
    assert dif != null;
    checkVariable(dif, "wind_from_direction",
            "EARTH SCIENCE > Atmosphere > Atmosphere Winds > Surface Winds > wind_from_direction");
  }

  ThreddsMetadata.VariableGroup getType(List<ThreddsMetadata.VariableGroup> list, String type) {
    for (ThreddsMetadata.VariableGroup vars : list) {
      if (vars.getVocabulary().equals(type)) return vars;
    }
    return null;
  }

  void checkVariable(ThreddsMetadata.VariableGroup vars, String name, String vname) {
    List<ThreddsMetadata.Variable> list = vars.getVariableList();
    for (ThreddsMetadata.Variable var : list) {
      if (var.getName().equals(name)) {
        assert var.getVocabularyName().equals(vname);
        return;
      }
    }
    assert false : "cant find " + name;
  }

  /////////////////

  @Test
  public void testSubset() throws IOException {
    Catalog cat = ClientCatalogUtil.open("InvCatalog-1.0.xml");
    CatalogXmlWriter writer = new CatalogXmlWriter();
    logger.debug("{}", writer.writeXML(cat));

    Dataset ds = cat.findDatasetByID("testSubset");
    assert (ds != null) : "cant find dataset 'testSubset'";
    assert ds.getFeatureType() == FeatureType.GRID;

    Catalog subsetCat = cat.subsetCatalogOnDataset(ds);
    logger.debug("{}", writer.writeXML(subsetCat));

    List<Dataset> dss = subsetCat.getDatasets();
    assert dss.size() == 1;
    Dataset first = dss.get(0);
    assert first.getServiceNameDefault() != null;
    assert first.getServiceNameDefault().equals("ACD");

    Dataset subsetDs = subsetCat.findDatasetByID("testSubset");
    assert subsetDs != null;
    assert subsetDs.getServiceNameDefault() != null;
    assert subsetDs.getServiceNameDefault().equals("ACD");
    assert subsetDs.getFeatureTypeName() != null;
    assert subsetDs.getFeatureTypeName().equalsIgnoreCase("Grid");
  }
}
