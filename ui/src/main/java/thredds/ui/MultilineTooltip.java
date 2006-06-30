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
