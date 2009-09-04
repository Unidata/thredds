

/*
LoadThread.java:  class that calls the loadPicture method of a SourcePicture in a thread

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




/**
 *  a class that moves all pictures of a group to a target directory
 */

public class LoadThread extends Thread {

	private SourcePicture srcPic;


	/**
	 *  This class serves to create threads that load a SourcePicture.
	 *  The SourcePicture creates this thread with itself as a reference.
	 *  The thread spawns and then invokes the SourcePicture's loadPicture
	 *  method.
	 *
	 *  @param srcPic	The SourcePicture that should have it's loadPicture 
	 *			method called from the new thread.
	 */
	LoadThread ( SourcePicture srcPic ) {
		this.srcPic = srcPic;
	}
		
	
	/**
	 *  method that is invoked by the thread which fires off the loadPicture 
	 *  method in the srcPic object.
	 */
	public void run() {
		srcPic.loadPicture();
	}
	

}


