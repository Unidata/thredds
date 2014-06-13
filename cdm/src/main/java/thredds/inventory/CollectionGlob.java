/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds.inventory;

import org.slf4j.Logger;
import thredds.filesystem.MFileOS;
import thredds.filesystem.MFileOS7;
import ucar.nc2.util.CloseableIterator;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 5/19/14
 */
public class CollectionGlob extends CollectionAbstract {
  PathMatcher matcher;
  boolean debug = false;
  int depth = 0;

  public CollectionGlob(String collectionName, String glob, Logger logger) {
    super(collectionName, logger);

    matcher = FileSystems.getDefault().getPathMatcher("glob:"+glob);
    System.out.printf("glob=%s%n", glob);

    // lets suppose the first "*" indicates the top dir
    int pos = glob.indexOf("*");
    this.root = glob.substring(0, pos-1);
    String match = glob.substring(pos);

    // count how far to recurse. LAME!!! why doesnt java provide the right thing !!!!
    pos = glob.indexOf("**");
    if (pos > 0)
      depth = Integer.MAX_VALUE;
    else {
      // count the "/" !!
      for (char c : match.toCharArray())
        if (c == '/') depth++;
    }

    if (debug) System.out.printf(" CollectionGlob.MFileIterator topPath='%s' depth=%d%n", this.root, this.depth);

  }

  @Override
  public void close() {

  }

  @Override
  public Iterable<MFile> getFilesSorted() throws IOException {
    return makeFileListSorted();
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MyFileIterator(this.root);
  }

  private class MyFileIterator implements CloseableIterator<MFile> {
    DirectoryStream<Path> dirStream;
    Iterator<Path> dirStreamIterator;
    MFile nextMFile;
    int count = 0, total = 0;
    Stack<Path> subdirs = new Stack<>();
    int currDepth = 0;

    MyFileIterator(String topDir) throws IOException {
      Path topPath = Paths.get(topDir);
      dirStream = Files.newDirectoryStream(topPath);
      dirStreamIterator = dirStream.iterator();
    }

    public boolean hasNext() {
      while (true) {

        try {
               // if (debug && count % 100 == 0) System.out.printf("%d ", count);
          while (!dirStreamIterator.hasNext()) {
            dirStream.close();
            if (subdirs.isEmpty()) return false;
            currDepth++;                             // LOOK wrong
            Path nextSubdir = subdirs.pop();
            dirStream = Files.newDirectoryStream(nextSubdir);
            dirStreamIterator = dirStream.iterator();
          }

          total++;
          Path nextPath = dirStreamIterator.next();
          BasicFileAttributes attr = Files.readAttributes(nextPath, BasicFileAttributes.class);
          if (attr.isDirectory()) {
            if (currDepth < depth) subdirs.push(nextPath);
            continue;
          }

          if (!matcher.matches(nextPath)) {
            // if (debug) System.out.printf(" SKIP %s%n ", nextPath);
            continue;
          }

          nextMFile = new MFileOS7(nextPath, attr);
          return true;
          // if (debug) System.out.printf("  OK  %s%n ", nextMFile);

       } catch (IOException e) {
         throw new RuntimeException(e);
       }
       // if (filter == null || filter.accept(nextMFile)) return true;
      }
    }

    public MFile next() {
      count++;
      return nextMFile;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    // better alternative is for caller to send in callback (Visitor pattern)
    // then we could use the try-with-resource
    public void close() throws IOException {
      if (debug) System.out.printf("  OK=%d total=%d%n ", count, total);
      dirStream.close();
    }
  }
}
