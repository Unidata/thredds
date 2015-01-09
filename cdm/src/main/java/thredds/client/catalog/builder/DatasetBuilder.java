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

import thredds.client.catalog.Access;
import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.Metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 1/8/2015
 */
public class DatasetBuilder {

  DatasetBuilder parent;
  String name;
  String collectionType;
  Boolean harvest;
  String id;
  String urlPath;
  List<Metadata> metadata;
  List<AccessBuilder> access;
  List<DatasetBuilder> datasets;

  public DatasetBuilder(DatasetBuilder parent, String name, String collectionType, Boolean harvest, String id, String urlPath) {
    this.parent = parent;
    this.name = name;
    this.collectionType = collectionType;
    this.harvest = harvest;
    this.id = id;
    this.urlPath = urlPath;
  }

  public void addDataset(DatasetBuilder d) {
    if (d == null) return;
    if (datasets == null) datasets = new ArrayList<>();
    datasets.add(d);
  }


  public void addAccess(AccessBuilder d) {
    if (access == null) access = new ArrayList<>();
    access.add(d);
  }


  public void addMetadata(Metadata d) {
    if (metadata == null) metadata = new ArrayList<>();
    metadata.add(d);
  }

}
