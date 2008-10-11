/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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

  public BufferedImage getNextImage() {
    if (holdList.size() == 0)
      holdList = new ArrayList( fileList);

    int next = Math.abs(random.nextInt()) % holdList.size();
    File nextFile = holdList.get(next);
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


}
