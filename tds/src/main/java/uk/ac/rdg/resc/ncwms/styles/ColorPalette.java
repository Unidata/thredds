/*
 * Copyright (c) 2008 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.styles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import uk.ac.rdg.resc.ncwms.metadata.Layer;

/**
 * A palette of colours that is used by an {@link ImageProducer} to render 
 * data into a BufferedImage
 * @author Jon
 */
public class ColorPalette
{
    private static org.slf4j.Logger logger =
              org.slf4j.LoggerFactory.getLogger(ColorPalette.class);
    
    private static final Map<String, ColorPalette> palettes =
        new HashMap<String, ColorPalette>();
    
    /**
     * The name of the default palette that will be used if the user doesn't 
     * request a specific palette.
     * @see DEFAULT_PALETTE
     */
    private static final String DEFAULT_PALETTE_NAME = "rainbow";
    
    /**
     * The width of the legend in pixels that will be created by createLegend()
     */
    public static final int LEGEND_WIDTH = 110;
    /**
     * The height of the legend in pixels that will be created by createLegend()
     */
    public static final int LEGEND_HEIGHT = 264;
    
    /**
     * This is the palette that will be used if no specific palette has been
     * chosen.  This palette is taken from the SGT graphics toolkit.
     * @see DEFAULT_PALETTE_NAME
     */
    private static final ColorPalette DEFAULT_PALETTE = new ColorPalette(new Color[] {
        new Color(0,0,143), new Color(0,0,159), new Color(0,0,175),
        new Color(0,0,191), new Color(0,0,207), new Color(0,0,223),
        new Color(0,0,239), new Color(0,0,255), new Color(0,11,255),
        new Color(0,27,255), new Color(0,43,255), new Color(0,59,255),
        new Color(0,75,255), new Color(0,91,255), new Color(0,107,255),
        new Color(0,123,255), new Color(0,139,255), new Color(0,155,255),
        new Color(0,171,255), new Color(0,187,255), new Color(0,203,255),
        new Color(0,219,255), new Color(0,235,255), new Color(0,251,255),
        new Color(7,255,247), new Color(23,255,231), new Color(39,255,215),
        new Color(55,255,199), new Color(71,255,183), new Color(87,255,167),
        new Color(103,255,151), new Color(119,255,135), new Color(135,255,119),
        new Color(151,255,103), new Color(167,255,87), new Color(183,255,71),
        new Color(199,255,55), new Color(215,255,39), new Color(231,255,23),
        new Color(247,255,7), new Color(255,247,0), new Color(255,231,0),
        new Color(255,215,0), new Color(255,199,0), new Color(255,183,0),
        new Color(255,167,0), new Color(255,151,0), new Color(255,135,0),
        new Color(255,119,0), new Color(255,103,0), new Color(255,87,0),
        new Color(255,71,0), new Color(255,55,0), new Color(255,39,0),
        new Color(255,23,0), new Color(255,7,0), new Color(246,0,0),
        new Color(228,0,0), new Color(211,0,0), new Color(193,0,0),
        new Color(175,0,0), new Color(158,0,0), new Color(140,0,0)
    });
    
    private Color[] palette;
    
    private ColorPalette(Color[] palette)
    {
        this.palette = palette;
    }
    
    /**
     * Gets the number of colours in this palette
     * @return the number of colours in this palette
     */
    public int getSize()
    {
        return this.palette.length;
    }
    
    /**
     * Gets the names of the supported palettes.
     * @return the names of the palettes as a Set of Strings.  All Strings
     * will be in lower case.
     */
    public static final Set<String> getAvailablePaletteNames()
    {
        return palettes.keySet();
    }
    
    /**
     * This is called by WMSController on initialization to load all the palettes
     * in the WEB-INF/conf/palettes directory.  This will attempt to load all files
     * with the file extension ".pal".
     * @param paletteLocationDir Directory containing the palette files.  This
     * has already been checked to exist and be a directory
     */
    public static final void loadPalettes(File paletteLocationDir)
    {
        for (File file : paletteLocationDir.listFiles())
        {
            if (file.getName().endsWith(".pal"))
            {
                try
                {
                    ColorPalette palette = new ColorPalette(readColorPalette(file));
                    String paletteName = file.getName().substring(0, file.getName().lastIndexOf("."));
                    logger.debug("Read palette with name {}", paletteName);
                    palettes.put(paletteName.toLowerCase(), palette);
                }
                catch(Exception e)
                {
                    logger.error("Error reading from palette file {}", file.getName(), e);
                }
            }
        }
        // If we don't already have a default palette, add one
        if (!palettes.containsKey(DEFAULT_PALETTE_NAME))
        {
            palettes.put(DEFAULT_PALETTE_NAME, DEFAULT_PALETTE);
        }
    }
    
    /**
     * Gets the palette with the given name.
     * @param name Name of the palette, corresponding with the name of the
     * palette file in WEB-INF/conf/palettes. Case insensitive.
     * @return the ColorPalette object, or null if there is no palette with
     * the given name.  If name is null or the empty string this will return
     * the default palette.
     */
    public static ColorPalette get(String name)
    {
        if (name == null || name.trim().equals(""))
        {
            return palettes.get(DEFAULT_PALETTE_NAME);
        }
        return palettes.get(name.trim().toLowerCase());
    }
    
    /**
     * Creates a color bar with the given width and height and the given number
     * of color bands.  The color bar will consist of horizontal stripes of color,
     * with the first color at the bottom. Clients that wish to display the bar
     * horizontally should rotate the image clockwise through ninety degrees.
     * @param width The width of the requested color bar in pixels
     * @param height The height of the requested color bar in pixels
     * @param numColorBands The number of bands of color to include in the bar
     * @return a new BufferedImage
     */
    public BufferedImage createColorBar(int width, int height, int numColorBands)
    {
        double colorBandWidth = (double)height / numColorBands;
        // Get an interpolated/subsampled palette for the color bar
        Color[] newPalette = this.getPalette(numColorBands);
        // Create a BufferedImage of the correct size - we don't need the alpha channel
        BufferedImage colorBar = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D gfx = colorBar.createGraphics();
        // Cycle through each row in the image and draw a band of the
        // appropriate colour.
        for (int i = 0; i < height; i++)
        {
            int colorIndex = (int)(i / colorBandWidth);
            // The colours at the end of the palette need to be at the top
            // of the image
            gfx.setColor(newPalette[numColorBands - colorIndex - 1]);
            gfx.drawLine(0, i, width - 1, i);
        }
        return colorBar;
    }
    
    /**
     * Creates and returns a BufferedImage representing the legend for this 
     * palette
     * @param numColorBands The number of color bands to show in the legend
     * @param layer Layer for which the legend is being created
     * @param logarithmic True if the scale is to be logarithmic: otherwise linear
     * @param colourScaleMin Data value corresponding to the bottom of the colour
     * scale.  If both this and colourScaleMax are zero then the default scale
     * range for the layer is used.
     * @param colourScaleMax Data value corresponding to the top of the colour
     * scale.  If both this and colourScaleMin are zero then the default scale
     * range for the layer is used.
     * @return a BufferedImage object representing the legend.  This has a fixed
     * size (110 pixels wide, 264 pixels high)
     * @throws IllegalArgumentException if the requested number of colour bands
     * is less than one or greater than 254.
     */
    public BufferedImage createLegend(int numColorBands, Layer layer,
        boolean logarithmic, float colourScaleMin, float colourScaleMax)
    {
        BufferedImage colourScale = new BufferedImage(LEGEND_WIDTH,
            LEGEND_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D gfx = colourScale.createGraphics();
        
        // Create the colour bar itself
        BufferedImage colorBar = this.createColorBar(24, 254, numColorBands);
        // Add the colour bar to the legend
        gfx.drawImage(colorBar, null, 2, 5);
        
        // Draw the text items
        gfx.setColor(Color.WHITE);
        // Add the scale values
        // Test to see if we are auto-scaling
        if (colourScaleMin == 0.0f && colourScaleMax == 0.0f)
        {
            logger.debug("colourscales ARE ZEROS: " + colourScaleMin  + " : " + colourScaleMax);
            colourScaleMin = layer.getScaleMin();
            colourScaleMax = layer.getScaleMax();
            logger.debug("changed to layer min/max: " + colourScaleMin  + " : " + colourScaleMax);
        }
        else
        {
            logger.debug("colourScaleMinMax are not ZEROS: " + colourScaleMin + ", " + colourScaleMax);
        }
        double min = logarithmic ? Math.log(colourScaleMin) : colourScaleMin;
        double max = logarithmic ? Math.log(colourScaleMax) : colourScaleMax;
        double quarter = 0.25 * (max - min);
        double scaleQuarter = logarithmic ? Math.exp(min + quarter) : min + quarter;
        double scaleMid = logarithmic ? Math.exp(min + 2 * quarter) : min + 2 * quarter;
        double scaleThreeQuarter = logarithmic ? Math.exp(min + 3 * quarter) : min + 3 * quarter;
        gfx.drawString(format(colourScaleMax), 27, 10);
        gfx.drawString(format(scaleThreeQuarter), 27, 73);
        gfx.drawString(format(scaleMid), 27, 137);
        gfx.drawString(format(scaleQuarter), 27, 201);
        gfx.drawString(format(colourScaleMin), 27, 264);
        
        // Add the title as rotated text
        String title = layer.getTitle();
        if (layer.getUnits() != null && !layer.getUnits().trim().equals(""))
        {
            title += " (" + layer.getUnits() + ")";
        }
        AffineTransform trans = new AffineTransform();
        trans.setToTranslation(90, 0);
        AffineTransform rot = new AffineTransform();
        rot.setToRotation(Math.PI / 2.0);
        trans.concatenate(rot);
        gfx.setTransform(trans);
        gfx.drawString(title, 5, 0);

        return colourScale;
    }
    
    /**
     * Formats a number to a limited number of d.p., using scientific notation
     * if necessary
     */
    private static String format(double d)
    {
        if (d == 0.0) return "0";
        if (Math.abs(d) > 1000 || Math.abs(d) < 0.01)
        {
            return new DecimalFormat("0.###E0").format(d);
        }
        return new DecimalFormat("0.#####").format(d);
    }
    
    /**
     * Creates and returns an IndexColorModel based on this palette.
     * @param numColorBands the number of bands of colour to use in the color
     * model (note that the ColorModel will have two more bands than this: one
     * for out-of-range pixels and one for transparent pixels)
     * @param opacity The opacity of each pixel as a percentage
     * @param bgColor The color to use for background pixels if transparent=false
     * @param transparent If true, then the background will be fully-transparent.
     * @throws IllegalArgumentException if the requested number of colour bands
     * is less than one or greater than 254.
     */
    public IndexColorModel getColorModel(int numColorBands, int opacity,
        Color bgColor, boolean transparent)
    {
        // Gets an interpolated/subsampled version of this palette with the
        // given number of colour bands
        Color[] newPalette = this.getPalette(numColorBands);
        // Compute the alpha value based on the percentage transparency
        int alpha;
        // Here we are playing safe and avoiding rounding errors that might
        // cause the alpha to be set to zero instead of 255
        if (opacity >= 100) alpha = 255;
        else if (opacity <= 0)  alpha = 0;
        else alpha = (int)(2.55 * opacity);

        // Now simply copy the target palette to arrays of r,g,b and a
        byte[] r = new byte[numColorBands + 2];
        byte[] g = new byte[numColorBands + 2];
        byte[] b = new byte[numColorBands + 2];
        byte[] a = new byte[numColorBands + 2];
        for (int i = 0; i < numColorBands; i++)
        {
            r[i] = (byte)newPalette[i].getRed();
            g[i] = (byte)newPalette[i].getGreen();
            b[i] = (byte)newPalette[i].getBlue();
            a[i] = (byte)alpha;
        }

        // The next index represents the background colour (which may be transparent)
        r[numColorBands] = (byte)bgColor.getRed();
        g[numColorBands] = (byte)bgColor.getGreen();
        b[numColorBands] = (byte)bgColor.getBlue();
        a[numColorBands] = transparent ? 0 : (byte)alpha;

        // The next represents out-of-range pixels (black)
        r[numColorBands + 1] = 0;
        g[numColorBands + 1] = 0;
        b[numColorBands + 1] = 0;
        a[numColorBands + 1] = (byte)alpha;

        // Now we can create the color model
        return new IndexColorModel(8, r.length, r, g, b, a);
    }
    
    /**
     * Gets a version of this palette with the given number of color bands,
     * either by subsampling or interpolating the existing palette
     * @param numColorBands The number of bands of colour to be used in the new
     * palette
     * @return An array of Colors, with length numColorBands
     * @throws IllegalArgumentException if the requested number of colour bands
     * is less than one or greater than 254.
     */
    private Color[] getPalette(int numColorBands)
    {
        if (numColorBands < 1 || numColorBands > 254)
        {
            // Shouldn't happen: we have constrained this to a sane value in
            // GetMapStyleRequest
            throw new IllegalArgumentException("numColorBands must be between 1 and 254");
        }
        Color[] targetPalette;
        if (numColorBands == this.palette.length)
        {
            // We can just use the source palette directly
            targetPalette = this.palette;
        }
        else
        {
            // We need to create a new palette
            targetPalette = new Color[numColorBands];
            // We fix the endpoints of the target palette to the endpoints of the source palette
            targetPalette[0] = this.palette[0];
            targetPalette[targetPalette.length - 1] = this.palette[this.palette.length - 1];

            if (targetPalette.length < this.palette.length)
            {
                // We only need some of the colours from the source palette
                // We search through the target palette and find the nearest colours
                // in the source palette
                for (int i = 1; i < targetPalette.length - 1; i++)
                {
                    // Find the nearest index in the source palette
                    // (Multiplying by 1.0f converts integers to floats)
                    int nearestIndex = Math.round(this.palette.length * i * 1.0f / (targetPalette.length - 1));
                    targetPalette[i] = this.palette[nearestIndex];
                }
            }
            else
            {
                // Transfer all the colours from the source palette into their corresponding
                // positions in the target palette and use interpolation to find the remaining
                // values
                int lastIndex = 0;
                for (int i = 1; i < this.palette.length - 1; i++)
                {
                    // Find the nearest index in the target palette
                    int nearestIndex = Math.round(targetPalette.length * i * 1.0f / (this.palette.length - 1));
                    targetPalette[nearestIndex] = this.palette[i];
                    // Now interpolate all the values we missed
                    for (int j = lastIndex + 1; j < nearestIndex; j++)
                    {
                        // Work out how much we need from the previous colour and how much
                        // from the new colour
                        float fracFromThis = (1.0f * j - lastIndex) / (nearestIndex - lastIndex);
                        targetPalette[j] = interpolate(targetPalette[nearestIndex],
                            targetPalette[lastIndex], fracFromThis);
                    }
                    lastIndex = nearestIndex;
                }
                // Now for the last bit of interpolation
                for (int j = lastIndex + 1; j < targetPalette.length - 1; j++)
                {
                    float fracFromThis = (1.0f * j - lastIndex) / (targetPalette.length - lastIndex);
                    targetPalette[j] = interpolate(targetPalette[targetPalette.length - 1],
                        targetPalette[lastIndex], fracFromThis);
                }
            }
        }
        return targetPalette;
    }
    
    /**
     * Linearly interpolates between two RGB colours
     * @param c1 the first colour
     * @param c2 the second colour
     * @param fracFromC1 the fraction of the final colour that will come from c1
     * @return the interpolated Color
     */
    private static Color interpolate(Color c1, Color c2, float fracFromC1)
    {
        float fracFromC2 = 1.0f - fracFromC1;
        return new Color(
            Math.round(fracFromC1 * c1.getRed() + fracFromC2 * c2.getRed()),
            Math.round(fracFromC1 * c1.getGreen() + fracFromC2 * c2.getGreen()),
            Math.round(fracFromC1 * c1.getBlue() + fracFromC2 * c2.getBlue())
        );
    }
    
    /**
     * Reads a colour palette (as an array of Color object) from the given File.
     * Each line in the file contains a single colour, expressed as space-separated
     * RGB values.  These values can be integers in the range 0->255 or floats
     * in the range 0->1.  If the palette cannot be read, no exception is thrown
     * but an event is logged to the error log.
     * @throws Exception if the palette file could not be read or contains a
     * format error
     */
    private static Color[] readColorPalette(File paletteFile) throws Exception
    {
        BufferedReader reader = new BufferedReader(new FileReader(paletteFile));
        List<Color> colours = new ArrayList<Color>();
        String line;
        try
        {
            while((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.trim().equals(""))
                {
                    continue; // Skip comment lines and blank lines
                }
                StringTokenizer tok = new StringTokenizer(line.trim());
                try
                {
                    if (tok.countTokens() < 3) throw new Exception();
                    // We only read the first three tokens
                    Float r = Float.valueOf(tok.nextToken());
                    Float g = Float.valueOf(tok.nextToken());
                    Float b = Float.valueOf(tok.nextToken());
                    // Check for negative numbers
                    if (r < 0.0f || g < 0.0f || b < 0.0f) throw new Exception();
                    if (r > 1.0f || g > 1.0f || b > 1.0f)
                    {
                        // We assume this colour is expressed in the range 0->255
                        if (r > 255.0f || g > 255.0f || b > 255.0f) throw new Exception();
                        colours.add(new Color(r.intValue(), g.intValue(), b.intValue()));
                    }
                    else
                    {
                        // the colours are expressed in the range 0->1
                        colours.add(new Color(r, g, b));
                    }
                }
                catch(Exception e)
                {
                    throw new Exception("File format error: each line must contain three numbers between 0 and 255 or 0.0 and 1.0 (R, G, B)");
                }
            }
        }
        finally
        {
            if (reader != null) reader.close();
        }
        return colours.toArray(new Color[0]);
    }

}
