// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.catalog;

import org.jdom.Element;

import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URISyntaxException;
import java.net.URI;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dt.grid.ForecastModelRunCollection;
import ucar.nc2.dt.grid.FmrcImpl;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.Attribute;
import ucar.unidata.util.StringUtil;

/**
 * InvDatasetFmrc deals with datasetFmrc elementss.
 *
 * @author caron
 */
public class InvDatasetFmrc extends InvCatalogRef {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFmrc.class);

  static private final String FMRC = "fmrc.ncd";
  static private final String BEST = "best.ncd";
  static private final String RUNS = "runs";
  static private final String RUN_NAME = "RUN_";
  static private final String FORECAST = "forecast";
  static private final String FORECAST_NAME = "ConstantForecast_";
  static private final String OFFSET = "offset";
  static private final String OFFSET_NAME = "Offset_";

  static private final String TITLE_RUNS = "Forecast Model Run";
  static private final String TITLE_OFFSET = "Constant Forecast Offset";
  static private final String TITLE_FORECAST = "Constant Forecast Date";

  //////////////////////////////////////////////

  private boolean madeDatasets = false, madeFmrc = false;
  private String path;
  private ForecastModelRunCollection fmrc;
  private InvCatalogImpl catalog, catalogRuns, catalogOffsets, catalogForecasts;
  private InventoryParams params;

  public InvDatasetFmrc(InvDatasetImpl parent, String name, String path) {
    super(parent, name, "/thredds/catalog/"+path+"/catalog.xml");
    this.path = path;
  }

  public String getPath() { return path; }

  public InventoryParams getFmrcInventoryParams() {
    return params;
  }

  public void setFmrcInventoryParams(String location, String def, String suffix) {
    this.params = new InventoryParams();
    this.params.location = location;
    this.params.def = def;
    this.params.suffix = suffix;
  }

  public class InventoryParams {
    public String location, def, suffix;
    public String toString() { return "def="+def +" location="+ location+" suffix="+ suffix; }
  }

  public boolean hasAccess() {
    return false;
  }

  public boolean hasNestedDatasets() {
    return true;
  }

  public InvCatalogImpl makeCatalog(String match) {
    try {
      if ((match == null) || (match.length() == 0))
        return makeCatalog();
      else if (match.equals(RUNS))
        return makeCatalogRuns();
      else if (match.equals(OFFSET))
        return makeCatalogOffsets();
      else if (match.equals(FORECAST))
        return makeCatalogForecasts();
      else
        return null;
    } catch (Exception e) {
      logger.error("makeCatalog", e);
      return null;
    }
  }

  private InvCatalogImpl makeCatalog() throws URISyntaxException {

    if (catalog == null) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      URI baseUri = new URI(getXlinkHref());
      catalog = new InvCatalogImpl( getFullName(), parent.getVersion(), baseUri);

      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local

      String id = getID();
      if (id == null)
        id = getPath();
      top.setID(id);

      makeFmrc();
      ThreddsMetadata tmi = top.getLocalMetadataInheritable();
      ThreddsMetadata.Variables vars = MetadataExtractor.extractVariables( this, fmrc.getGridDataset());
      tmi.addVariables(vars);
      ThreddsMetadata.GeospatialCoverage gc = MetadataExtractor.extractGeospatial(fmrc.getGridDataset());
      tmi.setGeospatialCoverage(gc);

      if (null != params) {
        ThreddsMetadata tm = top.getLocalMetadata();
        InvDocumentation doc = new InvDocumentation();
        String path = getPath();
        if (!path.endsWith("/")) path = path + "/";
        doc.setXlinkHref( "/thredds/modelInventory/"+path);
        doc.setXlinkTitle( "Available Inventory");
        tm.addDocumentation( doc);
      }

      catalog.addDataset(top);

      // any referenced services need to be local
      ArrayList services = new ArrayList( getServicesLocal());
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        catalog.addService(service);

      List datasets = getDatasets();
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
        top.addDataset(ds);
      }
      catalog.finish();
    }
    return catalog;
  }

  private InvCatalogImpl makeCatalogRuns() throws URISyntaxException, IOException {
    boolean changed =  madeFmrc && fmrc.sync();

    if (changed || catalogRuns == null) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      catalogRuns = new InvCatalogImpl( getFullName(), parent.getVersion(), parent.resolveUri(getCatalogHref(RUNS)));
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      //top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local
      top.setName(TITLE_RUNS);
      catalogRuns.addDataset(top);

      // any referenced services need to be local
      ArrayList services = new ArrayList( getServicesLocal());
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        catalogRuns.addService(service);

      List datasets = makeRunDatasets();
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
        top.addDataset(ds);
      }
      catalogRuns.finish();
    }
    return catalogRuns;
  }

  private InvCatalogImpl makeCatalogOffsets() throws URISyntaxException, IOException {
    boolean changed =  madeFmrc && fmrc.sync();

    if (changed || catalogOffsets == null) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      catalogOffsets = new InvCatalogImpl( getFullName(), parent.getVersion(), parent.resolveUri(getCatalogHref(OFFSET)));
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      //top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local

      top.setName(TITLE_OFFSET);
      catalogOffsets.addDataset(top);

      // any referenced services need to be local
      ArrayList services = new ArrayList( getServicesLocal());
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        catalogOffsets.addService(service);

      List datasets = makeOffsetDatasets();
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
        top.addDataset(ds);
      }
      catalogOffsets.finish();
    }
    return catalogOffsets;
  }

  private InvCatalogImpl makeCatalogForecasts() throws URISyntaxException, IOException {
    boolean changed =  madeFmrc && fmrc.sync();

    if (changed || catalogForecasts == null) {
      InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
      catalogForecasts = new InvCatalogImpl( getFullName(), parent.getVersion(), parent.resolveUri(getCatalogHref(FORECAST)));
      InvDatasetImpl top = new InvDatasetImpl(this);
      top.setParent(null);
      // top.transferMetadata( (InvDatasetImpl) this.getParent() ); // make all inherited metadata local
      top.setName(TITLE_FORECAST);
      catalogForecasts.addDataset(top);

      // any referenced services need to be local
      ArrayList services = new ArrayList( getServicesLocal());
      InvService service = getServiceDefault();
      if ((service != null) && !services.contains(service))
        catalogForecasts.addService(service);

      List datasets = makeForecastDatasets();
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
        top.addDataset(ds);
      }
      catalogForecasts.finish();
    }
    return catalogForecasts;
  }

  /** Get Datasets. This triggers a read of the referenced catalog the first time its called.
   */
  public java.util.List getDatasets() {
    if (!madeDatasets) {
      String id = getID();
      if (id == null)
        id = getPath();

      InvDatasetImpl ds = new InvDatasetImpl(this, "Forecast Model Run Collection (2D time coordinates)");
      String name = getName()+"_"+FMRC;
      ds.setUrlPath(path+"/"+name);
      ds.setID(id+"/"+name);
      ThreddsMetadata tm = ds.getLocalMetadata();
      tm.addDocumentation("summary", "Forecast Model Run Collection (2D time coordinates).");
      ds.finish();
      datasets.add( ds);

      ds = new InvDatasetImpl(this, "Best Time Series");
      name = getName()+"_"+BEST;
      ds.setUrlPath(path+"/"+name);
      ds.setID(id+"/"+name);
      tm = ds.getLocalMetadata();
      tm.addDocumentation("summary", "Best time series, taking the data from the most recent run available.");
      ds.finish();
      datasets.add( ds);

      // run datasets as catref
      ds = new InvCatalogRef(this, TITLE_RUNS, getCatalogHref(RUNS));
      ds.finish();
      datasets.add( ds);

      // run datasets as catref
      ds = new InvCatalogRef(this, TITLE_OFFSET, getCatalogHref(OFFSET));
      ds.finish();
      datasets.add( ds);

      // run datasets as catref
      ds = new InvCatalogRef(this, TITLE_FORECAST, getCatalogHref(FORECAST));
      ds.finish();
      datasets.add( ds);

      madeDatasets = true;
    }

    finish();
    return datasets;
  }

  private String getCatalogHref( String what) {
    return "/thredds/catalog/"+path+"/"+what+"/catalog.xml";
  }

  private synchronized void makeFmrc() {

    if (madeFmrc)
      return;

    Element ncml = getNcmlElement();
    try {
      NetcdfDataset ncd = NcMLReader.readNcML(ncml, null);
      fmrc = new FmrcImpl( ncd);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    madeFmrc = true;
  }

  private List makeRunDatasets() {
    makeFmrc();

    ArrayList datasets = new ArrayList();
    DateFormatter formatter = new DateFormatter();

    String id = getID();
    if (id == null)
      id = getPath();

    List runs = fmrc.getRunDates();
    for (int i = 0; i < runs.size(); i++) {
      Date runDate = (Date) runs.get(i);
      //String name = StringUtil.escape(formatter.toDateTimeStringISO( runDate), "");
      String name = getName()+"_"+RUN_NAME+formatter.toDateTimeStringISO( runDate);
      InvDatasetImpl nested = new InvDatasetImpl(this, name);
      nested.setUrlPath(path+"/"+RUNS+"/"+name);
      nested.setID(id+"/"+RUNS+"/"+name);
      ThreddsMetadata tm = nested.getLocalMetadata();
      tm.addDocumentation("summary", "Data from Run "+name);
      datasets.add( nested);
    }

    Collections.reverse( datasets);
    return datasets;
  }

  private List makeOffsetDatasets() {
    makeFmrc();

    ArrayList datasets = new ArrayList();

    String id = getID();
    if (id == null)
      id = getPath();

     List offsets = fmrc.getForecastOffsets();
     for (int i = 0; i < offsets.size(); i++) {
       Double offset = (Double) offsets.get(i);
       String name = getName()+"_"+OFFSET_NAME+offset+"hr";
       InvDatasetImpl nested = new InvDatasetImpl(this, name);
       nested.setUrlPath(path+"/"+OFFSET+"/"+name);
       nested.setID(id+"/"+OFFSET+"/"+name);
       ThreddsMetadata tm = nested.getLocalMetadata();
       tm.addDocumentation("summary", "Data from the "+offset+" hour forecasts, across different model runs.");
       datasets.add( nested);
     }

    return datasets;
  }

  private List makeForecastDatasets() {
    makeFmrc();

    ArrayList datasets = new ArrayList();
    DateFormatter formatter = new DateFormatter();

    String id = getID();
    if (id == null)
      id = getPath();

     List forecasts = fmrc.getForecastDates();
     for (int i = 0; i < forecasts.size(); i++) {
       Date forecastDate = (Date) forecasts.get(i);
       String name = getName()+"_"+FORECAST_NAME+formatter.toDateTimeStringISO( forecastDate);
       InvDatasetImpl nested = new InvDatasetImpl(this, name);
       nested.setUrlPath(path+"/"+FORECAST+"/"+name);
       nested.setID(id+"/"+FORECAST+"/"+name);
       ThreddsMetadata tm = nested.getLocalMetadata();
       tm.addDocumentation("summary", "Data with the same forecast date, "+name+", across different model runs.");
       datasets.add( nested);
     }

    return datasets;
  }


  /////////////////////////////////////////////////////////////////////////
  public NetcdfDataset getDataset(String path) throws IOException {
    makeFmrc();
    NetcdfDataset result = null;
    String name = path;

    if (path.endsWith(FMRC))
      result = fmrc.getFmrcDataset();

    else if (path.endsWith(BEST))
      result = fmrc.getBestTimeSeries();

    else {
      int pos = path.indexOf("/");
      String type = path.substring(0, pos);
      name = path.substring(pos+1);

      if (type.equals(OFFSET)) {
        int pos1 = name.indexOf(OFFSET_NAME);
        int pos2 = name.indexOf("hr");
        if ((pos1<0) || (pos2<0)) return null;
        String id = name.substring(pos1+OFFSET_NAME.length(), pos2);
        double hour = Double.parseDouble(id);
        result = fmrc.getForecastOffsetDataset( hour);

      } else if (type.equals(RUNS)) {
        int pos1 = name.indexOf(RUN_NAME);
        if (pos1<0) return null;
        String id = name.substring(pos1+RUN_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        result = fmrc.getRunTimeDataset(date);

      } else if (type.equals(FORECAST)) {
        int pos1 = name.indexOf(FORECAST_NAME);
        if (pos1<0) return null;
        String id = name.substring(pos1+FORECAST_NAME.length());

        DateFormatter formatter = new DateFormatter();
        Date date = formatter.getISODate(id);
        result = fmrc.getForecastTimeDataset(date);
      }
    }

    if (null != result) result.setLocation( name);
    return result;
  }

}
