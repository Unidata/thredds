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
package ucar.util.prefs.ui;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/**
 * started from jroller article.
 */

public class FldInputVerifier extends InputVerifier implements KeyListener {
  static private Color color = new Color(243, 255, 159);  // yaller
  private PopupFactory popupFactory = PopupFactory.getSharedInstance();
  private Popup popup;
  private Field fld;
  private JPanel main;
  private JLabel message = new JLabel();


  /**
   * @param c       The Swing component to listen to.
   * @param fld       The Field to be validated.
   */

  public FldInputVerifier(Component c, Field fld) {
    this.fld = fld;
    c.addKeyListener(this);
    main = new JPanel();
    main.setBackground(color);
    message.setBackground(color);
    main.add(message);
  }

  public boolean verify(JComponent c) {
    StringBuffer buff = new StringBuffer();
    if (!fld.validate(buff)) {
      message.setText( buff.toString());
      c.setBackground(Color.PINK);
      makePopup(c);
      return false;
    }

    c.setBackground(Color.WHITE);
    removePopup();
    return true;
  }

  private void makePopup(JComponent c) {
    removePopup(); // in case theres already one
    
    Point p = c.getLocationOnScreen();
    Dimension cDim = c.getSize();
    popup = popupFactory.getPopup(c, main, (int) p.getX() + 15, (int) (p.getY() + cDim.getHeight())); // LOOK 1.4
    popup.show();
  }

  private void removePopup() {
    if (popup != null)
      popup.hide();
    popup = null;
  }

  /**
   * @see KeyListener
   */

  public void keyPressed(KeyEvent e) {
    removePopup();
  }

  /**
   * @see KeyListener
   */

  public void keyTyped(KeyEvent e) {
    removePopup();
  }

  /**
   * @see KeyListener
   */

  public void keyReleased(KeyEvent e) {
    removePopup();
  }

}
