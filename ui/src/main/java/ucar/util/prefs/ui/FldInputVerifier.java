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
