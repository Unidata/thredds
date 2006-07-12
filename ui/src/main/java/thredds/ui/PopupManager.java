// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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