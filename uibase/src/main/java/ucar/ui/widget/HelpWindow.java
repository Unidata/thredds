/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

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
      cp.setLayout( new BorderLayout());
      cp.add(ta, BorderLayout.CENTER);
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