/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.dt.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Read in all images in a dir and subdirs, and randomly iterate.
 *
 * @author caron
 * @since Oct 9, 2008
 */
public class ImageFactoryRandom {
  private java.util.List<File> holdList;
  private java.util.List<File> fileList;
  private Random random = new Random( System.currentTimeMillis());

  public ImageFactoryRandom(File topDir) {
    if (!topDir.exists())
      return;

    fileList = new ArrayList<File>(1000);
    addToList(topDir, fileList);
    System.out.println("nfiles= "+ fileList.size());
    holdList = new ArrayList( fileList);
  }

  private void addToList(File dir, List<File> list) {
    for (File file : dir.listFiles()) {
      if (file.isDirectory())
        addToList(file, list);
      else if (file.getName().endsWith(".jpg") || file.getName().endsWith(".JPG"))
        list.add(file);
    }
  }

  File nextFile = null;
  public BufferedImage getNextImage() {
    if (holdList.size() == 0)
      holdList = new ArrayList( fileList);

    int next = Math.abs(random.nextInt()) % holdList.size();
    nextFile = holdList.get(next);
    holdList.remove( nextFile); // random draw without replacement

    try {
      System.out.printf("next %d %s %n", next, nextFile);
      return javax.imageio.ImageIO.read(nextFile);
    } catch (IOException e) {
      System.out.println("Failed to open image " + nextFile);
      fileList.remove( nextFile);
      return getNextImage();
    }

  }

  // remove last file
  public void delete() {
    if (nextFile == null) return;
    fileList.remove( nextFile);
    File f = new File("C:/tmp/deleted/"+nextFile.getName());
    nextFile.renameTo(f);

  }


}
