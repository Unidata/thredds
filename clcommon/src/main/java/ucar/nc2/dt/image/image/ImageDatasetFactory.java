/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.image.image;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.ma2.Array;

/**
 * A factory for buffered images
 * @author caron
 */
public class ImageDatasetFactory {

  private StringBuffer log;
  public String getErrorMessages() { return log == null ? "" : log.toString(); }
  private File currentFile, currentDir = null;
  private java.util.List<File> currentDirFileList;
  private int currentDirFileNo = 0;

  // grid stuff
  private GridDatatype grid = null;
  private int time = 0;
  private int ntimes = 1;

  public BufferedImage openDataset( GridDatatype grid) throws java.io.IOException {
    this.grid = grid;
    this.time = 0;
    GridCoordSystem gcsys = grid.getCoordinateSystem();
    if (gcsys.getTimeAxis() != null)
      ntimes = (int) gcsys.getTimeAxis().getSize();
    Array data = grid.readDataSlice( this.time, 0, -1, -1);
    return ImageArrayAdapter.makeGrayscaleImage( data, grid);
  }

  /**
   * Open from a URL:
   *   adde: use  AddeImage.factory()
   *   http: use javax.imageio.ImageIO.read()
   *   file: javax.imageio.ImageIO.read()
   * @param location open from this location
   * @return a BufferedImage
   * @throws java.io.IOException on read error
   */
  public BufferedImage open( String location) throws java.io.IOException {
    log = new StringBuffer();

    if (location.startsWith("http:")) {
      try {
        URL url = new URL(location);
        currentFile = null;
        return javax.imageio.ImageIO.read(url);

      } catch (MalformedURLException e) {
        log.append(e.getMessage());
        //e.printStackTrace();
        return null;
      } catch (IOException e) {
        log.append(e.getMessage());
        //e.printStackTrace();
        return null;
      }
    }

    else  {
      if (location.startsWith("file:)"))
        location = location.substring(5);

      try {
        File f = new File(location);
        if (!f.exists()) {
          return null;
        }

        currentFile = f;
        currentDir = null;
        return javax.imageio.ImageIO.read(f);

      } catch (MalformedURLException e) {
        log.append(e.getMessage());
        //e.printStackTrace();
        return null;
      } catch (IOException e) {
        log.append(e.getMessage());
        //e.printStackTrace();
        return null;
      }
    }
  }

  /**
   * This assumes you have opened a file. looks in the parent directory.
   * @param forward if true got to next, else previous
   * @return  next file in the directory, as a BufferedImage.
   */
  public BufferedImage getNextImage(boolean forward) {
    if (grid != null) {
      if (forward) {
        this.time++;
        if (this.time >= this.ntimes) this.time = 0;
      } else {
        this.time--;
        if (this.time < 0) this.time = this.ntimes-1;
      }

      Array data;
      try {
        data = grid.readDataSlice( this.time, 0, -1, -1);
        return ImageArrayAdapter.makeGrayscaleImage( data, grid);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    if (currentFile == null)
      return null;

    if (currentDir == null) {
      currentDirFileNo = 0;
      currentDir = currentFile.getParentFile();
      currentDirFileList = new ArrayList<>();
      addToList( currentDir, currentDirFileList);
      //Arrays.asList(currentDir.listFiles());
      //Collections.sort(currentDirFileList);
      for (int i = 0; i < currentDirFileList.size(); i++) {
        File file = currentDirFileList.get(i);
        if (file.equals(currentFile))
          currentDirFileNo = i;
      }
    }

    if (forward) {
      currentDirFileNo++;
      if (currentDirFileNo >= currentDirFileList.size())
        currentDirFileNo = 0;
    } else {
      currentDirFileNo--;
      if (currentDirFileNo < 0)
        currentDirFileNo = currentDirFileList.size()-1;
    }

    File nextFile = currentDirFileList.get(currentDirFileNo);
    try {
      System.out.println("Open image "+nextFile);
      return javax.imageio.ImageIO.read(nextFile);
    } catch (IOException e) {
      System.out.println("Failed to open image "+nextFile);
      return getNextImage( forward);
    }

  }

  private void addToList( File dir, List<File> list) {
    for (File file : dir.listFiles()) {
      if (file.isDirectory())
        addToList(file, list);
      else if (file.getName().endsWith(".jpg") || file.getName().endsWith(".JPG"))
        list.add(file);
    }
  }

}
