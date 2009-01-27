// $Id: HelpWindow.java 50 2006-07-12 16:30:06Z caron $
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