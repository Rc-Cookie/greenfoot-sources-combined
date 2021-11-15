package greenfoot.util;
/*
 * Copyright (c) 2007, Romain Guy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the TimingFramework project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * <p><code>GraphicsUtilities</code> contains a set of tools to perform
 * common graphics operations easily. These operations are divided into
 * several themes, listed below.</p>
 * <h2>Compatible Images</h2>
 * <p>Compatible images can, and should, be used to increase drawing
 * performance. This class provides a number of methods to load compatible
 * images directly from files or to convert existing images to compatibles
 * images.</p>
 * <h2>Creating Thumbnails</h2>
 * <p>This class provides a number of methods to easily scale down images.
 * Some of these methods offer a trade-off between speed and result quality and
 * shouuld be used all the time. They also offer the advantage of producing
 * compatible images, thus automatically resulting into better runtime
 * performance.</p>
 * <p>All these methodes are both faster than
 * {@link java.awt.Image#getScaledInstance(int, int, int)} and produce
 * better-looking results than the various <code>drawImage()</code> methods
 * in {@link java.awt.Graphics}, which can be used for image scaling.</p>
 * <h2>Image Manipulation</h2>
 * <p>This class provides two methods to get and set pixels in a buffered image.
 * These methods try to avoid unmanaging the image in order to keep good
 * performance.</p>
 *
 * @author Romain Guy <romain.guy@mac.com>
 */
public class GraphicsUtilities {

    private GraphicsUtilities() {
    }

    // Returns the graphics configuration for the primary screen
    private static GraphicsConfiguration getGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * <p>Returns a new <code>BufferedImage</code> using the same color model
     * as the image passed as a parameter. The returned image is only compatible
     * with the image passed as a parameter. This does not mean the returned
     * image is compatible with the hardware.</p>
     *
     * @param image the reference image from which the color model of the new
     *   image is obtained
     * @return a new <code>BufferedImage</code>, compatible with the color model
     *   of <code>image</code>
     */
    public static BufferedImage createColorModelCompatibleImage(BufferedImage image) {
        ColorModel cm = image.getColorModel();
        return new BufferedImage(cm,
            cm.createCompatibleWritableRaster(image.getWidth(),
                                              image.getHeight()),
            cm.isAlphaPremultiplied(), null);
    }

    /**
     * <p>Returns a new compatible image with the same width, height and
     * transparency as the image specified as a parameter.</p>
     *
     * @see java.awt.Transparency
     * @see #createCompatibleImage(int, int)
     * @see #createCompatibleImage(java.awt.image.BufferedImage, int, int)
     * @see #createCompatibleTranslucentImage(int, int)
     * @see #loadCompatibleImage(java.net.URL)
     * @see #toCompatibleImage(java.awt.image.BufferedImage)
     * @param image the reference image from which the dimension and the
     *   transparency of the new image are obtained
     * @return a new compatible <code>BufferedImage</code> with the same
     *   dimension and transparency as <code>image</code>
     */
    public static BufferedImage createCompatibleImage(BufferedImage image) {
        return createCompatibleImage(image, image.getWidth(), image.getHeight());
    }

    /**
     * <p>Returns a new compatible image of the specified width and height, and
     * the same transparency setting as the image specified as a parameter.</p>
     *
     * @see java.awt.Transparency
     * @see #createCompatibleImage(java.awt.image.BufferedImage)
     * @see #createCompatibleImage(int, int)
     * @see #createCompatibleTranslucentImage(int, int)
     * @see #loadCompatibleImage(java.net.URL)
     * @see #toCompatibleImage(java.awt.image.BufferedImage)
     * @param width the width of the new image
     * @param height the height of the new image
     * @param image the reference image from which the transparency of the new
     *   image is obtained
     * @return a new compatible <code>BufferedImage</code> with the same
     *   transparency as <code>image</code> and the specified dimension
     */
    public static BufferedImage createCompatibleImage(BufferedImage image,
                                                      int width, int height) {
        return getGraphicsConfiguration().createCompatibleImage(width, height,
                                                   image.getTransparency());
    }

    /**
     * <p>Returns a new opaque compatible image of the specified width and
     * height.</p>
     *
     * @see #createCompatibleImage(java.awt.image.BufferedImage)
     * @see #createCompatibleImage(java.awt.image.BufferedImage, int, int)
     * @see #createCompatibleTranslucentImage(int, int)
     * @see #loadCompatibleImage(java.net.URL)
     * @see #toCompatibleImage(java.awt.image.BufferedImage)
     * @param width the width of the new image
     * @param height the height of the new image
     * @return a new opaque compatible <code>BufferedImage</code> of the
     *   specified width and height
     */
    public static BufferedImage createCompatibleImage(int width, int height) {
        return getGraphicsConfiguration().createCompatibleImage(width, height);
    }

    /**
     * <p>Returns a new translucent compatible image of the specified width
     * and height.</p>
     *
     * @see #createCompatibleImage(java.awt.image.BufferedImage)
     * @see #createCompatibleImage(java.awt.image.BufferedImage, int, int)
     * @see #createCompatibleImage(int, int)
     * @see #loadCompatibleImage(java.net.URL)
     * @see #toCompatibleImage(java.awt.image.BufferedImage)
     * @param width the width of the new image
     * @param height the height of the new image
     * @return a new translucent compatible <code>BufferedImage</code> of the
     *   specified width and height
     */
    public static BufferedImage createCompatibleTranslucentImage(int width,
                                                                 int height) {
        return getGraphicsConfiguration().createCompatibleImage(width, height,
                                                   Transparency.TRANSLUCENT);
    }

    /**
     * <p>Returns a new compatible image from a URL. The image is loaded from the
     * specified location and then turned, if necessary into a compatible
     * image.</p>
     *
     * @see #createCompatibleImage(java.awt.image.BufferedImage)
     * @see #createCompatibleImage(java.awt.image.BufferedImage, int, int)
     * @see #createCompatibleImage(int, int)
     * @see #createCompatibleTranslucentImage(int, int)
     * @see #toCompatibleImage(java.awt.image.BufferedImage)
     * @param resource the URL of the picture to load as a compatible image
     * @return a new translucent compatible <code>BufferedImage</code> of the
     *   specified width and height
     * @throws java.io.IOException if the image cannot be read or loaded
     */
    public static BufferedImage loadCompatibleImage(URL resource)
            throws IOException {
        BufferedImage image = ImageIO.read(resource);
        if (image == null) {
            throw new IOException("Image format of resource not supported. Resource: " + resource);
        }
        return toCompatibleImage(image);
    }
    
    /**
     * <p>Returns a new compatible image from a URL. The image is loaded from the
     * specified location and then turned, if necessary into a translucent compatible
     * image.</p>
     *
     * @see #createCompatibleImage(java.awt.image.BufferedImage)
     * @see #createCompatibleImage(java.awt.image.BufferedImage, int, int)
     * @see #createCompatibleImage(int, int)
     * @see #createCompatibleTranslucentImage(int, int)
     * @see #toCompatibleImage(java.awt.image.BufferedImage)
     * @param resource the URL of the picture to load as a compatible image
     * @return a new translucent compatible <code>BufferedImage</code> of the
     *   specified width and height
     * @throws java.io.IOException if the image cannot be read or loaded
     */
    public static BufferedImage loadCompatibleTranslucentImage(URL resource)
            throws IOException {
        BufferedImage image = ImageIO.read(resource);
        if (image == null) {
            throw new IOException("Image format of resource not supported. Resource: " + resource);
        }
        return toCompatibleTranslucentImage(image);
    }
    
    // As above, but uses given file contents rather than path
    public static BufferedImage loadCompatibleTranslucentImage(byte[] imageData)
        throws IOException
    {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            throw new IOException("Image format of byte data not supported");
        }
        return toCompatibleTranslucentImage(image);
    }

    /**
     * <p>Return a new compatible image that contains a copy of the specified
     * image. This method ensures an image is compatible with the hardware,
     * and therefore optimized for fast blitting operations.</p>
     *
     * @see #createCompatibleImage(java.awt.image.BufferedImage)
     * @see #createCompatibleImage(java.awt.image.BufferedImage, int, int)
     * @see #createCompatibleImage(int, int)
     * @see #createCompatibleTranslucentImage(int, int)
     * @see #loadCompatibleImage(java.net.URL)
     * @param image the image to copy into a new compatible image
     * @return a new compatible copy, with the
     *   same width and height and transparency and content, of <code>image</code>
     */
    public static BufferedImage toCompatibleImage(BufferedImage image) {
        if (image.getColorModel().equals(
                getGraphicsConfiguration().getColorModel())) {
            return image;
        }

        BufferedImage compatibleImage =
                getGraphicsConfiguration().createCompatibleImage(
                    image.getWidth(), image.getHeight(),
                    image.getTransparency());
        Graphics g = compatibleImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return compatibleImage;
    }
    
    /**
     * <p>
     * Return a new compatible image that contains a copy of the specified
     * image. This method ensures an image is compatible with the hardware, and
     * therefore optimized for fast blitting operations. It also ensures that
     * the image is translucent.
     * </p>
     * 
     * @see #createCompatibleImage(java.awt.image.BufferedImage)
     * @see #createCompatibleImage(java.awt.image.BufferedImage, int, int)
     * @see #createCompatibleImage(int, int)
     * @see #createCompatibleTranslucentImage(int, int)
     * @see #loadCompatibleImage(java.net.URL)
     * @param image the image to copy into a new compatible image
     * @return a new compatible copy, with the same width and height and
     *         transparency and content, of <code>image</code>
     */
    public static BufferedImage toCompatibleTranslucentImage(BufferedImage image)
    {
        if (image.getColorModel().equals(getGraphicsConfiguration().getColorModel())
                && image.getColorModel().hasAlpha()) {
            return image;
        }

        BufferedImage compatibleImage = getGraphicsConfiguration().createCompatibleImage(
                    image.getWidth(), image.getHeight(),
                    Transparency.TRANSLUCENT);
        Graphics g = compatibleImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return compatibleImage;
    }

    /**
     * Given the dimensions of a series of lines of text, draws those shapes using the given colours.
     * @param g The Graphics context to use for drawing.
     * @param d The dimensions of the lines to be drawn (from getMultiLineStringDimensions)
     * @param foreground The colour to draw the foreground in.  BLACK is used if null
     * @param outline The colour to draw the outline in.  Not drawn if null
     */
    public static void drawOutlinedText(Graphics2D g, MultiLineStringDimensions d, Color foreground, Color outline)
    {
        if (foreground == null)
            foreground = Color.BLACK;
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for (int i = 0; i < d.lineShapes.length;i++) {
            g.setColor(foreground);
            //
            g.fill(d.lineShapes[i]);
            if (outline != null)
            {
                g.setColor(outline);
                g.draw(d.lineShapes[i]);
            }
                
        }
    }

    /**
     * Sets the font on the given graphics context to have the given style and target size
     * @param g
     * @param style The font style (e.g. Font.PLAIN)
     * @param targetSize The target height of the line
     */
    private static void setFontOfPixelHeight(Graphics2D g, int style, double targetSize)
    {
        // Likely DPI ranges for a monitor: 120 to 500 pixels per inch (via wikipedia)
        // An inch is 72 points, so range is something like 1 pixel per point to 8 pixels per point
        // So we explore from 1 point, up to the desired pixel size in points.
        // e.g. if we want 40 pixels, then a 40 point font is going to be bigger than 40 pixels if the display is above 72 DPI
        Font font = new Font("SansSerif", style, 1);
        
        for (int i = 1; i < targetSize; i++)
        {
            Font bigger = font.deriveFont((float)i);
            g.setFont(bigger);
            // This string should be full height in the font:
            if (bigger.getLineMetrics("WBLMNqpyg", g.getFontRenderContext()).getHeight() < targetSize) // getStringHeight(g, "WBLMNqpyg") < targetSize)
            {
                font = bigger;
            }
            else
            {
                break; // Too big; keep previous
            }
        }
        g.setFont(font);
    
    }

    // Splits lines by newlines, and strips \r:
    public static String[] splitLines(String string)
    {
        return string.replaceAll("\r", "").split("\n");
    }

    /**
     * Given a list of lines, gets the dimensions/outlines of the lines when drawn in the given style,
     * one above the other, horizontally centred.  The shapes will be drawn relative to the
     * top-left of the image.
     * 
     * @param lines The text lines to draw.  Should be the output of splitLines
     * @param style The style (e.g. Font.PLAIN)
     * @param size The height in pixels of each line of text
     * @return The dimensions of the lines
     */
    public static MultiLineStringDimensions getMultiLineStringDimensions(String[] lines, int style, double size)
    {
        BufferedImage image = createCompatibleTranslucentImage(1, 1);
        MultiLineStringDimensions r = new MultiLineStringDimensions(lines.length);
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        setFontOfPixelHeight(g, style, size);
        
        FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D[] lineBounds = new Rectangle2D[lines.length];
        int maxX = 1;
        int y = 0;
        for (int i = 0; i < lines.length;i++)
        {
            lineBounds[i] = g.getFontMetrics().getStringBounds(lines[i], g);
            maxX = Math.max(maxX, (int)Math.ceil(lineBounds[i].getWidth()));
            y += Math.ceil(lineBounds[i].getHeight());
        }
        y = Math.max(y + 1, 1);
        r.overallBounds = new Dimension(maxX, y);
        
        y = 0;
        for (int i = 0; i < lines.length;i++)
        {
            // Draw the shape in the right space in the overall text, by translating it down and moving to middle:
            AffineTransform translate = AffineTransform.getTranslateInstance((r.overallBounds.getWidth() - lineBounds[i].getWidth()) / 2, y - lineBounds[i].getMinY() /* add on to baseline */);
            r.lineShapes[i] = new TextLayout(!lines[i].isEmpty()? lines[i] : " ", g.getFont(), frc).getOutline(translate);
            y += Math.ceil(lineBounds[i].getHeight());
        }
        // Make it at least one pixel, and add one for the outline width:
        
        g.dispose();
        
        return r;
    }
    
    public static class MultiLineStringDimensions
    {
        private Shape[] lineShapes;
        private Dimension overallBounds;
        
        public MultiLineStringDimensions(int length)
        {
            lineShapes = new Shape[length];
        }
        
        public int getWidth()
        {
            return overallBounds.width;
        }
        
        public int getHeight()
        {
            return overallBounds.height;
        }
    }
}
