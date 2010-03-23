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

import net.jcip.annotations.ThreadSafe;
import thredds.inventory.MCollection;
import thredds.inventory.*;

import java.util.*;
import java.io.File;

/**
 * Inventory Management Controller directly reading OS Files, no caching. recheck is ignored (always true)
 *
 * @author caron
 * @since Jun 25, 2009
 */

@ThreadSafe
public class ControllerOS implements MController {

  ////////////////////////////////////////

  public ControllerOS() {
  }

  @Override
  public Iterator<MFile> getInventory(MCollection mc) {
    return getInventory(mc, true);
  }

  @Override
  public Iterator<MFile> getInventoryNoSubdirs(MCollection mc) {
    return getInventoryNoSubdirs(mc, true);
  }

  @Override
  public Iterator<MFile> getInventory(MCollection mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith("file:")) {
      path = path.substring(5);
    }

    File cd = new File(path);
    if (!cd.exists()) return null;
    if (!cd.isDirectory()) return null;
    return new FilteredIterator(mc, new MFileIteratorWithSubdirs(cd));  }

  @Override
  public Iterator<MFile> getInventoryNoSubdirs(MCollection mc, boolean recheck) {
    String path = mc.getDirectoryName();
    if (path.startsWith("file:")) {
      path = path.substring(5);
    }

    File cd = new File(path);
    if (!cd.exists()) return null;
    if (!cd.isDirectory()) return null;
    return new FilteredIterator(mc, new MFileIterator(cd));
  }

  public void close() {
  } // NOOP


  ////////////////////////////////////////////////////////////

  // handles filtering and removing subdirectories
  private class FilteredIterator implements Iterator<MFile> {
    private Iterator<MFile> orgIter;
    private MCollection mc;

    private MFile next;

    FilteredIterator(MCollection mc, Iterator<MFile> iter) {
      this.orgIter = iter;
      this.mc = mc;
    }

    public boolean hasNext() {
      next = nextFilteredFile();
      return (next != null);
    }

    public MFile next() {
      return next;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private MFile nextFilteredFile() {
      if (orgIter == null) return null;
      if (!orgIter.hasNext()) return null;

      MFile pdata = orgIter.next();
      while (pdata.isDirectory() || !mc.accept(pdata)) {  // skip directories, and filter
        if (!orgIter.hasNext()) return null;
        pdata = orgIter.next();
      }
      return pdata;
    }
  }

  // returns everything in the directory
  private class MFileIterator implements Iterator<MFile> {
    List<File> files;
    int count = 0;

    MFileIterator(File dir) {
      files = Arrays.asList(dir.listFiles());
    }

    MFileIterator(List<File> files) {
      this.files = files;
    }

    public boolean hasNext() {
      return count < files.size();
    }

    public MFile next() {
      File cfile = files.get(count++);
      return new MFileOS(cfile);
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  // recursively scans everything in the directory and in subdirectories, depth first, leaves before subdirs
  private class MFileIteratorWithSubdirs implements Iterator<MFile> {
    Queue<Traversal> traverse;
    Traversal currTraversal;
    Iterator<MFile> currIter;

    MFileIteratorWithSubdirs(File top) {
      traverse = new LinkedList<Traversal>();
      currTraversal = new Traversal(top);
    }

    public boolean hasNext() {
      if (currIter == null) {
        currIter = getNextIterator();
        if (currIter == null) {
          return false;
        }
      }

      if (!currIter.hasNext()) {
        currIter = getNextIterator();
        return hasNext();
      }

      return true;
    }

    public MFile next() {
      return currIter.next();
    }

    private Iterator<MFile> getNextIterator() {

      if (!currTraversal.leavesAreDone) {
        currTraversal.leavesAreDone = true;
        return new MFileIterator(currTraversal.fileList); // look for leaves in the current directory

      } else {
        if ((currTraversal.subdirIterator != null) && currTraversal.subdirIterator.hasNext()) { // has subdirs
          File nextDir = currTraversal.subdirIterator.next();

          traverse.add(currTraversal); // keep track of current traversal
          currTraversal = new Traversal(nextDir);
          return getNextIterator();

        } else {
          if (traverse.peek() == null) return null;
          currTraversal = traverse.remove();
          return getNextIterator();
        }
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

   private class Traversal {
    File dir;
    List<File> fileList;
    Iterator<File> subdirIterator;
    boolean leavesAreDone = false;

    Traversal(File dir) {
      this.dir = dir;

      fileList = new ArrayList<File>();
      List<File> subdirList = new ArrayList<File>();
      for (File f : dir.listFiles()) {
        if (f.isDirectory())
          subdirList.add(f);
        else
          fileList.add(f);
      }

      if (subdirList.size() > 0)
        this.subdirIterator = subdirList.iterator();
    }
  }

}
