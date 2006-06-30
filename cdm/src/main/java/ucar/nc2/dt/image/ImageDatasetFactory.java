package ucar.nc2.dt.image;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.File;
import java.util.List;

import thredds.catalog.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.ma2.Array;

/**
 * Created by IntelliJ IDEA.
 * User: john
 * Date: Aug 23, 2004
 * Time: 5:18:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageDatasetFactory {

  private StringBuffer log;
  public String getErrorMessages() { return log == null ? "" : log.toString(); }
  private File currentFile, currentDir = null;
  private File[] currentDirFiles = null;
  private int currentDirFileNo = 0;

  // grid stuff
  private GeoGrid grid = null;
  private int time = 0;
  private int ntimes = 1;

  public BufferedImage openDataset( GeoGrid grid) throws java.io.IOException {
    this.grid = grid;
    this.time = 0;
    GridCoordSys gcsys = grid.getCoordinateSystem();
    if (gcsys.hasTimeAxis())
      ntimes = (int) gcsys.getTimeAxis().getSize();
    Array data = grid.readDataSlice( this.time, 0, -1, -1);
    return ImageArrayAdapter.makeGrayscaleImage( data);
  }

  /**
   * Open from a URL:
   *   adde: use  AddeImage.factory()
   *   http: use javax.imageio.ImageIO.read()
   *   file: javax.imageio.ImageIO.read()
   * @param location
   * @return
   * @throws java.io.IOException
   */
  public BufferedImage open( String location) throws java.io.IOException {
    log = new StringBuffer();

    if (location.startsWith("adde:")) {

      try {
        ucar.nc2.adde.AddeImage addeImage = ucar.nc2.adde.AddeImage.factory( location);
        currentFile = null;
        return addeImage.getImage();

      } catch (Exception e) {
        log.append(e.getMessage());
        // e.printStackTrace();
        return null;
      }

    }

    else if (location.startsWith("http:")) {
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
   * @param forward
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

      Array data = null;
      try {
        data = grid.readDataSlice( this.time, 0, -1, -1);
        return ImageArrayAdapter.makeGrayscaleImage( data);
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
      currentDirFiles = currentDir.listFiles();
      for (int i = 0; i < currentDirFiles.length; i++) {
        File file = currentDirFiles[i];
        if (file.equals(currentFile))
          currentDirFileNo = i;
      }
    }

    if (forward) {
      currentDirFileNo++;
      if (currentDirFileNo >= currentDirFiles.length)
        currentDirFileNo = 0;
    } else {
      currentDirFileNo--;
      if (currentDirFileNo < 0)
        currentDirFileNo = currentDirFiles.length-1;
    }

    File nextFile = currentDirFiles[currentDirFileNo];
    try {
      return javax.imageio.ImageIO.read(nextFile);
    } catch (IOException e) {
      System.out.println("Failed to open image "+nextFile);
      return getNextImage( forward);
    }

  }

}
