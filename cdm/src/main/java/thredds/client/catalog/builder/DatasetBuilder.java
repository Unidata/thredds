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

import thredds.client.catalog.Dataset;
import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.ThreddsMetadata;

import java.util.*;

/**
 * Builder of client catalog Dataset
 *
 * @author caron
 * @since 1/8/2015
 */
public class DatasetBuilder {

  /**
   * Utility routine to keep list of objects small.
   * add fldValue to the fldName list in flds.
   * fldValue may be a list or an object.
   * if no list, just keep the object without creating a list (common case).
   * otherwise add it to the existing list.
   */
  public static void addToList(Map<String, Object> flds, String fldName, Object fldValue) {
    if (fldValue == null) return;
    Object prevVal = flds.get(fldName);
    if (prevVal == null) {
      flds.put(fldName, fldValue);
      return;
    }

    List prevList;
    if (prevVal instanceof List) {
      prevList = (List) prevVal;
    } else {
      prevList = new ArrayList(5);
      prevList.add(prevVal);
      flds.put(fldName, prevList);
    }

    if (fldValue instanceof List) {
      prevList.addAll((List) fldValue);
    } else {
      prevList.add(fldValue);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////

  protected DatasetBuilder parent;
  protected String name;
  protected Map<String, Object> flds = new HashMap<>(10);

  protected List<AccessBuilder> accessBuilders;
  protected List<DatasetBuilder> datasetBuilders;

  public DatasetBuilder(DatasetBuilder parent) {
    this.parent = parent;
  }

  public DatasetBuilder getParent() {
    return parent;
  }

  public Object get(String fldName) {
    return flds.get(fldName);
  }

  public void put(String fldName, Object fldValue) {
    if (fldValue != null)
      flds.put(fldName, fldValue);
    else
      flds.remove(fldName);
  }

  public void putInheritedField(String fldName, Object fldValue) {
    if (fldValue == null) return;

    ThreddsMetadata tmi = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmi == null) {
      tmi = new ThreddsMetadata();
      put(Dataset.ThreddsMetadataInheritable, tmi);
    }
    tmi.getFlds().put(fldName, fldValue);
  }

  public void addToList(String fldName, Object fldValue) {
    if (fldValue != null) addToList(flds, fldName, fldValue);
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void addDataset(DatasetBuilder d) {
    if (d == null) return;
    if (datasetBuilders == null) datasetBuilders = new ArrayList<>();
    datasetBuilders.add(d);
  }

  public void addAccess(AccessBuilder d) {
    if (accessBuilders == null) accessBuilders = new ArrayList<>();
    accessBuilders.add(d);
  }

  public Dataset makeDataset(DatasetNode parent) {  // LOOK whats relationship of parent with this.parent ??
    ThreddsMetadata tmi = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmi != null) tmi.finish();
    return new Dataset(parent, name, flds, accessBuilders, datasetBuilders);
  }

  // make an immutable copy without changin DatasetBuilder
  public Dataset copyDataset(DatasetNode parent) {
    return new Dataset(parent, name, flds, accessBuilders, datasetBuilders);
  }

  /////////////////////////////////////////////////////////////////////////////

  public void transferMetadata( DatasetNode from, boolean inherit) {
    if (inherit)
      inheritMetadata(flds, from);

    Map<String, Object> fromFlds = from.getFlds();
    for (Map.Entry<String, Object> entry : fromFlds.entrySet()) {
      flds.put(entry.getKey(), entry.getValue());
    }

    // tmi needs to be transferred to mutable version
    ThreddsMetadata tmiOld = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmiOld != null && tmiOld.isImmutable()) {
      ThreddsMetadata tmiNew = new ThreddsMetadata(tmiOld);
      flds.put(Dataset.ThreddsMetadataInheritable, tmiNew);
    }
  }

  private void inheritMetadata( Map<String, Object> flds, DatasetNode from) {

    Dataset fromParent = from.getParentDataset();
    if (fromParent == null) return;
    // depth first, so closer parents override; LOOK need to add to list
    inheritMetadata( flds, fromParent);

    Map<String, Object> fromFlds = fromParent.getFlds();
    for (Map.Entry<String, Object> entry : fromFlds.entrySet()) {
      flds.put(entry.getKey(), entry.getValue());
    }
  }

  public ThreddsMetadata getInheritableMetadata() {
    ThreddsMetadata tmi = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmi == null) {
      tmi = new ThreddsMetadata();
      put(Dataset.ThreddsMetadataInheritable, tmi);
    }
    return tmi;
  }


}
