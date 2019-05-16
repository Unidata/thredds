/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import ucar.nc2.constants.CDM;
import ucar.ui.util.Resource;
import ucar.ui.widget.BAMutil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

/**
 *
 */
public class ToolsAboutWindow extends JWindow {

/**
 *
 */
    public ToolsAboutWindow(final JFrame parent) {
        super(parent);

        JLabel lab1 = new JLabel("<html> <body bgcolor=\"#FFECEC\"> <center>" +
              "<h1>Netcdf Tools User Interface (ToolsUI)</h1>" +
              "<b>" + getVersion() + "</b>" +
              "<br><i>http://www.unidata.ucar.edu/software/netcdf-java/</i>" +
              "<br><b><i>Developers:</b> John Caron, Sean Arms, Dennis Heimbinger, Ryan May, Christian Ward-Garrison</i></b>" +
              "</center>" +
              "<br><br>With thanks to these <b>Open Source</b> contributors:" +
              "<ul>" +
              "<li><b>ADDE/VisAD</b>: Bill Hibbard, Don Murray, Tom Whittaker, et al (http://www.ssec.wisc.edu/~billh/visad.html)</li>" +
              "<li><b>Apache HTTP Components</b> libraries: (http://hc.apache.org/)</li>" +
              "<li><b>Apache Jakarta Commons</b> libraries: (http://jakarta.apache.org/commons/)</li>" +
              "<li><b>IDV:</b> Yuan Ho, Julien Chastang, Don Murray, Jeff McWhirter, Yuan H (http://www.unidata.ucar.edu/software/IDV/)</li>" +
              "<li><b>Joda Time</b> library: Stephen Colebourne (http://www.joda.org/joda-time/)</li>" +
              "<li><b>JDOM</b> library: Jason Hunter, Brett McLaughlin et al (www.jdom.org)</li>" +
              "<li><b>JGoodies</b> library: Karsten Lentzsch (www.jgoodies.com)</li>" +
              "<li><b>JPEG-2000</b> Java library: (http://www.jpeg.org/jpeg2000/)</li>" +
              "<li><b>JUnit</b> library: Erich Gamma, Kent Beck, Erik Meade, et al (http://sourceforge.net/projects/junit/)</li>" +
              "<li><b>NetCDF C Library</b> library: Russ Rew, Ward Fisher, Dennis Heimbinger</li>" +
              "<li><b>OPeNDAP Java</b> library: Dennis Heimbinger, James Gallagher, Nathan Potter, Don Denbo, et. al.(http://opendap.org)</li>" +
              "<li><b>Protobuf serialization</b> library: Google (http://code.google.com/p/protobuf/)</li>" +
              "<li><b>Simple Logging Facade for Java</b> library: Ceki Gulcu (http://www.slf4j.org/)</li>" +
              "<li><b>Spring lightweight framework</b> library: Rod Johnson, et. al.(http://www.springsource.org/)</li>" +
              "<li><b>Imaging utilities:</b>: Richard Eigenmann</li>" +
              "<li><b>Udunits:</b>: Steve Emmerson</li>" +
              "</ul><center>Special thanks to <b>Sun/Oracle</b> (java.oracle.com) for the platform on which we stand." +
              "</center></body></html> ");

        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.BLACK, 1),
                                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        main.setBackground(new Color(0xFFECEC));

        JLabel ring = new JLabel(new ImageIcon(BAMutil.getImage("nj22/NetcdfUI")));
        ring.setOpaque(true);
        ring.setBackground(new Color(0xFFECEC));

        JLabel threddsLogo = new JLabel(Resource.getIcon(BAMutil.getResourcePath() + "nj22/Cdm.png", false));
        threddsLogo.setBackground(new Color(0xFFECEC));
        threddsLogo.setOpaque(true);

        main.add(threddsLogo, BorderLayout.NORTH);
        main.add(lab1, BorderLayout.CENTER);
        main.add(ring, BorderLayout.SOUTH);
        getContentPane().add(main);
        pack();

        //show();
        final Dimension labelSize = getPreferredSize();

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment ( );
        final GraphicsDevice gd = ge.getDefaultScreenDevice ( );
        final GraphicsConfiguration gc = gd.getDefaultConfiguration ( );

        Point location;

        if (gc != null) {
            final Rectangle gcrect = gc.getBounds();

            location = new Point(gcrect.x + gcrect.width  / 2 - (labelSize.width  / 2),
                                 gcrect.y + gcrect.height / 2 - (labelSize.height / 2));
        }
        else {
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            location = new Point(screenSize.width  / 2 - (labelSize.width  / 2),
                                 screenSize.height / 2 - (labelSize.height / 2));
        }

        setLocation(location);

        // Any click on the window hides it.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setVisible(false);
            }
        });
        setVisible(true);
    }

/**
 *
 */
    private String getVersion() {
        String version;
        try (InputStream is = Resource.getFileResource("/README")) {
            if (is == null) {
                return "5.0";
            }
            BufferedReader dataIS = new BufferedReader(new InputStreamReader(is, CDM.utf8Charset));
            StringBuilder sbuff = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                sbuff.append(dataIS.readLine());
                sbuff.append("<br>");
            }
            version = sbuff.toString();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            version = "version unknown";
        }

        return version;
    }
}
