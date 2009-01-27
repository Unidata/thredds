// $Id: PopupManager.java 50 2006-07-12 16:30:06Z caron $
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

package thredds.ui;

import java.awt.*;
import javax.swing.*;

/**
 * Helper class for managing javax.swing.Popup
 */
public class PopupManager {
  private PopupFactory factory = PopupFactory.getSharedInstance(); // LOOK
  private Popup popup;                                             // 1.4

  private JPanel main;
  private JLabel info;
  private boolean isShowing = false;
  private Object showing;
  private StringBuffer sbuff = new StringBuffer();

  public PopupManager(String title) {
    main = new JPanel();
    main.setBorder( new javax.swing.border.TitledBorder(title));
    info = new JLabel();
    //main.setBackground(Color.yellow);
    main.add(info);
    main.setFocusable(false);
  }

  public void show(String text, Point p, Component owner, Object forWho) {
    if (isShowing && (showing == forWho))
      return;
    if (isShowing && (showing != forWho))
      popup.hide();

    isShowing = true;
    showing = forWho;

    sbuff.setLength(0);
    sbuff.append("<html><body>");

    String textSubbed = ucar.unidata.util.StringUtil.substitute( text, "\n", "<br>");
    sbuff.append(textSubbed);
    sbuff.append("</body></html>");
    info.setText(sbuff.toString());

    SwingUtilities.convertPointToScreen(p, owner);
    int x = (int) (p.getX());
    int y = (int) (p.getY());

    popup = factory.getPopup(owner, main, x + 5, y + 5); // LOOK 1.4
    popup.show();
  }

  public void hide() {
    if (!isShowing)
      return;
    isShowing = false;
    popup.hide();
  }
}

/* Change History:
   $Log: PopupManager.java,v $
   Revision 1.5  2005/08/22 01:12:27  caron
   DatasetEditor

   Revision 1.4  2004/09/28 21:39:09  caron
   *** empty log message ***

   Revision 1.3  2004/09/25 00:09:43  caron
   add images, thredds tab

   Revision 1.2  2004/09/24 03:26:34  caron
   merge nj22

   Revision 1.1  2004/05/21 05:57:35  caron
   release 2.0b

 */