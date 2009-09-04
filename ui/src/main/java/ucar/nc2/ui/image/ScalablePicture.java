/*
Copyright (C) 2002  Richard Eigenmann.
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or any later version. This program is distributed
in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
more details. You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
The license is in gpl.txt.
See http://www.gnu.org/copyleft/gpl.html for the details.
*/
package ucar.nc2.ui.image;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.imageio.plugins.jpeg.*;

import java.io.*;
import java.net.*;
import java.util.Vector;
import java.util.Enumeration;

/*
ScalablePicture.java:  class that can load and save images

Copyright (C) 2002  Richard Eigenmann.
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or any later version. This program is distributed 
in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE.  See the GNU General Public License for 
more details. You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
The license is in gpl.txt.
See http://www.gnu.org/copyleft/gpl.html for the details.
*/


/**
 * a class to load and scale an image either immediately or in a seperate thread.
 */

public class ScalablePicture implements SourcePictureListener {
  /**
   * the source picture for the scalable picture
   */
  public SourcePicture sourcePicture;// = new SourcePicture();

  /**
   * The scaled version of the image
   */
  public BufferedImage scaledPicture = null;

  /**
   * The scaling factor
   */
  private double ScaleFactor;

  /**
   * the URL of the picture
   */
  public URL imageUrl = null;

  /**
   * variable to track the status of the picture
   */
  private int pictureStatusCode;


  /**
   * variable to compose te status message
   */
  private String pictureStatusMessage;

  /**
   * if true means that the image should be scaled so that it fits inside
   * a given dimension (TargetSize). If false the ScaleFactor should be used.
   */
  private boolean scaleToSize;

  /**
   * variable to record the size of the box that the scaled image must fit into.
   */
  private Dimension TargetSize;

  /**
   * status code used to signal that the picture is not loaded
   */
  public static final int UNINITIALISED = SourcePicture.LOADING_COMPLETED + 1;

  /**
   * status code used to signal that the picture is cleaning up memory
   */
  public static final int GARBAGE_COLLECTION = UNINITIALISED + 1;

  /**
   * status code used to signal that the thread is loading the image
   */
  public static final int LOADING = GARBAGE_COLLECTION + 1;

  /**
   * status code used to signal that the thread has finished loading the image
   */
  public static final int LOADED = LOADING + 1;


  /**
   * status code used to signal that the thread has loaded the tread is scaling the image
   */
  public static final int SCALING = LOADED + 1;

  /**
   * status code used to signal that the image is available.
   */
  public static final int READY = SCALING + 1;

  /**
   * status code used to signal that there was an error
   */
  public static final int ERROR = READY + 1;

  /**
   * thingy to scale the image
   */
  private AffineTransformOp op;

  /**
   * the object to notify when the image operation changes the status.
   */
  private Vector scalablePictureStatusListeners = new Vector();


  /**
   * the quality with which the JPG pictures shall be written. 0 means poor 1 means great.
   */
  public float jpgQuality = 0.8f;


  /**
   * which method to use on scaling, a fast one or a good quality one
   */
  public boolean fastScale = true;


  /**
   * Rendering Hints object for good quality.
   */
  private static RenderingHints rh_quality = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

  /**
   * Rendering Hints object for using the bicubic method.
   */
  private static RenderingHints rh_bicubic = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);


  /**
   * flag that indicates that the image should be scaled after
   * a status message is received from the SourcePicture that the
   * picture was loaded.
   */
  public boolean scaleAfterLoad = false;


  /**
   * Constructor
   */
  public ScalablePicture() {
    setStatus(UNINITIALISED, "Creating uninitialised ScalablePicture object.");
    setScaleFactor((double) 1);
  }


  /**
   * method to invoke with a filename or URL of a picture that is to be loaded and scaled in
   * a new thread. This is handy to update the screen while the loading chuggs along in the background.
   * Make sure you invoked setScaleFactor or setScaleSize before
   * invoking this method.
   * <p/>
   * Step 1: Am I already loading what I need somewhere?
   * If yes -> use it.
   * Has it finished loading?
   * If no -> wait for it
   * If yes -> use it
   * Else -> load it
   *
   * @param priority The Thread priority
   * @param  imageUrl  The URL of the image you want to load
   * @param  rotation  The rotation 0-360 that the image should be put through
   * after loading.
   */
  public void loadAndScalePictureInThread(URL imageUrl, int priority, double rotation) {
    this.imageUrl = imageUrl;

    boolean alreadyLoading = false;
    Tools.log("ScalablePicture.loadAndScalePictureInThread: checking if picture " + imageUrl + " is already being loaded.");
    if ((sourcePicture != null) && (sourcePicture.getUrl().equals(imageUrl))) {
      Tools.log("ScalablePicture.loadAndScalePictureInThread: the SourcePicture is already loading the sourcePictureimage");
      alreadyLoading = true;
    } else if (PictureCache.isInCache(imageUrl)) {
      // in case the old image has a listener connected remove it
      //  fist time round the sourcePicture is still null therefore the if.
      if (sourcePicture != null) sourcePicture.removeListener(this);

      sourcePicture = PictureCache.getSourcePicture(imageUrl);
      String status = sourcePicture.getStatusMessage();
      if (status == null) status = "";
      Tools.log("ScalablePicture.loadAndScalePictureInThread: Picture in cache! Status: " + status);

      if (sourcePicture.getRotation() == rotation) {
        alreadyLoading = true;
        Tools.log("ScalablePicture.loadAndScalePictureInThread: Picture was even rotated to the correct angle!");
      } else {
        alreadyLoading = false;
        Tools.log("ScalablePicture.loadAndScalePictureInThread: Picture was in cache but with wrong rotation. Forcing reload.");
      }
    }


    if (alreadyLoading) {
      switch (sourcePicture.getStatusCode()) {
        case SourcePicture.UNINITIALISED:
          alreadyLoading = false;
          Tools.log("ScalablePicture.loadAndScalePictureInThread: pictureStatus was: UNINITIALISED");
          break;
        case SourcePicture.ERROR:
          alreadyLoading = false;
          Tools.log("ScalablePicture.loadAndScalePictureInThread: pictureStatus was: ERROR");
          break;
        case SourcePicture.LOADING:
          Tools.log("ScalablePicture.loadAndScalePictureInThread: pictureStatus was: LOADING");
          sourcePicture.addListener(this);
          setStatus(LOADING, "Loading: " + imageUrl.toString());
          sourceLoadProgressNotification(SourcePicture.LOADING_PROGRESS, sourcePicture.getPercentLoaded());
          scaleAfterLoad = true;
          break;
        case SourcePicture.ROTATING:
          Tools.log("ScalablePicture.loadAndScalePictureInThread: pictureStatus was: ROTATING");
          setStatus(LOADING, "Rotating: " + imageUrl.toString());
          sourceLoadProgressNotification(SourcePicture.LOADING_PROGRESS, sourcePicture.getPercentLoaded());
          scaleAfterLoad = true;
          break;
        case SourcePicture.READY:
          Tools.log("ScalablePicture.loadAndScalePictureInThread: pictureStatus was: READY. Sending SCALING status.");
          setStatus(SCALING, "Scaling: " + imageUrl.toString());
          createScaledPictureInThread(priority);
          break;
        default:
          Tools.log("ScalablePicture.loadAndScalePictureInThread: Don't know what status this is:" + Integer.toString(sourcePicture.getStatusCode()));
          break;

      }
    }

    // if the image is not already there then load it.
    if (!alreadyLoading) {
      if (sourcePicture != null) sourcePicture.removeListener(this);
      sourcePicture = new SourcePicture();
      sourcePicture.addListener(this);
      setStatus(LOADING, "Loading: " + imageUrl.toString());
      scaleAfterLoad = true;
      sourcePicture.loadPictureInThread(imageUrl, priority, rotation);
      // when the thread is done it sends a sourceStatusChange message to us
    }
  }


  /**
   * Synchroneous method to load the image.
   * It should only be called by something which is a thread itself such as the HtmlDistillerThread.
   * Since this intended for large batch operations this bypasses the cache.
   *
   * @param imageUrl The Url of the image to be loaded
   * @param rotation The angle by which it is to be roated upon loading.
   */
  public void loadPictureImd(URL imageUrl, double rotation) {
    Tools.log("loadPictureImd invoked with URL: " + imageUrl.toString());
    if (sourcePicture != null) sourcePicture.removeListener(this);
    sourcePicture = new SourcePicture();
    sourcePicture.addListener(this);
    setStatus(LOADING, "Loading: " + imageUrl.toString());
    scaleAfterLoad = true;
    sourcePicture.loadPicture(imageUrl, rotation);
  }


  /**
   * stops all picture loading except if the Url we desire is being loaded
   *
   * @param url The URL of the image which is to be loaded.
   */
  public void stopLoadingExcept(URL url) {
    if (sourcePicture != null) {
      boolean isCurrentlyLoading = sourcePicture.stopLoadingExcept(url);
      if (!isCurrentlyLoading) {
        // sourcePicture.removeListener( this );
      }
      PictureCache.stopBackgroundLoadingExcept(url);
    }
  }


  /**
   * method that is invoked by the SourcePictureListener interface. Usually this
   * will be called by the SourcePicture telling the ScalablePicture that
   * it has completed loading. The ScalablePicture should then change it's own
   * status and tell the ScalableListeners what's up.
   */

  public void sourceStatusChange(int statusCode, String statusMessage, SourcePicture sp) {
    //Tools.log("ScalablePicture.sourceStatusChange: status received from SourceImage: " + statusMessage);

    switch (statusCode) {
      case SourcePicture.UNINITIALISED:
        Tools.log("ScalablePicture.sourceStatusChange: pictureStatus was: UNINITIALISED message: " + statusMessage);
        setStatus(UNINITIALISED, statusMessage);
        break;
      case SourcePicture.ERROR:
        Tools.log("ScalablePicture.sourceStatusChange: pictureStatus was: ERROR message: " + statusMessage);
        setStatus(ERROR, statusMessage);
        sourcePicture.removeListener(this);
        break;
      case SourcePicture.LOADING:
        Tools.log("ScalablePicture.sourceStatusChange: pictureStatus was: LOADING message: " + statusMessage);
        setStatus(LOADING, statusMessage);
        break;
      case SourcePicture.ROTATING:
        Tools.log("ScalablePicture.sourceStatusChange: pictureStatus was: ROTATING message: " + statusMessage);
        setStatus(LOADING, statusMessage);
        break;
      case SourcePicture.READY:
        Tools.log("ScalablePicture.sourceStatusChange: pictureStatus was: READY message: " + statusMessage);
        setStatus(LOADED, statusMessage);
        sourcePicture.removeListener(this);
        if (scaleAfterLoad) {
          createScaledPictureInThread(Thread.MAX_PRIORITY);
          scaleAfterLoad = false;
        }
        break;
      default:
        Tools.log("ScalablePicture.sourceStatusChange: Don't recognize this status: " + statusMessage);
        break;

    }
  }

  /**
   * pass on the update on the loading Progress to the listening objects
   */
  public void sourceLoadProgressNotification(int statusCode, int percentage) {
    Enumeration e = scalablePictureStatusListeners.elements();
    while (e.hasMoreElements()) {
      ((ScalablePictureListener) e.nextElement())
          .sourceLoadProgressNotification(statusCode, percentage);
    }

  }

  /**
   * method that creates the scaled image in the background in it's own thread.
   *
   * @param priority The priority this image takes relative to the others.
   */
  public void createScaledPictureInThread(int priority) {
    setStatus(SCALING, "Scaling picture.");
    ScaleThread t = new ScaleThread(this);
    t.setPriority(priority);
    t.start();
  }

  /**
   * method that is called to create a scaled version of the image.
   */
  public void scalePicture() {
    Tools.log("ScalablePicture.scalePicture invoked");
    try {
      setStatus(SCALING, "Scaling picture.");

      if ((sourcePicture != null) && (sourcePicture.getSourceBufferedImage() != null)) {
        if (scaleToSize) {
          int WindowWidth = TargetSize.width;
          int WindowHeight = TargetSize.height;
          Tools.log("ScalablePicture.scalePicture: scaleToSize: Windowsize: "
              + Integer.toString(WindowWidth) + "x"
              + Integer.toString(WindowHeight));


          int PictureWidth = sourcePicture.getWidth();
          int PictureHeight = sourcePicture.getHeight();

          // Scale so that the entire picture fits in the component.
          if ((WindowWidth == 0) || (WindowHeight == 0)) { // jc added: window not ready
            ScaleFactor = 1.0;
          } else if (((double) PictureHeight / WindowHeight) > ((double) PictureWidth / WindowWidth)) {
            // Vertical scaling
            ScaleFactor = ((double) WindowHeight / PictureHeight);
          } else {
            // Horizontal scaling
            ScaleFactor = ((double) WindowWidth / PictureWidth);
          }

          //				if ( Settings.dontEnlargeSmallImages && ScaleFactor > 1 )
          //					ScaleFactor = 1;
        }

        Tools.log("ScalablePicture.scalePicture: doing an AffineTransform with Factor: " + Double.toString(ScaleFactor));
        AffineTransform af = AffineTransform.getScaleInstance((double) ScaleFactor, (double) ScaleFactor);
        if (fastScale)
          op = new AffineTransformOp(af, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        else
          op = new AffineTransformOp(af, AffineTransformOp.TYPE_BILINEAR);
        scaledPicture = op.filter(sourcePicture.getSourceBufferedImage(), null);

        int PictureWidth = scaledPicture.getWidth();
        int PictureHeight = scaledPicture.getHeight();

        setStatus(READY, "Scaled Picture is ready.");
      } else {
        if (getStatusCode() == LOADING) {
          Tools.log("ScalablePicture.scalePicture invoked while image is still loading. I wonder why?");
          return;
        } else {
          setStatus(ERROR, "Could not scale image as SourceImage is null.");
        }
      }
    } catch (OutOfMemoryError e) {
      Tools.log("ScalablePicture.scalePicture caught an OutOfMemoryError while scaling an image.\n" + e.getMessage());

      setStatus(ERROR, "Out of Memory Error while scaling " + imageUrl.toString());
      scaledPicture = null;
      PictureCache.clear();

      JOptionPane.showMessageDialog(null, // Settings.anchorFrame,
          "outOfMemoryError",
          "genericError",
          JOptionPane.ERROR_MESSAGE);

      System.gc();
      System.runFinalization();

      Tools.log("ScalablePicture.scalePicture: JPO has now run a garbage collection and finalization.");
    } catch (Exception e) {
      System.out.println("Exception in ScalablePicture.scalePicture"+e);
    }
  }


  /**
   * set the scale factor to the new desired value. The scale factor is a multiplier by which the original picture
   * needs to be multiplied to get the size of the picture on the screen. You must
   * call {@link #createScaledPictureInThread(int)} to
   * make anything happen.<p>
   * <p/>
   * Example: Original is 3000 x 2000 --> Scale Factor 0.10  --> Target Picutre is 300 x 200
   */
  public void setScaleFactor(double newFactor) {
    scaleToSize = false;
    TargetSize = null;
    ScaleFactor = newFactor;
  }


  /**
   * invoke this method to tell the scale process to figure out the scal factor
   * so that the image fits either by height or by width into the indicated dimension.
   */
  public void setScaleSize(Dimension newSize) {
    scaleToSize = true;
    TargetSize = newSize;
  }


  /**
   * return the current scale factor
   */
  public double getScaleFactor() {
    return ScaleFactor;
  }


  /**
   * return the current scale size. This is the area that the picture
   * ist fitted into. Since the are could be wider or taller than the picture
   * will be scaled to there is a different mehtod <code>getScaledSize</code>
   * that will return the size of the picture.
   */
  public Dimension getScaleSize() {
    return TargetSize;
  }


  /**
   * return the scaled image
   */
  public BufferedImage getScaledPicture() {
    return scaledPicture;
  }


  /**
   * return the size of the scaled image or Zero if there is none
   */
  public Dimension getScaledSize() {
    if (scaledPicture != null)
      return new Dimension(scaledPicture.getWidth(), scaledPicture.getHeight());
    else
      return new Dimension(0, 0);
  }


  /**
   * return the size of the scaled image as a neatly formatted text or Zero if there is none
   */
  public String getScaledSizeString() {
    if (scaledPicture != null)
      return Integer.toString(scaledPicture.getWidth())
          + " x "
          + Integer.toString(scaledPicture.getHeight());
    else
      return "0 x 0";
  }


  /**
   * return the height of the scaled image or Zero if there is none
   */
  public int getScaledHeight() {
    if (scaledPicture != null)
      return scaledPicture.getHeight();
    else
      return 0;
  }


  /**
   * return the width of the scaled image or Zero if there is none
   */
  public int getScaledWidth() {
    if (scaledPicture != null)
      return scaledPicture.getWidth();
    else
      return 0;
  }


  /**
   * return the image in the original size
   */
  public BufferedImage getOriginalImage() {
    return sourcePicture.getSourceBufferedImage();
  }


  /**
   * return the image in the original size
   */
  public SourcePicture getSourcePicture() {
    return sourcePicture.getSourcePicture();
  }

  public SourcePicture setSourcePicture(SourcePicture source) {
    return this.sourcePicture = source;
  }


  /**
   * return the size of the original image or Zero if there is none
   */
  public Dimension getOriginalSize() {
    return sourcePicture.getSize();
  }


  /**
   * return the height of the original image or Zero if there is none
   */
  public int getOriginalHeight() {
    return sourcePicture.getHeight();
  }


  /**
   * return the width of the original image or Zero if there is none
   */
  public int getOriginalWidth() {
    return sourcePicture.getWidth();
  }


  /**
   * return the filename of the original image
   */
  public String getFilename() {
    return imageUrl.toString();
  }


  /**
   * This method allows the ScalablePicture's scaled BufferedImage to be written
   * to the desired file.
   *
   * @param  writeFile  The File that shall receive the jpg data
   */
  public void writeScaledJpg(File writeFile) {
    writeJpg(writeFile, scaledPicture, jpgQuality);
  }


  /**
   * This static method writes the indicated renderedImage (BufferedImage)
   * to the indicated file.
   *
   * @param  writeFile  The File that shall receive the jpg data
   * @param  renderedImage  The RenderedImage (BufferedImage) to be written
   * @param  jpgQuality  The quality with which to compress to jpg
   */
  public static void writeJpg(File writeFile, RenderedImage renderedImage, float jpgQuality) {
    Iterator writers = ImageIO.getImageWritersByFormatName("jpg");
    ImageWriter writer = (ImageWriter) writers.next();
    JPEGImageWriteParam params = new JPEGImageWriteParam(null);
    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    params.setCompressionQuality(jpgQuality);
    params.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
    params.setDestinationType(new ImageTypeSpecifier(java.awt.image.IndexColorModel.getRGBdefault(),
        IndexColorModel.getRGBdefault().createCompatibleSampleModel(16, 16)));

    try {
      ImageOutputStream ios = ImageIO.createImageOutputStream(new FileOutputStream(writeFile));
      writer.setOutput(ios);
      writer.write(null, new IIOImage(renderedImage, null, null), params);
      ios.close();

    } catch (IOException e) {
      //Tools.log("ScalablePicture.writeJpg caught IOException: " +  e.getMessage() + "\nwhile writing " + writeFile.toString());
      e.printStackTrace();
    }
    //writer = null;
    writer.dispose(); //1.4.1 documentation says to do this.
  }


  /**
   * method to register the listening object of the status events
   */
  public void addStatusListener(ScalablePictureListener listener) {
    scalablePictureStatusListeners.add(listener);
  }


  /**
   * method to register the listening object of the status events
   */
  public void removeStatusListener(ScalablePictureListener listener) {
    scalablePictureStatusListeners.remove(listener);
  }


  /**
   * Method that sets the status of the ScalablePicture object and notifies
   * intereasted objects of a change in status (not built yet).
   */
  private void setStatus(int statusCode, String statusMessage) {
    String filename = (imageUrl == null) ? "" : imageUrl.toString();
    Tools.log("ScalablePicture.setStatus: sending: " + statusMessage + " to all Listeners from Image: " + filename);

    pictureStatusCode = statusCode;
    pictureStatusMessage = statusMessage;

    Enumeration e = scalablePictureStatusListeners.elements();
    while (e.hasMoreElements()) {
      ((ScalablePictureListener) e.nextElement())
          .scalableStatusChange(pictureStatusCode, pictureStatusMessage);
    }
  }


  /**
   * Method that returns the status code of the picture loading.
   */
  public int getStatusCode() {
    return pictureStatusCode;
  }


  /**
   * Method that returns the status code of the picture loading.
   */
  public String getStatusMessage() {
    return pictureStatusMessage;
  }


  /**
   * accessor method to set the quality that should be used on jpg write operations.
   */
  public void setJpgQuality(float quality) {
    //Tools.log( "setJpgQuality requested with " + Float.toString( quality ) );
    if (quality >= 0f && quality <= 1f) {
      //Tools.log( "Quality set." );
      jpgQuality = quality;
    }
  }


  /**
   * sets the picture into fast scaling mode
   */
  public void setFastScale() {
		fastScale = true;
	}
	
	/**
   * sets the picture into quality sacling mode
   */
	public void setQualityScale() {
		fastScale = false;
	}

}