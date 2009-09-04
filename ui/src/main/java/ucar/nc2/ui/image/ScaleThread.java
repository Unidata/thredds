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

/*
ScaleThread.java:  class that calls scalePicture methods of a ScalablePicture

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
 *  a thread object that calls the ScalablePicture scalePicture method
 */

public class ScaleThread extends Thread {

	private ScalablePicture sclPic;


	/**
	 *  @param sclPic	The picture we are doing this for
	 */
	ScaleThread ( ScalablePicture sclPic ) {
		//Tools.log("Constructing ScaleThread object");
		this.sclPic = sclPic;
	}
		
	
	/**
	 *  method that is invoked by the thread to do things asynchroneousely
	 */
	public void run() {
		sclPic.scalePicture();
	}


	public void finalize() {
    //Tools.log( "ScaleThread.finalize: " + sclPic.imageUrl.toString() );
	}

}


