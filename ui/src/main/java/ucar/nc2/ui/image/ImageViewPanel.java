package ucar.nc2.ui.image;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import java.util.Date;
import java.util.Random;
import javax.swing.*;
import javax.swing.event.*;

import ucar.nc2.dt.image.ImageDatasetFactory;
import ucar.nc2.dt.image.ImageFactoryRandom;
import ucar.nc2.dt.GridDatatype;
import thredds.ui.BAMutil;

/**
 *  *
 * @author caron
 */
public class ImageViewPanel extends JPanel {
  private static boolean debug = false;

  private ImageDatasetFactory imageDatasetFactory = new ImageDatasetFactory();
  private ImageFactoryRandom imageFactoryRandom;
  private String location;

  private boolean movieIsPlaying = false;
  private javax.swing.Timer timer;
  private int delay = 4000; // millisescs
  private JSpinner spinner;
  private Random random = new Random( System.currentTimeMillis());
  private long start = 0;

  private boolean fullscreenMode = false;

  private PicturePane pixPane;

  public ImageViewPanel(Container buttPanel) {
    pixPane = new PicturePane();
    setLayout(new BorderLayout());
    add( pixPane, BorderLayout.CENTER);

    if (buttPanel == null) {
      buttPanel = new JPanel();
      add( buttPanel, BorderLayout.NORTH);
    }

    AbstractAction prevAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setImage( imageDatasetFactory.getNextImage(false));
      }
    };
    BAMutil.setActionProperties( prevAction, "VCRPrevFrame", "previous", false, 'P', -1);
    BAMutil.addActionToContainer(buttPanel, prevAction);

    AbstractAction nextAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
       setImage( imageDatasetFactory.getNextImage(true));
      }
    };
    BAMutil.setActionProperties( nextAction, "VCRNextFrame", "next", false, 'N', -1);
    BAMutil.addActionToContainer(buttPanel, nextAction);

    AbstractAction loopAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (movieIsPlaying) {
          if (timer != null) timer.stop();
          movieIsPlaying = false;

        } else {
          if (location == null) return;
          File f = new File(location);
          if (!f.exists()) return;

          imageFactoryRandom = new ImageFactoryRandom(f.getParentFile());

          timer = new Timer(delay, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
              setImage(imageFactoryRandom.getNextImage());
              int delayMsecs = delay + random.nextInt() % delay/2;
              timer.setDelay(delayMsecs);
              long time = System.currentTimeMillis();
              long took = time - start;
              start = time;
              //System.out.printf(" delay=%d; took=%d%n ",delayMsecs, took);
            }
          });
          timer.start();
          movieIsPlaying = true;
        }
      }
    };
    BAMutil.setActionProperties( loopAction, "MovieLoop", "loop", true, 'N', -1);
    BAMutil.addActionToContainer(buttPanel, loopAction);

    spinner = new JSpinner( new SpinnerNumberModel(5000, 10, 20000, 1000));
    spinner.addChangeListener( new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Integer value = (Integer) spinner.getModel().getValue();
        delay = value.intValue();
        if (timer != null) timer.setDelay( delay);
      }
    });
    buttPanel.add( spinner);

    AbstractAction fullscreenAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();
        System.out.println("isFullScreenSupported= "+device.isFullScreenSupported());

        fullFrame = new JFrame();
        fullFrame.setUndecorated( true);
        fullFrame.getContentPane().add( pixPane);

        fullscreenMode = true;
        device.setFullScreenWindow(fullFrame);

        // look for any input - get out of full screen mode
        KeyEventDispatcher dispatcher = new MyKeyEventDispatcher();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher( dispatcher);
      }
    };
    BAMutil.setActionProperties(fullscreenAction, "Export", "fullscreen", true, 'N', -1);
    BAMutil.addActionToContainer(buttPanel, fullscreenAction);
  }

  private JFrame fullFrame;

  private class MyKeyEventDispatcher implements java.awt.KeyEventDispatcher {
    public boolean dispatchKeyEvent(KeyEvent e) {
      if (fullscreenMode) {
        System.out.println(" dispatchKeyEvent="+e);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();
        device.setFullScreenWindow(null);
        fullscreenMode = false;
      }

      resetPane();
      if (fullFrame != null) fullFrame.dispose();

      // deregister yourself
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher( this);
      return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }

  private void resetPane() {
    add( pixPane, BorderLayout.CENTER);
    revalidate();
  }

  public void setImageFromGrid( GridDatatype grid) {
      try {
        BufferedImage image = imageDatasetFactory.openDataset( grid);
        setImage( image);

      } catch (Exception e2) {
        javax.swing.JOptionPane.showMessageDialog(null, "Error on dataset\n"+ imageDatasetFactory.getErrorMessages());
        e2.printStackTrace();
      }
  }


   public boolean setImageFromUrl( String location) {
     this.location = location;

     if (location.startsWith("http")) {
       URL url = null;
       try {
         url = new URL( location);
       } catch (MalformedURLException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
       }

       // uses ImageIO.createImageInputStream()
       pixPane.setPicture( url, "legendParam", 0.0);

     } else {

      try {
        BufferedImage image = imageDatasetFactory.open( location);
        if (image == null) {
          javax.swing.JOptionPane.showMessageDialog(null, "Cant open dataset as image = "+location+"\n"+ imageDatasetFactory.getErrorMessages());
          return false;
        }

        setImage( image);

      } catch (Exception e2) {
        javax.swing.JOptionPane.showMessageDialog(null, "Error on dataset = "+location+"\n"+ imageDatasetFactory.getErrorMessages());
        e2.printStackTrace();
        return false;
      }
    }

    return true;
  }

  public void setImage( BufferedImage im) {
    if (im == null) return;
    if (debug) System.out.println("ImageViewPanel setImage ");
    pixPane.setBufferedImage( im, "setImage");
    pixPane.repaint();
  }

  static public void main( String[] args) {
      String[] readers = javax.imageio.ImageIO.getReaderFormatNames();
      for (int i = 0; i < readers.length; i++) {
        System.out.println(" reader = "+ readers[i]);
      }
      System.out.println("-------------------------------------------");
      String[] readerMimes = javax.imageio.ImageIO.getReaderMIMETypes();
      for (int i = 0; i < readerMimes.length; i++) {
        System.out.println(" readerMimes = "+ readerMimes[i]);
      }
      System.out.println("-------------------------------------------");
      String[] writers = javax.imageio.ImageIO.getWriterFormatNames();
      for (int i = 0; i < writers.length; i++) {
        System.out.println(" writers = "+ writers[i]);
      }
      System.out.println("-------------------------------------------");
      String[] writerMimes = javax.imageio.ImageIO.getWriterMIMETypes();
      for (int i = 0; i < writerMimes.length; i++) {
        System.out.println(" writerMimes = "+ writerMimes[i]);
      }
      System.out.println("-------------------------------------------");
  }

}

