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
package ucar.nc2.ui.image;

import java.util.*;
import java.net.*;


/*
PictureCache.java:  class that manages the cache of pictures

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
 *  class that manages the cache of pictures
 *
 **/
public class PictureCache  {

	/**
	 *  hashtable to facilitate the caching of images. It is static 
	 *  so that there is just one for the application
	 */
	private static Hashtable pictureCache = new Hashtable();


	/**
	 *  This Vector keeps track of which pictures the PictureCache has been
	 *  requested to load in the background. They may have to be stopped in 
	 *  a hurry.
	 */
	public static Vector cacheLoadsInProgress = new Vector();


	/**
	 *  Vector to keep track of what we should remove first in the cache
	 */
	private static Vector removalQueue = new Vector();


  private static int maxCache = 10; // was Settings


	/**
	 *  this method removes the least popular picture(s) in the cache.
	 *  It first removes those pictures which have been suggested for
	 *  removal. And then it picks any it can find
	 *  As many pictures are removed as nescessary untill there are less pictures in the 
	 *  cache than the Settings.maxCache specifies. (If maxCache is 0 then the
	 *  Enumeration finds no elements and we don't get an endless loop.
	 */
	public static synchronized void removeLeastPopular() {
		Tools.log("PictureCache.removeLeastPopular:");
		//reportCache();

		Enumeration e = removalQueue.elements();
		while ( ( e.hasMoreElements() ) 
		    &&  ( pictureCache.size() >= maxCache ) ) {
		     	String removeElement = (String) e.nextElement();
		     	Tools.log ("PictureCache.remove: " + removeElement );
			pictureCache.remove( removeElement );
			removalQueue.remove( removeElement );
		}
		
		e = pictureCache.keys();
		while ( ( pictureCache.size() >= maxCache )
		     && ( e.hasMoreElements() ) ) {
		     	String removeElement = (String) e.nextElement();
		     	Tools.log ("PictureCache.remove: " + removeElement );
			pictureCache.remove( removeElement );
		} 
		System.gc();
		//reportCache();
	}
		


	/**
	 *   Method that can be called when a picture is no longer needed.
	 *   These pictures will be removed first from the cache when
	 *   we need more space.
	 */
/*	public static void suggestCacheRemoval( SortableDefaultMutableTreeNode node ) {
		if (node == null ) return;
		Object userObject = node.getUserObject();
		if ( userObject instanceof PictureInfo ) {
			try {
				removalQueue.add( ((PictureInfo) userObject ).getHighresURL().toString() );
			} catch ( MalformedURLException x ) {
				// ignore
			}
		}
	}*/
		

	
	/**
	 *  returns whether an image is in the cache. <p>
	 */
	public static synchronized boolean isInCache ( URL url ) {
		return isInCache( url.toString() );
	}

	/**
	 *  returns whether an image is in the cache. <p>
	 */
	public static synchronized boolean isInCache ( String urlString ) {
		return pictureCache.containsKey( urlString );
	}



	/**
	 *  store an image in the cache
	 *  @param url	The URL of the picture
	 *  @param sp	The picture to be stored
	 */
	public static synchronized void add( URL url, SourcePicture sp ) {
		Tools.log("PictureCache.add: " + url.toString() );
		if ( sp.getSourceBufferedImage() == null ) {
			Tools.log ("PictureCache.add: invoked with a null picture! Not cached!");
			return;
		}
		
		if ( ( maxCache < 1 ) ) {
			Tools.log("PictureCache.add: cache is diabled. Not adding picture.");
			return;
		}
		
		if ( isInCache( url ) ) {
			Tools.log( "Picture " + url.toString() + " is already in the cache. Not adding again.");
			return;
		}
		
		if ( pictureCache.size() >= maxCache )
			removeLeastPopular();
			
		if ( pictureCache.size() < maxCache )
			pictureCache.put( url.toString(), sp );
			
		//reportCache();	
	}



	/**
	 *  remove a picture from the cache
	 */
	public static synchronized void remove( String urlString ) {
		if ( isInCache( urlString ) ) {
			pictureCache.remove( urlString );
		}
	}


	/**
	 *  returns a picture from the cache. Returns null if image is not there
	 *  @param url 	The URL of the picture to be retrieved
	 */
	public static synchronized SourcePicture getSourcePicture( URL url ) {
		return (SourcePicture) pictureCache.get( url.toString() );
	}
	
	
	/** 
	 *  clears out all images in the cache. Important after OutOfMemoryErrors
	 */
	public static void clear() {
		Tools.log("PictureCache.clear: Zapping entire cache");
		pictureCache.clear();
	}

	
	/**
	 *  method to inspect the cache
	 */
	public static void reportCache() {
		Tools.log("   PictureCache.reportCache: cache contains: " 
			+ Integer.toString( pictureCache.size() )
			+ " max: " 
			+ Integer.toString( maxCache ) );
		//Tools.freeMem();
		Enumeration e = pictureCache.keys();
		while ( e.hasMoreElements() ) {
			Tools.log("   Cache contains: " + ((String) e.nextElement()) );
		} 
		Tools.log("  End of cache contents");
	}



	/** 
	 * method to stop all background loading
	 */
	public static void stopBackgroundLoading() {
		Enumeration e = cacheLoadsInProgress.elements();
		while ( e.hasMoreElements() ) {
			((SourcePicture) e.nextElement()).stopLoading();
		}
	}
	


	/** 
	 *  method to stop all background loading except the indicated file. Returns whether the
	 *  image is already being loaded. True = loading in progress, False = not in progress.
	 */
	public static boolean stopBackgroundLoadingExcept( URL exemptionURL ) {
		SourcePicture sp;
		String exemptionURLString = exemptionURL.toString();
		Enumeration e = cacheLoadsInProgress.elements();
		boolean inProgress = false;
		while ( e.hasMoreElements() ) {
			sp = ((SourcePicture) e.nextElement());
			if ( ! sp.getUrlString().equals( exemptionURLString ) )
				sp.stopLoading();
			else {
				Tools.log( "PictureCache.stopBackgroundLoading: picture was already loading");
				inProgress = true;
			}
		}
		return inProgress;
	}

	

}
