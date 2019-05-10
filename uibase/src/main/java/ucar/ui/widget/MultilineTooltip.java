/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.*;

/*
 * @author Zafir Anjum
 */
public class MultilineTooltip extends JToolTip {
  JComponent component;

  public MultilineTooltip() {
    updateUI();
  }

  public void updateUI() {
    setUI(MultiLineToolTipUI.createUI(this));
  }

  public void setColumns(int columns) {
    this.columns = columns;
    this.fixedwidth = 0;
  }

  public int getColumns() {
    return columns;
  }

  public void setFixedWidth(int width) {
    this.fixedwidth = width;
    this.columns = 0;
  }

  public int getFixedWidth() {
    return fixedwidth;
  }

  protected int columns = 0;
  protected int fixedwidth = 0;
}

class MultiLineToolTipUI extends BasicToolTipUI {
  static MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();
  static JToolTip tip;
  protected CellRendererPane rendererPane;

  private static JTextArea textArea;

  public static ComponentUI createUI(JComponent c) {
    return sharedInstance;
  }

  public MultiLineToolTipUI() {
    super();
  }

  public void installUI(JComponent c) {
    super.installUI(c);
    tip = (JToolTip) c;
    rendererPane = new CellRendererPane();
    c.add(rendererPane);
  }

  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);

    c.remove(rendererPane);
    rendererPane = null;
  }

  public void paint(Graphics g, JComponent c) {
    Dimension size = c.getSize();
    textArea.setBackground(c.getBackground());
    rendererPane.paintComponent(g, textArea, c, 1, 1,
            size.width - 1, size.height - 1, true);
  }

  public Dimension getPreferredSize(JComponent c) {
    String tipText = ((JToolTip) c).getTipText();
    if (tipText == null)
      return new Dimension(0, 0);
    textArea = new JTextArea(tipText);
    rendererPane.removeAll();
    rendererPane.add(textArea);
    textArea.setWrapStyleWord(true);
    int width = ((MultilineTooltip) c).getFixedWidth();
    int columns = ((MultilineTooltip) c).getColumns();

    if (columns > 0) {
      textArea.setColumns(columns);
      textArea.setSize(0, 0);
      textArea.setLineWrap(true);
      textArea.setSize(textArea.getPreferredSize());
    } else if (width > 0) {
      textArea.setLineWrap(true);
      Dimension d = textArea.getPreferredSize();
      d.width = width;
      d.height++;
      textArea.setSize(d);
    } else
      textArea.setLineWrap(false);


    Dimension dim = textArea.getPreferredSize();

    dim.height += 1;
    dim.width += 1;
    return dim;
  }

  public Dimension getMinimumSize(JComponent c) {
    return getPreferredSize(c);
  }

  public Dimension getMaximumSize(JComponent c) {
    return getPreferredSize(c);
  }
}