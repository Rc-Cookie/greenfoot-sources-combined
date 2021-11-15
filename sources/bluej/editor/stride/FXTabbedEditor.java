/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2014,2015,2016,2017,2018  Michael Kolling and John Rosenberg
 
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
package bluej.editor.stride;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.Main;
import bluej.collect.DataCollector;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXSupplier;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.UnfocusableScrollPane;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * FXTabbedEditor is the editor window that contains all the editors in JavaFX tabs (currently,
 * this is the Stride editors only).
 *
 * Because you can drag between tabs, it is responsible for handling dragging, rather than the individual editor tabs doing it.
 *
 * It is also responsible for changing the menus on the window when the tab changes.
 */
@SuppressWarnings("unused")
public @OnThread(Tag.FX) class FXTabbedEditor
{
    /** Are we currently showing the frame catalogue/cheat sheet? */
    private final SimpleBooleanProperty showingCatalogue = new SimpleBooleanProperty(true);
    /** The associated project (one window always maps to single project */
    private final Project project;
    /** The frames which are currently being dragged, if any */
    private final ArrayList<?> dragSourceFrames = new ArrayList<>();
    /** Relative to window overlay, not to scene */
    private final SimpleDoubleProperty mouseDragXProperty = new SimpleDoubleProperty();
    private final SimpleDoubleProperty mouseDragYProperty = new SimpleDoubleProperty();
    /** The starting size of the window.  May be null. Updated on window close. */
    private Rectangle startSize;
    /** The actual window */
    private Stage stage;
    /** The scene within the stage */
    private Scene scene;
    /** The tabs container */
    private TabPane tabPane;
    /** The Pane used for frames being dragged */
    private Pane dragPane;
    /** The Pane in front of dragPane used for showing cursor drop destinations
     *  (so they appear in front of the dragged frames): */
    private Pane dragCursorPane;
    /** The window overlay pane, covers whole window */
    private Object overlayPane;
    /** The right-hand side FrameCatalogue */
    private Object cataloguePane;
    /** The menu bar at the top of the window (or system menu bar on Mac) */
    private MenuBar menuBar;
    /** The tab that is being hovered over to switch tabs (while dragging frames): */
    private Tab hoverTab;
    /** The cancellation action for the scheduled task to switch to another tab after enough time has passed while hovering */
    private FXPlatformRunnable hoverTabTask;
    /** The picture being shown of the currently dragged frames */
    private ImageView dragIcon = null;
    /** Cached so it can be read from any thread.  Written to once on Swing thread in initialise,
     * then effectively final thereafter */
    @OnThread(Tag.Any) private String projectTitle;
    private StringProperty titleStatus = new SimpleStringProperty("");
    private Object collapsibleCatalogueScrollPane;
    private Object shelf;
    private boolean dragFromShelf;


    // Neither the constructor nor any initialisers should do any JavaFX work until
    // initialise is called.
    @OnThread(Tag.Any)
    public FXTabbedEditor(Project project, Rectangle startSize)
    {
        this.project = project;
        this.startSize = startSize;
    }

    static boolean isUselessDrag(Object dragTarget, List<?> dragging, boolean copying)
    {
        return false;
    }

    /**
     * Initialises the FXTabbedEditor.
     */
    @OnThread(Tag.FXPlatform)
    public void initialise()
    {
        projectTitle = project.getProjectName();
        stage = new Stage();
        //add the greenfoot icon to the Stride editor.
        BlueJTheme.setWindowIconFX(stage);

        initialiseFX();
    }

    /**
     * The actual initialisation, on the FX thread
     */
    @OnThread(Tag.FXPlatform)
    private void initialiseFX()
    {
        
    }

    /**
     * It prepares the recordShowHideFrameCatalogue event and invoke the appropriate method to register it.
     * If there is an appropriate selected tab, will invoke this tab method after looking for a possible focused cursor.
     * If there is no an appropriate selected tab, will invoke the DataCollector's recordShowHideFrameCatalogue method,
     * without any info about any editor or a focused cursor.
     *
     * @param show    true for showing and false for hiding
     * @param reason  The event which triggers the change.
     *                It is one of the values in the FrameCatalogue.ShowReason enum.
     */
    @OnThread(Tag.FXPlatform)
    private void recordShowHideFrameCatalogue(boolean show, Object reason)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    private void updateMenusForTab(Object selTab)
    {
        
    }

    /**
     * Property for whether the catalogue/cheat sheet is currently showing
     */
    public BooleanProperty catalogueShowingProperty()
    {
        return showingCatalogue;
    }

    /**
     * Adds the given FXTab to this FXTabbedEditor window
     * @param panel The FXTab to add
     * @param visible Whether to make the FXTabbedEditor window visible 
     * @param toFront Whether to bring the tab to the front (i.e. select the tab)
     */
    @OnThread(Tag.FXPlatform)
    public void addTab(final Object panel, boolean visible, boolean toFront)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public void addTab(final Object panel, boolean visible, boolean toFront, boolean partOfMove)
    {
        
    }

    /** 
     * Opens a Javadoc tab for a core Java class (i.e. comes with the JRE)
     * with given qualified name.
     * 
     * If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    @OnThread(Tag.FXPlatform)
    public void openJavaCoreDocTab(String qualifiedClassName)
    {
        openJavaCoreDocTab(qualifiedClassName, "");
    }

    /** Opens a Javadoc tab for a core Java class (i.e. comes with the JRE) with given qualified name,
     *  and the given URL suffix (typically, "#method_anchor")
     *  
     *  If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    @OnThread(Tag.FXPlatform)
    public void openJavaCoreDocTab(String qualifiedClassName, String suffix)
    {
        String target = Utility.getDocURL(qualifiedClassName, suffix);
        openWebViewTab(target);
    }

    /**
     * Opens a Javadoc tab for a Greenfoot class (e.g. greenfoot.Actor)
     * with given qualified name.
     *
     * If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    @OnThread(Tag.FXPlatform)
    public void openGreenfootDocTab(String qualifiedClassName)
    {
        openGreenfootDocTab(qualifiedClassName, "");
    }

    /**
     * Opens a Javadoc tab for a Greenfoot class (e.g. greenfoot.Actor)
     * with given qualified name and URL suffix (typically, "#method_anchor")
     *
     * If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    @OnThread(Tag.FXPlatform)
    public void openGreenfootDocTab(String qualifiedClassName, String suffix)
    {
        try
        {
            String target = Utility.getGreenfootApiDocURL(qualifiedClassName.replace('.', '/') + ".html");
            openWebViewTab(target + suffix);
        }
        catch (IOException e)
        {
            Debug.reportError(e);
        }

    }

    /**
     * Sets the window visible, and adds/removes the given tab
     * @param visible Whether to add the tab and make window visible (true), or remove the tab (false).
     *                Window is only hidden if no tabs remain (handled elsewhere in code)
     * @param tab     The tab in question
     */
    public void setWindowVisible(boolean visible, Tab tab)
    {
        if (visible)
        {
            if (!stage.isShowing()) {
                if (startSize != null)
                {
                    stage.setX(startSize.getX());
                    stage.setY(startSize.getY());
                    stage.setWidth(startSize.getWidth());
                    stage.setHeight(startSize.getHeight());
                }
                //Debug.time("Showing");
                stage.show();
                //Debug.time("Shown");
                //org.scenicview.ScenicView.show(stage.getScene());
            }
            if (!tabPane.getTabs().contains(tab))
            {
                tabPane.getTabs().add(tab);
            }
        }
        else
        {
            tabPane.getTabs().remove(tab);
        }
    }
    
    /** Returns whether the window is currently shown */
    public boolean isWindowVisible()
    {
        return stage.isShowing();
    }

    /**
     * Brings the tab to the front: unminimises window, brings window to the front, and selects the tab
     */
    @OnThread(Tag.FXPlatform)
    public void bringToFront(Tab tab)
    {
        stage.setIconified(false);
        Utility.bringToFrontFX(stage);
        tabPane.getSelectionModel().select(tab);
    }

    /**
     * Gets the project this window is associated with
     */
    @OnThread(Tag.Any)
    public Project getProject()
    {
        return project;
    }

    /**
     * Removes the given tab from this tabbed editor window
     */
    @OnThread(Tag.FXPlatform)
    public void close(Object tab)
    {
        close(tab, false);
    }

    @OnThread(Tag.FXPlatform)
    private void close(Object tab, boolean partOfMove)
    {
        
    }


    /**
     * Opens a web view tab to display the given URL.
     * 
     * If a web view tab already exists which is displaying that URL (sans anchors),
     * that tab is displayed and a new tab is not opened.
     */
    @OnThread(Tag.FXPlatform)
    public void openWebViewTab(String url)
    {
        openWebViewTab(url, false);
    }

    /**
     * Opens a web view tab to display the given URL.
     *
     * If a web view tab already exists which is displaying that URL (sans anchors),
     * that tab is displayed and a new tab is not opened.
     *
     * @param isTutorial True if this is a special web tab containing the interactive tutorial
     */
    @OnThread(Tag.FXPlatform)
    public void openWebViewTab(String url, boolean isTutorial)
    {
        
    }

    /**
     * The list of currently open tabs
     */
    public ObservableList<Tab> tabsProperty()
    {
        return tabPane.getTabs();
    }

    /**
     * The pane on which the dragged frames are shown
     */
    public Pane getDragPane()
    {
        return dragPane;
    }

    /**
     * The pane on which dragged frame cursor destinations are shown
     * (so that they appear in front of the dragged frames)
     */
    public Pane getDragCursorPane()
    {
        return dragCursorPane;
    }

    /**
     * Begin dragging the given list of frames, starting at the given scene position
     */
    public void frameDragBegin(List<?> srcFrames, boolean fromShelf, double mouseSceneX, double mouseSceneY)
    {
        
    }

    /**
     * Notify us that the current frame has reached the given position
     * @param sceneX Scene X position of mouse
     * @param sceneY Scene Y position of mouse
     * @param dragType Whether the copy-drag key is being held (true) or not (false)
     */
    @OnThread(Tag.FXPlatform)
    public void draggedTo(double sceneX, double sceneY, JavaFXUtil.DragType dragType)
    {
        
    }

    /**
     * Given the current state of this.dragFromShelf and the two params,
     * works out if the drag operation would be a copy or a move.
     * @param toShelf
     * @return true if copying, false if moving
     */
    private boolean calcDragCopy(JavaFXUtil.DragType dragType, boolean toShelf)
    {
        switch (dragType)
        {
            case FORCE_MOVING:
                return false;
            case FORCE_COPYING:
                return true;
            default:
                // Do the default:
                if (dragFromShelf && toShelf)
                    return false; // Move within shelf
                else if (dragFromShelf && !toShelf)
                    return true; // Copy from shelf to editor
                else
                    return false; // Move from editor to shelf, or editor to editor
        }
    }

    /**
     * Returns whether a frame drag is currently taking place
     */
    public boolean isDragging()
    {
        return false;
    }

    /**
     * During a drag, checks if the mouse is hovering over a tab header.
     * (If so, after a little delay, that tab is switched to)
     * @param sceneX The mouse scene X position
     * @param sceneY The mouse scene Y position
     * @param dragType Whether the copy-drag key is being held down
     */
    @OnThread(Tag.FXPlatform)
    private void checkHoverDuringDrag(double sceneX, double sceneY, JavaFXUtil.DragType dragType)
    {
        
    }

    /**
     * Notify us that a frame drag has ended
     * @param dragType Whether the copy-drag key was held down as the drag finished
     */
    @OnThread(Tag.FXPlatform)
    public void frameDragEnd(JavaFXUtil.DragType dragType)
    {
        
    }

    /**
     * Schedule a future update to the frame catalogue.
     * 
     * @param editor The editor associated with the currently shown tab.
     *               Pass null if a different kind of tab (e.g. web tab) is showing.
     * @param c The frame cursor currently focused.
     *          Pass null if there is no currently focused cursor or a different kind of tab is showing
     * @param codeCompletion Whether code completion is currently possible
     * @param selection Whether there is currently a frame selection
     * @param viewMode The current view mode (if a frame editor tab is showing)
     * @param hints The list of hints that should be displayed
     */
    @OnThread(Tag.FXPlatform)
    public void scheduleUpdateCatalogue(Object editor, Object c, CodeCompletionState codeCompletion, boolean selection, Object viewMode, List<?> altExtensions, List<?> hints)
    {

    }

    /**
     * Gets the window overlay pane
     */
    public Object getOverlayPane()
    {
        return null;
    }

    /**
     * Returns true when, and only when, this editor window has a single tab in it
     */
    public boolean hasOneTab()
    {
        return tabPane.getTabs().size() == 1;
    }

    public boolean containsTab(Tab tab)
    {
        return tabPane.getTabs().contains(tab);
    }

    public StringExpression titleProperty()
    {
        return stage.titleProperty();
    }

    private List<?> getFXTabs()
    {
        return null;
    }

    @OnThread(Tag.Swing)
    public void setPosition(int x, int y)
    {
        Platform.runLater(() -> {
            stage.setX(x);
            stage.setY(y);
        });
    }

    @OnThread(Tag.Swing)
    public void setSize(int width, int height)
    {
        Platform.runLater(() -> {
            stage.setWidth(width);
            stage.setHeight(height);
        });
    }

    public void setTitleStatus(String status)
    {
        this.titleStatus.set(status);
    }

    public Window getWindow()
    {
        return stage;
    }

    public static void setupFrameDrag(Object f, boolean isShelf, FXSupplier<FXTabbedEditor> parent, FXSupplier<Boolean> canDrag, FXSupplier<?> selection)
    {
        
    }

    /**
     * Called when this window is no longer going to be used
     */
    public void cleanup()
    {
        
    }

    /**
     * Does one of the tabs in this window contain a tutorial web view tab?
     */
    public boolean hasTutorial()
    {
        return false;
    }

    public static enum CodeCompletionState
    {
        NOT_POSSIBLE, SHOWING, POSSIBLE;
    }

    @OnThread(Tag.FXPlatform)
    public void moveToNewLater(Object tab)
    {
        FXTabbedEditor newWindow = project.createNewFXTabbedEditor();
        moveTabTo(tab, newWindow);
    }

    @OnThread(Tag.FXPlatform)
    public void moveTabTo(Object tab, FXTabbedEditor destination)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public void updateMoveMenus()
    {
        
    }

    @OnThread(Tag.FX)
    public int getX()
    {
        return (int)stage.getX();
    }

    @OnThread(Tag.FX)
    public int getY()
    {
        return (int)stage.getY();
    }

    @OnThread(Tag.FX)
    public int getWidth()
    {
        return (int)stage.getWidth();
    }

    @OnThread(Tag.FX)
    public int getHeight()
    {
        return (int)stage.getHeight();
    }
}
