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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

  public Catalog buildFromLocation(String location) {
    return null;
  }

  public String getErrorMessage() {
    return null;
  }

  public Catalog buildFromCatref(CatalogRef catref) {
     return null;
  }

  public void buildFromCatrefAsynch(CatalogRef catref, CatalogBuilder.Callback call) {
    Catalog cat = buildFromCatref(catref);
    if (cat != null) call.setCatalog(cat);
  }

  ////////////////////////////////////////////////////
  private String name, expires, version;
  private URI baseURI;
  List<Property> properties;
  List<Service> services;
  List<DatasetBuilder> datasets;

  public void setName(String name) {
    this.name = name;
  }

  public void setBaseURI(URI baseURI) {
    this.baseURI = baseURI;
  }

  public void setExpires(String expires) {
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
    if (datasets == null) datasets = new ArrayList<>();
    datasets.add(d);
  }


}
