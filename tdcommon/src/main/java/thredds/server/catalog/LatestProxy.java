/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

import net.jcip.annotations.Immutable;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import thredds.inventory.MFile;

import java.io.IOException;
import java.util.*;

/**
 * Description
 *
 * @author John
 * @since 1/14/2015
 */
@Immutable
public class LatestProxy {
  private final String latestName;
  private final boolean locateAtTopOrBottom;
  private final Service service;
  private final boolean isResolver;

  private final long lastModifiedLimit;

  /**
   * Constructor.
   * <p/>
   * The latestName is used as the name of latest dataset created. The
   * location for the placement of the latest dataset is given by
   * locateAtTopOrBottom (true - locate on top; false - locate on bottom).
   *
   * @param latestName          the name to be used for all latest dataset, if null, the default is "latest.xml".
   * @param locateAtTopOrBottom indicates where to locate the latest dataset (true - locate on top; false - locate on bottom).
   * @param service             the InvService used by the created dataset.
   * @param lastModifiedLimit   only use datasets whose lastModified() time is at least this many minutes in the past
   */
  public LatestProxy(String latestName,
                     boolean locateAtTopOrBottom,
                     Service service,
                     boolean isResolver,
                     long lastModifiedLimit) {

    this.latestName = latestName;
    this.locateAtTopOrBottom = locateAtTopOrBottom;
    this.service = service;
    this.isResolver = isResolver;
    this.lastModifiedLimit = lastModifiedLimit;
  }

  public String getName() {
    return latestName;
  }

  public boolean isLocateAtTopOrBottom() {
    return locateAtTopOrBottom;
  }

  public String getServiceName() {
    return service.getName();
  }

  public long getLastModifiedLimit() {
    return lastModifiedLimit;
  }

  public String getProxyDatasetName() {
    return latestName;
  }

  public Object getConfigObject() {
    return null;
  }

  public MFile createProxyDataset(MFile parent) {
    return new LatestDataset(parent, this.latestName);
  }

  public Service getProxyDatasetService(MFile parent) {
    return service;
  }

  public int getProxyDatasetLocation(MFile parent, int collectionDatasetSize) {
    if (locateAtTopOrBottom)
      return 0;
    else
      return collectionDatasetSize;
  }

  public boolean isProxyDatasetResolver() {
    return this.isResolver;
  }

  public InvCrawlablePair getActualDataset(List<InvCrawlablePair> atomicDsInfo) {
    if (atomicDsInfo == null || atomicDsInfo.isEmpty())
      return null;

    // Place into temporary list all dataset not modified more recently
    // than lastModifiedLimit before present.
    long targetTime = System.currentTimeMillis() - (this.lastModifiedLimit * 60 * 1000);
    List<InvCrawlablePair> tmpList = new ArrayList<>(atomicDsInfo);
    for (Iterator<InvCrawlablePair> it = tmpList.iterator(); it.hasNext(); ) {
      InvCrawlablePair curDsInfo = it.next();
      MFile curCrDs = curDsInfo.crawlableDataset;
      if (curCrDs != null && curCrDs.getLastModified() > 0 && curCrDs.getLastModified() > targetTime) {
        it.remove();
      }
    }

    // Get the maximum item according to lexigraphic comparison of InvDataset names.

    return Collections.max(tmpList, new Comparator<InvCrawlablePair>() {
      public int compare(InvCrawlablePair dsInfo1, InvCrawlablePair dsInfo2) {
        return (dsInfo1.invDataset.getName().compareTo(dsInfo2.invDataset.getName()));
      }
    });
  }

  public String getActualDatasetName(InvCrawlablePair actualDataset, String baseName) {
    if (baseName == null) baseName = "";
    return baseName.equals("") ? "Latest" : "Latest " + baseName;
  }

  private static class LatestDataset implements MFile {
    private MFile parent;
    private String name;

    LatestDataset(MFile parent, String name) {
      this.parent = parent;
      this.name = name;
    }

    @Override
    public long getLastModified() {
      return 0;
    }

    @Override
    public long getLength() {
      return 0;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public String getPath() {
      return parent.getPath() + "/" + name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public MFile getParent() throws IOException {
      return parent;
    }

    @Override
    public int compareTo(MFile o) {
      return 0;
    }

    @Override
    public Object getAuxInfo() {
      return null;
    }

    @Override
    public void setAuxInfo(Object info) {

    }

  }

  private class InvCrawlablePair {
    MFile crawlableDataset;
    Dataset invDataset;
  }
}
