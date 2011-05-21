/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package thredds.monitor;

import thredds.inventory.bdb.MetadataManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * JMX bean
 *
 * @author caron
 * @since Apr 19, 2010
 */
public class FmrcCacheMonitorImpl implements FmrcCacheMonitor {

  public List<String> getCachedCollections() {
    java.util.List<String> result = new ArrayList<String>();
    for (String name : MetadataManager.getCollectionNames())
      result.add(name);
    return result;
  }

  public String getCacheLocation() {
    return MetadataManager.getCacheLocation();
  }

  public void getCacheStatistics(Formatter f) {
    MetadataManager.showEnvStats(f);
  }

  public List<String> getFilesInCollection(String collectionName) {
    java.util.List<String> result = new ArrayList<String>();
    MetadataManager mm = null;
    try {
      mm = new MetadataManager(collectionName);

      for (MetadataManager.KeyValue data : mm.getContent())
        result.add(data.key);

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (mm != null) mm.close();
    }
    return result;
  }

  public String getCachedFile(String collectionName, String name) {
    MetadataManager mm = null;
    try {
      mm = new MetadataManager(collectionName);
      return mm.get(name);

    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    } finally {
      if (mm != null) mm.close();
    }
  }

  public void deleteCollection(String collectionName) throws Exception {
    MetadataManager.deleteCollection(collectionName);
  }

  public void sync() {
    MetadataManager.sync();
  }


}
