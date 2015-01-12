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
package thredds.client.catalog.builder;

import thredds.client.catalog.*;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * builds Catalogs
 *
 * @author caron
 * @since 1/8/2015
 */
public class CatalogBuilder {

  public interface Callback {
    void setCatalog(Catalog cat);
  }

  public Catalog buildFromCatref(CatalogRef catref) throws IOException {
    URI catrefURI = catref.getURI();
    if (catrefURI == null) {
      errlog.format("Catref doesnt have valid UrlPath=%s%n", catref.getUrlPath());
      error = true;
      return null;
    }
    Catalog result =  buildFromURI(catrefURI);
    catref.setRead(!error);
    return result;
  }

  //////////////////////////////////////////////////////////////////////////////////

  private Formatter errlog = new Formatter();
  private boolean error = false;

  public Catalog buildFromLocation(String location) throws IOException {
    URI uri;
    try {
      uri = new URI(location);
    } catch (URISyntaxException e) {
      errlog.format("Bad location = %s err=%s%n", location, e.getMessage());
      return null;
    }
    return buildFromURI(uri);
  }

  public Catalog buildFromURI(URI uri) throws IOException {
    JdomCatalogBuilder jdomBuilder = new JdomCatalogBuilder(errlog);
    error = jdomBuilder.readXML(this, uri);
    return makeCatalog();
  }

  public String getErrorMessage() {
    return errlog.toString();
  }

  public boolean hasFatalError() {
    return error;
  }

  ////////////////////////////////////////////////////
  String name, version;
  CalendarDate expires;
  URI baseURI;
  List<Property> properties;
  List<Service> services;
  List<DatasetBuilder> datasetBuilders;

  public void setName(String name) {
    this.name = name;
  }

  public void setBaseURI(URI baseURI) {
    this.baseURI = baseURI;
  }

  public void setExpires(CalendarDate expires) {
    this.expires = expires;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void addProperty(Property p) {
    if (properties == null) properties = new ArrayList<>();
    properties.add(p);
  }

  public void addService(Service s) {
    if (services == null) services = new ArrayList<>();
    services.add(s);
  }

  public void addDataset(DatasetBuilder d) {
    if (datasetBuilders == null) datasetBuilders = new ArrayList<>();
    datasetBuilders.add(d);
  }

  Catalog makeCatalog() {
    Map<String, Object> flds = new HashMap<>(10);

    if (expires != null) flds.put(Dataset.Expires, expires);
    if (version != null) flds.put(Dataset.Version, version);
    if (services != null) flds.put(Dataset.Services, services);
    if (properties != null) flds.put(Dataset.Properties, properties);

    return new Catalog(baseURI, name, flds, datasetBuilders);

  }


}
