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
import java.io.*;
import java.net.*;
import java.awt.image.*;
import java.awt.Dimension;
import java.util.Vector;
import java.util.Enumeration;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.imageio.event.*;
import java.text.*;
import javax.swing.*;
import java.awt.geom.*;

/*
SourcePicture.java:  class that can load a picture from a URL

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
 *  a class to load and scale a picture either immediately or in a 
 *  seperate thread from a URL
 */

public class SourcePicture implements Cloneable {

 /**
	 *  status code used to signal that the picture is not loaded
	 */
	public static final int UNINITIALISED = 0;

	/**
	 * status code used to signal that the thread is loading the image
	 */
	public static final int LOADING = UNINITIALISED + 1;

	/**
	 * status code used to signal that the thread is rotating the image
	 */
	public static final int ROTATING = LOADING + 1;

	/**
	 *  status code used to signal that the rotated image is available.
	 */
	public static final int READY = ROTATING + 1;

	/**
	 *  status code used to signal that there was an error
	 */
	public static final int ERROR = READY + 1;

	/**
	 *  status code used to tell that we have started loading an
	 *  image but only used on notifySourceLoadProgressListeners
	 */
	public static final int LOADING_STARTED = ERROR + 1;

	/**
	 *  status code used to tell that we have a progress update
	 *  but only used on notifySourceLoadProgressListeners
	 */
	public static final int LOADING_PROGRESS = LOADING_STARTED + 1;

	/**
	 *  status code used to tell that we have a finished loading
	 *  but only used on notifySourceLoadProgressListeners
	 */
	public static final int LOADING_COMPLETED = LOADING_PROGRESS + 1;


  ////////////////////////////////////////////////////////////////////////////

	/**
	 *  the Buffered Image that this class protects and provides features for.
	 */
	public BufferedImage sourcePictureBufferedImage = null;

	/**
	 *  the URL of the picture
	 */
	private URL imageUrl = null;

	/**
	 *  variable to track the status of the picture
	 */
	private int pictureStatusCode;
	

	/**
	 *  variable to compose te status message
	 */	
	private String pictureStatusMessage;

	/** 
	 *  the time it took to load the image
	 */
	public long loadTime = 0;
	
	/**
	 *  A vector that holds all the listeners that want to be notified about 
	 *  changes to this SourcePicture.
	 */
	private Vector sourcePictureListeners = new Vector();

	/**
	 *  variable that records how much has been loaded
	 */
	private int percentLoaded = 0;

	/**
	 *  reference to the inner class that listens to the image loading progress
	 */
	protected ImageProgressListener imageProgressListener = new ImageProgressListener();

	/**
	 * the reader object that will read the image
	 */
	private ImageReader reader;
	
	/**
	 *   Indicator to tell us if the loading was aborted.
	 */
	private boolean abortFlag = false;	 

	/**
	 *  Rotation 0-360 that the image is subjected to after loading
	 */
	private double rotation = 0;

	/**
	 *   Constructor
	 */
	SourcePicture() {
		setStatus(UNINITIALISED, "Uninitialised SourcePicture object.");
	}

	/**
	 *  method to invoke with a filename or URL of a picture that is to be loaded 
	 *  a new thread. This is handy to update the screen while the loading chuggs along in the background.
	 *
	 *  @param	imageUrl	The URL of the image to be loaded
	 *  @param	priority	The Thread priority for this thread.
	 *  @param	rotation	The rotation 0-360 to be used on this picture
	 */
	public void loadPictureInThread( URL imageUrl, int priority, double rotation ) {
		if ( pictureStatusCode == LOADING ) {
			stopLoadingExcept( imageUrl );
		}
		
		this.imageUrl = imageUrl;
		this.rotation = rotation;
		LoadThread t = new LoadThread( this );
		t.setPriority( priority );
		t.start();
	}

	/**
	 *  method to invoke with a filename or URL of a picture that is to be loaded in
	 *  the main thread.
	 */
	public void loadPicture( URL imageUrl, double rotation ) {
		if ( pictureStatusCode == LOADING ) {
			stopLoadingExcept( imageUrl );
		}
		this.imageUrl = imageUrl;
		this.rotation = rotation;
		loadPicture();

	}

	/**
	 *  loads a picture from the URL in the imageUrl object into the sourcePictureBufferedImage
	 *  object and updates the status when done or failed.
	 */
	public void loadPicture() {
		Tools.log("SourcePicture.loadPicture: " + imageUrl.toString() + " loaded into SourcePicture object: " + Integer.toString(this.hashCode()) );
		//Tools.freeMem();
						
		setStatus( LOADING, "Loading: " + imageUrl.toString() );
		abortFlag = false;
		long start = System.currentTimeMillis();
		loadTime = 0;
				
		try {
			
			// Java 1.4 way with a Listener
			ImageInputStream iis = ImageIO.createImageInputStream( imageUrl.openStream() );
			Iterator i = ImageIO.getImageReaders( iis );
			if ( ! i.hasNext() ) { throw new IOException ("No Readers Available!"); }
			reader = (ImageReader) i.next();  // grab the first one
			
			reader.addIIOReadProgressListener( imageProgressListener );
			reader.setInput( iis );
			sourcePictureBufferedImage = null;
			try {
				sourcePictureBufferedImage = reader.read( 0 ); // just get the first image
			} catch ( OutOfMemoryError e ) {
				Tools.log("SourcePicture caught an OutOfMemoryError while loading an image." );

				iis.close();
				reader.removeIIOReadProgressListener( imageProgressListener );
				reader.dispose();
				i=null;
			
				setStatus(ERROR, "Out of Memory Error while reading " + imageUrl.toString());
				sourcePictureBufferedImage = null; 
				// PictureCache.clear();

				JOptionPane.showMessageDialog( null,  //deliberately null or it swaps the window
					"outOfMemoryError",
					"genericError",
					JOptionPane.ERROR_MESSAGE);

				System.gc();
				System.runFinalization();

				Tools.log("JPO has now run a garbage collection and finalization.");
				return;
			}
			
			
			iis.close();
			reader.removeIIOReadProgressListener( imageProgressListener );
			//Tools.log("!!dispose being called!!");
			reader.dispose();
			i=null;

			

			if ( ! abortFlag ) {
			
				if ( rotation != 0 ) {
					setStatus( ROTATING, "Rotating: " + imageUrl.toString() );
					int xRot = sourcePictureBufferedImage.getWidth() / 2;
					int yRot = sourcePictureBufferedImage.getHeight() / 2;
					AffineTransform rotateAf = AffineTransform.getRotateInstance( Math.toRadians( rotation ), xRot, yRot );
					AffineTransformOp op = new AffineTransformOp( rotateAf, AffineTransformOp.TYPE_BILINEAR );
					Rectangle2D newBounds = op.getBounds2D( sourcePictureBufferedImage );
					// a simple AffineTransform would give negative top left coordinates -->
					// do another transform to get 0,0 as top coordinates again.
					double minX = newBounds.getMinX();
					double minY = newBounds.getMinY();
					
					AffineTransform translateAf = AffineTransform.getTranslateInstance( minX * (-1), minY * (-1) );
					rotateAf.preConcatenate( translateAf );
					op = new AffineTransformOp( rotateAf, AffineTransformOp.TYPE_BILINEAR );
					newBounds = op.getBounds2D( sourcePictureBufferedImage );

					// this piece of code is so essential!!! Otherwise the internal image format
					// is totally altered and either the AffineTransformOp decides it doesn't
					// want to rotate the image or web browsers can't read the resulting image.
					BufferedImage targetImage = new BufferedImage(
						(int) newBounds.getWidth(),
						(int) newBounds.getHeight(),
						BufferedImage.TYPE_3BYTE_BGR );
					
					sourcePictureBufferedImage = op.filter( sourcePictureBufferedImage, targetImage );
				}

				setStatus( READY, "Loaded: " + imageUrl.toString() );
				long end   = System.currentTimeMillis();
				loadTime = end - start;
				PictureCache.add( imageUrl, (SourcePicture) this.clone() );
			} else {
				loadTime = 0;
				setStatus( ERROR, "Aborted: " + imageUrl.toString() );
				sourcePictureBufferedImage = null; 
			}
		} catch ( IOException e ) {
			setStatus(ERROR, "Error while reading " + imageUrl.toString());
			sourcePictureBufferedImage = null; 
		};  

	}


	/**
	 *  this method can be invoked to stop the current reader
	 */
	public void stopLoading() {
		if ( imageUrl == null ) 
			return; // SourcePicture has never been used yet
	
		Tools.log("SourcePicture.stopLoading: called on " + imageUrl );
		if ( pictureStatusCode == LOADING ) {
			reader.abort();
			abortFlag = true;
			//reader.dispose();
			//setStatus( ERROR, "Cache Loading was stopped " + imageUrl.toString() );
			//sourcePictureBufferedImage = null; 
			// actually the thread reading the image continues
		}
	}
	

	/**
	 *  this method can be invoked to stop the current reader except if it
	 *  is reading the desired file. It returns true is the desired file
	 *  is being loaded. Otherwise it returns false.
	 */
	public boolean stopLoadingExcept( URL exemptionURL ) {
		if ( imageUrl == null ) 
			return false; // has never been used yet

		if ( pictureStatusCode != LOADING ) {
			Tools.log( "SourcePicture.stopLoadingExcept: called but pointless since image is not LOADING: " + imageUrl.toString());
			return false;
		}
			
		if ( ! exemptionURL.equals( imageUrl ) ) {
			Tools.log ("SourcePicture.stopLoadingExcept: called with Url " + exemptionURL.toString() + " --> stopping loading of " + imageUrl.toString() );
			stopLoading();
			return true;
		} else
			return false;
	}

	/**
	 *   return the size of the image or Zero if there is none
	 */	
	public Dimension getSize() {
		if ( sourcePictureBufferedImage != null )
			return new Dimension( sourcePictureBufferedImage.getWidth(), sourcePictureBufferedImage.getHeight());
		else 
			return new Dimension(0,0);
		
	}

	/**
	 *   return the height of the image or Zero if there is none
	 */	
	public int getHeight() {
		if ( sourcePictureBufferedImage != null )
			return sourcePictureBufferedImage.getHeight();
		else 
			return 0;
	}

	/**
	 *   return the width of the image or Zero if there is none
	 */	
	public int getWidth() {
		if ( sourcePictureBufferedImage != null )
			return sourcePictureBufferedImage.getWidth();
		else 
			return 0;
	}



	/**
	 *   return the URL of the original image as a string
	 */	
	public String getUrlString() { 
		return imageUrl.toString();
	}


	/**
	 *  return the URL of the original image
	 */
	public URL getUrl() {
		return imageUrl;
	}
	

	/**
	 *  return the rotation of the image
	 */
	public double getRotation() {
		return rotation;
	}
	

	/**
	 *  method to register the listening object of the status events
	 */
	public void addListener ( SourcePictureListener listener ) {
		Tools.log("SourcePicture.addListener: listener added on SourcePicture " + Integer.toString(this.hashCode()) + " of class: " + listener.getClass().toString() );
		sourcePictureListeners.add( listener );
		//showListeners();
	}


	/**
	 *  method to register the listening object of the status events
	 */
	public void removeListener ( SourcePictureListener listener ) {
		Tools.log("SourcePicture.removeListener: listener removed from SourcePicture " + Integer.toString(this.hashCode()) + " of class: " + listener.getClass().toString() );
		sourcePictureListeners.remove( listener );
		//showListeners();
	}


	public void showListeners() {
		Tools.log("SourcePicture.showListeners: SoucePicture " + Integer.toString(this.hashCode()));
		Enumeration en = sourcePictureListeners.elements();
		while ( en.hasMoreElements() ) {
			Object obj = en.nextElement();
			Tools.log("    reports to Listener: " + Integer.toString(obj.hashCode()) + " of class " + obj.getClass().toString() );
		}
		Tools.log("    --------");
	}

	

	/**
	 *  method that says whether there are any listeners attached to this object
	 */
	public boolean hasNoListeners() {
		return ( sourcePictureListeners.isEmpty() );
	}

	/**
	 * Method that sets the status of the ScalablePicture object and notifies 
	 * intereasted objects of a change in status (not built yet).
	 */
	private void setStatus(int statusCode, String statusMessage) {
		Tools.log("\nSourcePicture.setStatus: sending status: " + statusMessage );
		pictureStatusCode = statusCode;
		pictureStatusMessage = statusMessage;
		
		
		Vector nonmodifiedVector = (Vector) sourcePictureListeners.clone();

		Enumeration e = nonmodifiedVector.elements();
		while ( e.hasMoreElements() ) {
			//((SourcePictureListener) e.nextElement())
			//	.sourceStatusChange(pictureStatusCode, pictureStatusMessage, this );
			SourcePictureListener spl = ((SourcePictureListener) e.nextElement());
				spl.sourceStatusChange(pictureStatusCode, pictureStatusMessage, this );

			Tools.log("\nSourcePicture.setStatus: sending status: " + statusMessage + " to " + spl.getClass().toString() );
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
	 *  Returns how much of the image has been loaded
	 */
	public int getPercentLoaded() {
		return percentLoaded;
	}


	/**
	 *  returns the buffered image that was loaded or null if there is no image.
	 *
	 *  @return	the <code>BufferedImage</code> that was loaded or null if there is no image.
	 */
	public BufferedImage getSourceBufferedImage() {
		return sourcePictureBufferedImage;
	}

	/**
	 *  sets the buffered image. Unusual method use with care.
	 *
	 */
	public void setSourceBufferedImage( BufferedImage img, String statusMessage ) {
		sourcePictureBufferedImage = img;
		setStatus( READY, statusMessage );
	}

	/**
	 *  returns a reference to this <code>SourcePicture</code> object
	 *
	 *  @return	the reference to this <code>SourcePicture</code> object
	 */
	public SourcePicture getSourcePicture() {
		return this;
	}

	/**
	 *  used to check when the garbage collector wandered by
	 */
	public void finalize() {
		Tools.log("SourcePicture.finalize: " + imageUrl.toString() );
	}


	/**
	 *  creates a copy of the SourcePicture
	 */
	public Object clone() {
		Object obj = null;
		try {
			obj = super.clone();
		} catch ( CloneNotSupportedException e ) {
			Tools.log("Cloning not supported on SourcePicture!");
		}
		return obj;
	}
	

	/**
	 *  Special class that allows to catch notifications about how the image
	 *  reading is getting along
	 */
	class ImageProgressListener implements IIOReadProgressListener {
		private DecimalFormat percentageFormatter = new DecimalFormat("##0");

		private void notifySourceLoadProgressListeners( int statusCode, int percentage ) {
			percentLoaded = percentage;
			Enumeration e = sourcePictureListeners.elements();
			while ( e.hasMoreElements() ) {
				((SourcePictureListener) e.nextElement())
					.sourceLoadProgressNotification( statusCode, percentage );
			}
		}


		public void imageComplete( ImageReader source ) {
			notifySourceLoadProgressListeners ( LOADING_COMPLETED, 100 );
			Tools.log( "imageComplete" );
		}
		public void imageProgress( ImageReader source, float percentageDone ) {
			notifySourceLoadProgressListeners ( LOADING_PROGRESS, (new Float(percentageDone)).intValue() );
			Tools.log( "imageProgress: " + percentageFormatter.format( percentageDone ) + "%");
		}
		public void imageStarted( ImageReader source, int imageIndex ) {
			notifySourceLoadProgressListeners ( LOADING_STARTED, 0 );
			Tools.log( "imageStarted" );
		}
		public void readAborted( ImageReader source ) {
		}
		public void sequenceComplete( ImageReader source ) {
		}
		public void sequenceStarted( ImageReader source, int minIndex ) {
		}
		public void thumbnailComplete( ImageReader source ) {
		}
		public void thumbnailProgress( ImageReader source, float percentageDone ) {
		}
		public void thumbnailStarted( ImageReader source, int imageIndex, int thumbnailIndex ) {
		}
		
	}

}