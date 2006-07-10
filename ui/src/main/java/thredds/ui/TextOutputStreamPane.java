// $Id: TextOutputStreamPane.java,v 1.2 2004/09/30 00:33:38 caron Exp $
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
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

/**
 * A TextHistoryPane widget connected to a ByteBufferOutputStream for its text.
 *
 * @author John Caron
 * @version $Id: TextOutputStreamPane.java,v 1.2 2004/09/30 00:33:38 caron Exp $
 */

public class TextOutputStreamPane extends TextHistoryPane {
    private ByteArrayOutputStream bos;

    public TextOutputStreamPane() {
      super(true);

      bos = new ByteArrayOutputStream(20000);

      JButton showButton = new JButton("Show");
      showButton.setToolTipText("Show contents");
      showButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setText( bos.toString());
        }
      });
      JButton clearButton = new JButton("Clear");
      clearButton.setToolTipText("Clear contents");
      clearButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          bos.reset();
          setText( bos.toString());
        }
      });

      JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
      buttPanel.add( showButton);
      buttPanel.add( clearButton);

      // setLayout( new BorderLayout());
      add( buttPanel, BorderLayout.NORTH);
      // add( new JScrollPane(ta), BorderLayout.CENTER);
    }

    public ByteArrayOutputStream getOutputStream() { return bos; }

}

/* Change History:
   $Log: TextOutputStreamPane.java,v $
   Revision 1.2  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.1  2004/09/24 03:26:35  caron
   merge nj22

   Revision 1.5  2004/06/12 02:08:40  caron
   validate dqc or catalog

   Revision 1.4  2004/06/04 00:51:57  caron
   release 2.0b

   Revision 1.3  2004/05/11 23:30:36  caron
   release 2.0a

   Revision 1.2  2004/03/05 23:43:25  caron
   1.3.1 release

 */