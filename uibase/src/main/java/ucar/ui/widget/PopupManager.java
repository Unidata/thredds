/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.widget;

import com.google.common.base.CharMatcher;
import java.awt.Component;
import java.awt.Point;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;

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
    main.add(info);
    main.setFocusable(false);
  }

  public void show(String text, Point p, Component owner, Object forWho) {
    if (isShowing && (showing == forWho)) { return; }
    if (isShowing) { popup.hide(); }

    isShowing = true;
    showing = forWho;

    sbuff.setLength(0);
    sbuff.append("<html><body>");

    String textSubbed = CharMatcher.is('\n').replaceFrom(text, "<br>");
    sbuff.append(textSubbed);
    sbuff.append("</body></html>");
    info.setText(sbuff.toString());

    SwingUtilities.convertPointToScreen(p, owner);
    int x = (int) (p.getX());
    int y = (int) (p.getY());

    popup = factory.getPopup(owner, main, x + 5, y + 5); // LOOK 1.4
    if (popup != null ) { popup.show(); }
  }

  public void hide() {
    if (!isShowing)
      return;
    isShowing = false;
    if (popup != null) { popup.hide(); }
  }
}
