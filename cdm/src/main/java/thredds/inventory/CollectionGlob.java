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
 * A MCollection defined by a glob filter.  Experimental.
 *
 * From http://blog.eyallupu.com/2011/11/java-7-working-with-directories.html
 *
 * The 'glob' Syntax
 The glob (stands for globbing) syntax is a 'simplified' form of regular expressions with awareness to path
 components (directories), the syntax is composed of the following syntactic tokens:
  1) The '*' character matches zero or more characters from the path elements without crossing directory boundaries
   (unlike regular expression this is not a Kleene star and it has nothing to do with the preceding part of the expression)
 2) The '**' characters match zero or more characters crossing directory boundaries
 3) The '?' character matches exactly one character of a name component
 4) '[' and ']' can be used to match a single character in the path name from a set of characters
    '-' (hyphen) can be used to specify a range of characters. If hyphen has to be included in the characters set it must be the first in the set
    '!' as the first character in the set can be used as a negation expression
 5) '{' and '}' can group sub patterns, the group matches if any of the sub patterns matches (comma is used to separate between the groups)
 6 ) The dot '.' character represents a dot (unlike regular expressions in which a dot is a replacement for any character)
 7) and finally: special characters escaping is done using backslash

 The '**' expression is the only one to cross directory boundaries, all other expressions are bound within a single element
  (either a directory or a filename), below is a sample usage of PathMatcher followed by few pattern examples:

 *
 *
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

  // from http://blog.eyallupu.com/2011/11/java-7-working-with-directories.html
  public static DirectoryStream newDirectoryStream(Path dir, String glob) throws IOException {
    FileSystem fs = dir.getFileSystem();
    final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
    DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
        public boolean accept(Path entry)  {
            return matcher.matches(entry.getFileName());
        }
    };
    return fs.provider().newDirectoryStream(dir, filter);
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
            if (subdirs.isEmpty()) {
              nextMFile = null;
              return false;
            }
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
      if (nextMFile == null) throw new NoSuchElementException();
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
