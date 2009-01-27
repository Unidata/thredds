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

// JMultiLineToolTip.java
import javax.swing.*;
import javax.swing.plaf.*;

import java.awt.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicToolTipUI;


/**
 * @author Zafir Anjum
 */


public class MultilineTooltip extends JToolTip
{
        private static final String uiClassID = "ToolTipUI";

        String tipText;
        JComponent component;

        public MultilineTooltip() {
            updateUI();
        }

        public void updateUI() {
            setUI(MultiLineToolTipUI.createUI(this));
        }

        public void setColumns(int columns)
        {
                this.columns = columns;
                this.fixedwidth = 0;
        }

        public int getColumns()
        {
                return columns;
        }

        public void setFixedWidth(int width)
        {
                this.fixedwidth = width;
                this.columns = 0;
        }

        public int getFixedWidth()
        {
                return fixedwidth;
        }

        protected int columns = 0;
        protected int fixedwidth = 0;
}



     class MultiLineToolTipUI extends BasicToolTipUI {
        static MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();
        Font smallFont;
        static JToolTip tip;
        protected CellRendererPane rendererPane;

        private static JTextArea textArea ;

        public static ComponentUI createUI(JComponent c) {
            return sharedInstance;
        }

        public MultiLineToolTipUI() {
            super();
        }

        public void installUI(JComponent c) {
            super.installUI(c);
                tip = (JToolTip)c;
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
                String tipText = ((JToolTip)c).getTipText();
                if (tipText == null)
                        return new Dimension(0,0);
                textArea = new JTextArea(tipText );
            rendererPane.removeAll();
                rendererPane.add(textArea );
                textArea.setWrapStyleWord(true);
                int width = ((MultilineTooltip)c).getFixedWidth();
                int columns = ((MultilineTooltip)c).getColumns();

                if( columns > 0 )
                {
                        textArea.setColumns(columns);
                        textArea.setSize(0,0);
                textArea.setLineWrap(true);
                        textArea.setSize( textArea.getPreferredSize() );
                }
                else if( width > 0 )
                {
                textArea.setLineWrap(true);
                        Dimension d = textArea.getPreferredSize();
                        d.width = width;
                        d.height++;
                        textArea.setSize(d);
                }
                else
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

/* Change History:
   $Log: MultilineTooltip.java,v $
   Revision 1.5  2004/11/07 03:00:48  caron
   *** empty log message ***

   Revision 1.4  2004/10/06 19:03:39  caron
   clean up javadoc
   change useV3 -> useRecordsAsStructure
   remove id, title, from NetcdfFile constructors
   add "in memory" NetcdfFile

   Revision 1.3  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.2  2004/05/21 05:57:34  caron
   release 2.0b

*/
