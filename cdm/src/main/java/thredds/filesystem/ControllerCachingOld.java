/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.filesystem;

import thredds.inventory.MCollection;
import thredds.inventory.*;

import java.util.*;

/**
 * Inventory Management Controller that caches OS Files
 *
 * @author caron
 * @since Jun 25, 2009
 */


public class ControllerCachingOld {

  ////////////////////////////////////////
  private CacheManager cacheManager;
  private Map<String, MCollection> map = new HashMap<String, thredds.inventory.MCollection>();

  public ControllerCachingOld(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  public void addCollection(MCollection mc) {
    map.put(mc.getName(), mc);
  }

  public Iterator<MFile> getInventory(String collectionName) {
    MCollection mc = map.get(collectionName);
    if (mc == null) return null;
    return getInventory(mc);
  }

  public Iterator<MFile> getInventory(MCollection mc) {
    String path = mc.getDirectoryName();
    if ( path.startsWith( "file:" ) ) {
      path = path.substring(5);
    }

    CacheDirectory cd = cacheManager.get(path, true); // check in cache, else call File.listFiles()
    if (cd == null) return null;
    return new FilteredIterator(mc, cd);
  }

  public void close() {
    cacheManager.close();
    cacheManager = null;
  }


  ////////////////////////////////////////////////////////////

  private class FilteredIterator implements Iterator<MFile> {
    private Iterator<MFile> orgIter;
    private MCollection mc;

    private MFile next;

    FilteredIterator(MCollection mc, CacheDirectory cd) {
      this.orgIter = new MFileIterator(cd);
      this.mc = mc;
    }

    public boolean hasNext() {
      next = nextFilteredDataPoint();
      return (next != null);
    }

    public MFile next() {
      return next;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private MFile nextFilteredDataPoint() {
      if (orgIter == null) return null;
      if (!orgIter.hasNext()) return null;

      MFile pdata = orgIter.next();
      while (!mc.accept(pdata)) {
        if (!orgIter.hasNext()) return null;
        pdata = orgIter.next();
      }
      return pdata;
    }
  }

  private class MFileIterator implements Iterator<MFile> {
    CacheDirectory cd;
    CacheFile[] files;
    int count = 0;

    MFileIterator(CacheDirectory cd) {
      this.cd = cd;
      files = cd.getChildren();
    }

    public boolean hasNext() {
      return count < files.length;
    }

    public MFile next() {
      CacheFile cfile = files[count++];
      return new MFileCached(cd.getPath(), cfile);
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
