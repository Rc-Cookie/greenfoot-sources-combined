/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2016,2017,2018  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr.target;

import bluej.pkgmgr.Package;
import bluej.utility.javafx.JavaFXUtil;

import java.util.Properties;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Labeled;
import javafx.scene.Cursor;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A general target in a package
 * 
 * @author Michael Cahill
 */
public abstract class Target
    implements Comparable<Target>
{
    static final int DEF_WIDTH = 80;
    static final int DEF_HEIGHT = 50;
    static final int ARR_HORIZ_DIST = 5;
    static final int ARR_VERT_DIST = 10;
    static final int HANDLE_SIZE = 20;
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 4;
    static final int SHAD_SIZE = 4;
    protected static final double RESIZE_CORNER_SIZE = 16;

    // Store the position before moving, and size before resizing.
    // Not because we allow cancelling (we don't), but because we move/resize
    // by deltas, so we need to know the start position/size.
    @OnThread(Tag.FXPlatform)
    private int preMoveX;
    @OnThread(Tag.FXPlatform)
    private int preMoveY;
    @OnThread(Tag.FXPlatform)
    private int preResizeWidth;
    @OnThread(Tag.FXPlatform)
    private int preResizeHeight;
    // Keeps track of whether a mouse button press at the current position
    // would be a resize.
    @OnThread(Tag.FXPlatform)
    private boolean pressIsResize;
    // The position of the mouse press (e.g. for positioning/sizing)
    // relative to the pane:
    @OnThread(Tag.FXPlatform)
    private double pressDeltaX;
    @OnThread(Tag.FXPlatform)
    private double pressDeltaY;
    // The currently showing context menu (if any).  Null if no menu showing.
    @OnThread(Tag.FXPlatform)
    private ContextMenu showingContextMenu;

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private String identifierName; // the name handle for this target within
    // this package (must be unique within this
    // package)
    private String displayName; // displayed name of the target
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Package pkg; // the package this target belongs to

    // Is the current node selected?
    @OnThread(Tag.Any)
    protected boolean selected;
    // Is the current node queued for compilation?
    protected boolean queued;

    // The graphical item in the class diagram
    @OnThread(Tag.FXPlatform)
    protected BorderPane pane = JavaFXUtil.initFX(BorderPane::new);
    // Is the target directly resizable?  Readmes and test classes are not.
    @OnThread(Tag.FX)
    private boolean resizable = true;

    /**
     * Create a new target with default size.
     */
    @OnThread(Tag.FXPlatform)
    public Target(Package pkg, String identifierName)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    private void updateCursor(MouseEvent e, boolean moving)
    {
        if (moving)
        {
            pane.setCursor(Cursor.MOVE);
        }
        else if (isSelected() && isResizable() && cursorAtResizeCorner(e))
        {
            pane.setCursor(Cursor.SE_RESIZE);
        }
        else
        {
            pane.setCursor(Cursor.HAND);

        }
    }

    @OnThread(Tag.FXPlatform)
    protected boolean cursorAtResizeCorner(MouseEvent e)
    {
        // Check if it's in the 45-degree corner in the bottom right:
        return e.getX() + e.getY() >= getWidth() + getHeight() - RESIZE_CORNER_SIZE;
    }

    /**
     * Calculate the width of a target depending on the length of its name and
     * the font used for displaying the name. The size returned is a multiple of
     * 10 (to fit the interactive resizing behaviour).
     * 
     * @param name
     *            the name of the target (may be null).
     * @return the width the target should have to fully display its name.
     */
    @OnThread(Tag.FX)
    protected static int calculateWidth(Labeled node, String name)
    {
        return 0;
    }
    
    /**
     * This target has been removed from its package.
     */
    public synchronized void setRemoved()
    {
        // This can be used to detect that a class target has been removed.
        pkg = null;
    }

    /**
     * Load this target's properties from a properties file. The prefix is an
     * internal name used for this target to identify its properties in a
     * properties file used by multiple targets.
     */
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        // No super.load, but need to get Vertex properties:
        int xpos = 0;
        int ypos = 0;
        int width = 20; // arbitrary fallback values
        int height = 10;
        
        // Try to get the positional properties in a robust manner.
        try {
            xpos = Math.max(Integer.parseInt(props.getProperty(prefix + ".x")), 0);
            ypos = Math.max(Integer.parseInt(props.getProperty(prefix + ".y")), 0);
            width = Math.max(Integer.parseInt(props.getProperty(prefix + ".width")), 1);
            height = Math.max(Integer.parseInt(props.getProperty(prefix + ".height")), 1);
        }
        catch (NumberFormatException nfe) {}

        setPos(xpos, ypos);
        setSize(width, height);
    }

    /**
     * Save the target's properties to 'props'.
     */
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        props.put(prefix + ".x", String.valueOf(getX()));
        props.put(prefix + ".y", String.valueOf(getY()));
        props.put(prefix + ".width", String.valueOf(getWidth()));
        props.put(prefix + ".height", String.valueOf(getHeight()));

        props.put(prefix + ".name", getIdentifierName());
    }

    /**
     * Return this target's package (ie the package that this target is
     * currently shown in)
     */
    @OnThread(Tag.Any)
    public synchronized Package getPackage()
    {
        return pkg;
    }

    /**
     * Change the text which the target displays for its label
     */
    public void setDisplayName(String name)
    {
        displayName = name;
    }

    /**
     * Returns the text which the target is displaying as its label
     */
    public String getDisplayName()
    {
        return displayName;
    }

    @OnThread(Tag.Any)
    public synchronized String getIdentifierName()
    {
        return identifierName;
    }

    public synchronized void setIdentifierName(String newName)
    {
        identifierName = newName;
    }

    /*
     * Sets the selected status of this target.  Do not call directly
     * to select us; instead call SelectionController/SelectionSet's methods,
     * which will call this after updating the selected set.
     */
    @OnThread(Tag.FXPlatform)
    public void setSelected(boolean selected)
    {
        this.selected = selected;
        JavaFXUtil.setPseudoclass("bj-selected", selected, pane);
        redraw();
    }

    @OnThread(Tag.FXPlatform)
    protected void redraw()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.graph.Selectable#isSelected()
     */
    @OnThread(Tag.Any)
    public boolean isSelected()
    {
        return selected;
    }

    @OnThread(Tag.FXPlatform)
    public int getX()
    {
        Double leftAnchor = AnchorPane.getLeftAnchor(pane);
        if (leftAnchor == null)
            return 0;
        else
            return (int)(double)leftAnchor;
    }

    @OnThread(Tag.FXPlatform)
    public int getY()
    {
        Double topAnchor = AnchorPane.getTopAnchor(pane);
        if (topAnchor == null)
            return 0;
        else
            return (int)(double)topAnchor;
    }

    @OnThread(Tag.FXPlatform)
    public int getWidth()
    {
        // We use pref width because that's the internally intended width
        // Actual width may be 0 during initialisation so we can't use that.
        return (int)pane.getPrefWidth();
    }

    @OnThread(Tag.FXPlatform)
    public int getHeight()
    {
        return (int)pane.getPrefHeight();
    }

    public boolean isQueued()
    {
        return queued;
    }

    public void setQueued(boolean queued)
    {
        this.queued = queued;
    }

    @OnThread(Tag.FX)
    public boolean isResizable()
    {
        return resizable;
    }

    @OnThread(Tag.FXPlatform)
    public void setResizable(boolean resizable)
    {
        this.resizable = resizable;
    }

    @OnThread(Tag.Any)
    public boolean isSaveable()
    {
        return true;
    }

    public boolean isSelectable()
    {
        return true;
    }

    @OnThread(Tag.FXPlatform)
    public void repaint()
    {
        Package thePkg = getPackage();
        if (thePkg != null) // Can happen during removal
            thePkg.repaint();
    }

    /**
     * We have a notion of equality that relates solely to the identifierName.
     * If the identifierNames's are equal then the Target's are equal.
     */
    @OnThread(Tag.Any)
    public boolean equals(Object o)
    {
        if (o instanceof Target) {
            Target t = (Target) o;
            return this.identifierName.equals(t.identifierName);
        }
        return false;
    }

    @OnThread(Tag.Any)
    public synchronized int hashCode()
    {
        return identifierName.hashCode();
    }

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public int compareTo(Target t)
    {
        if (equals(t))
            return 0;

        if (this.getY() < t.getY())
            return -1;
        else if (this.getY() > t.getY())
            return 1;

        if (this.getX() < t.getX())
            return -1;
        else if (this.getX() > t.getX())
            return 1;

        return this.identifierName.compareTo(t.getIdentifierName());
    }

    public String toString()
    {
        return getDisplayName();
    }

    @OnThread(Tag.FXPlatform)
    public void setPos(int x, int y)
    {
        AnchorPane.setTopAnchor(pane, (double)y);
        AnchorPane.setLeftAnchor(pane, (double)x);
        repaint();
    }

    @OnThread(Tag.FXPlatform)
    public void setSize(int width, int height)
    {
        pane.setPrefWidth(width);
        pane.setPrefHeight(height);
        repaint();
    }

    @OnThread(Tag.FXPlatform)
    public abstract void doubleClick(boolean openInNewWindow);

    @OnThread(Tag.FXPlatform)
    public abstract void popupMenu(int x, int y, Object editor);

    public abstract void remove();

    @OnThread(Tag.FXPlatform)
    public Node getNode()
    {
        return pane;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isMoveable()
    {
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public void setIsMoveable(boolean b)
    {
        
    }

    /**
     * Is this a front class (true; most of them) or back (false;
     * unit-test classes which always appear behind others)
     */
    @OnThread(Tag.FXPlatform)
    public boolean isFront()
    {
        return true;
    }

    /**
     * Gets the bounds relative to the package editor.
     * @return
     */
    @OnThread(Tag.FXPlatform)
    public Bounds getBoundsInEditor()
    {
        return pane.getBoundsInParent();
    }

    /**
     * Save the current position so that we later know
     * how much to move by (i.e. the delta) when dragging.
     */
    @OnThread(Tag.FXPlatform)
    public void savePreMovePosition()
    {
        preMoveX = getX();
        preMoveY = getY();
    }

    /**
     * The X position before we started the move
     */
    @OnThread(Tag.FXPlatform)
    public int getPreMoveX()
    {
        return preMoveX;
    }

    /**
     * The Y position before we started the move
     */
    @OnThread(Tag.FXPlatform)
    public int getPreMoveY()
    {
        return preMoveY;
    }

    /**
     * Save the current size so that we later know
     * how much to resize by (i.e. the delta).
     */
    @OnThread(Tag.FXPlatform)
    public void savePreResize()
    {
        preResizeWidth = getWidth();
        preResizeHeight = getHeight();
    }

    @OnThread(Tag.FXPlatform)
    public int getPreResizeWidth()
    {
        return preResizeWidth;
    }

    @OnThread(Tag.FXPlatform)
    public int getPreResizeHeight()
    {
        return preResizeHeight;
    }

    @OnThread(Tag.FXPlatform)
    protected final void showingMenu(ContextMenu newMenu)
    {
        if (newMenu != null)
        {
            // Request focus in order to draw selection around us while showing menu:
            requestFocus();
        }
        if (showingContextMenu != null)
        {
            showingContextMenu.hide();
        }
        showingContextMenu = newMenu;
    }

    @OnThread(Tag.FXPlatform)
    private static boolean isArrowKey(KeyEvent evt)
    {
        return evt.getCode() == KeyCode.UP || evt.getCode() == KeyCode.DOWN
            || evt.getCode() == KeyCode.LEFT || evt.getCode() == KeyCode.RIGHT;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isFocused()
    {
        return pane.isFocused();
    }

    @OnThread(Tag.FXPlatform)
    public void requestFocus()
    {
        pane.requestFocus();
    }

    @OnThread(Tag.FXPlatform)
    public void setCreatingExtends(boolean drawingExtends)
    {
        // By default , we darken ourselves:
        pane.setEffect(drawingExtends ? new ColorAdjust(0, 0, -0.2, 0): null);
    }
}
