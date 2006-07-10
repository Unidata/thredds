// $Id: HelpWindow.java,v 1.2 2004/09/24 03:26:33 caron Exp $
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
 * Popup Help window.
 * @author jcaron
 * @version 1.0
 *
 * Example:
 *
 * <pre>
*  if (help != null) {
    helpButton = new JButton("help");
    helpButton.addActionListener(new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (helpWindow == null)
          helpWindow = new HelpWindow(null, "Help on "+tit, helpMessage);
        helpWindow.show(helpButton);
       }
    });
    butts.add(helpButton);
  }
  </pre>
 */

public class HelpWindow extends IndependentDialog {
  private String helpMessage;
  private JTextArea ta;

  public HelpWindow(JFrame parent, String title, String helpMessage) {
    super( parent, true, title);
    this.helpMessage = helpMessage;
  }

  public void show( Component source) {
    if (ta == null) {
      ta = new JTextArea(7, 30);
      ta.setLineWrap(true); // need to insert \n ourself, because  XML removes \n ? or use IndependentWinfow ?
      ta.setWrapStyleWord(true);
      ta.setEditable(false);
      ta.setText(helpMessage);

      Container cp = getContentPane();
      cp.setLayout( new java.awt.BorderLayout());
      cp.add(ta, java.awt.BorderLayout.CENTER);
      pack();
    }

    Point op = new Point( source.getLocation());
    if (parent != null)
      SwingUtilities.convertPoint(source, op, parent);
    else
      SwingUtilities.convertPointToScreen(op, source);

    setLocation( op);
    super.show();
  }

  public void show() {
    show( this);
  }

}

/* Change History:
   $Log: HelpWindow.java,v $
   Revision 1.2  2004/09/24 03:26:33  caron
   merge nj22

   Revision 1.1  2004/05/21 05:57:34  caron
   release 2.0b

 */