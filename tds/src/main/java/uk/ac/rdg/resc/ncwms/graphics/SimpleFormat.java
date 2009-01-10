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

package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import uk.ac.rdg.resc.ncwms.metadata.Layer;

/**
 * Abstract superclass for simple image formats that do not require information
 * about the layer, time values, bounding box etc to render an image.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class SimpleFormat extends ImageFormat
{
    /**
     * Returns false: simple formats do not require a lagend.
     */
    @Override
    public final boolean requiresLegend()
    {
        return false;
    }
    
    /**
     * Delegates to writeImage(frames, out), ignoring most of the parameters.
     * @param frames List of BufferedImages to render into an image
     * @param out The OutputStream to which the image will be written
     * @param layer the Layer object representing the image(s)
     * @param tValues List of Strings representing the time values, one for each frame
     * @param zValue The elevation value representing the image(s)
     * @param bbox The bounding box of the image(s)
     * @param legend A legend image (this will be null unless this.requiresLegend()
     * returns true.
     * @throws IOException if there was an error writing to the output stream
     * @throws IllegalArgumentException if this ImageFormat cannot render all
     * of the given BufferedImages.
     */
    @Override
    public final void writeImage(List<BufferedImage> frames,
        OutputStream out, Layer layer, List<String> tValues,
        String zValue, double[] bbox, BufferedImage legend) throws IOException
    {
        this.writeImage(frames, out);
    }
    
    /**
     * Writes the given list of {@link java.awt.BufferedImage}s to the given
     * OutputStream.  If this ImageFormat doesn't support animations then the
     * given list of frames should only contain one entry, otherwise an
     * IllegalArgumentException will be thrown.
     * @param frames List of BufferedImages to render into an image
     * @param out The OutputStream to which the image will be written
     * @throws IOException if there was an error writing to the output stream
     * @throws IllegalArgumentException if this ImageFormat cannot render all
     * of the given BufferedImages.
     */
    public abstract void writeImage(List<BufferedImage> frames,
        OutputStream out) throws IOException;
}
