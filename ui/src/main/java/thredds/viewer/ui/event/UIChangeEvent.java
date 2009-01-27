// $Id: UIChangeEvent.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.ui.event;

import java.awt.Rectangle;
/** Change events for UI objects.
 * @author John Caron
 * @version $Id: UIChangeEvent.java 50 2006-07-12 16:30:06Z caron $
 */
public class UIChangeEvent extends java.util.EventObject {
  private String property;
  private Object objectChanged;
  private Object newValue;

  public UIChangeEvent(Object source, String property, Object changed, Object newValue) {
    super(source);
    this.property = property;
    this.objectChanged = changed;
    this.newValue = newValue;
  }

  public String getChangedProperty() { return property; }
  public Object getChangedObject() { return objectChanged; }
  public Object getNewValue() { return newValue; }

  public String toString() {
    return "UIChangeEvent: "+ property+ " objectChanged: "+ objectChanged+ "  newValue: "+ newValue;
  }
}

/* Change History:
   $Log: UIChangeEvent.java,v $
   Revision 1.2  2004/09/24 03:26:40  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.4  2000/08/18 04:16:03  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:44:15  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:27:09  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:49  caron
   startAgain

# Revision 1.3  1999/03/16  17:00:59  caron
# fix StationModel editing; add TopLevel
#
# Revision 1.2  1998/12/14  17:12:03  russ
# Add comment for accumulating change histories.
#
*/
