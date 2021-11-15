/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugger.SourceLocation;
import bluej.debugger.VarDisplayInfo;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Project.DebuggerThreadDetails;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Window for controlling the debugger.
 * <p>
 * There are two modes; one which displays a list of all threads (and the user can select a thread
 * to control/inspect) and another where only a single thread is displayed.
 *
 * @author  Michael Kolling
 */
@SuppressWarnings("unused")
public class ExecControls
{
    private static final String stackTitle =
        Config.getString("debugger.execControls.stackTitle");
    private static final String staticTitle =
        Config.getString("debugger.execControls.staticTitle");
    private static final String instanceTitle =
        Config.getString("debugger.execControls.instanceTitle");
    private static final String localTitle =
        Config.getString("debugger.execControls.localTitle");
    private static final String threadTitle =
        Config.getString("debugger.execControls.threadTitle");

    private static final String haltButtonText =
        Config.getString("debugger.execControls.haltButtonText");
    private static final String stepButtonText =
        Config.getString("debugger.execControls.stepButtonText");
    private static final String stepIntoButtonText =
        Config.getString("debugger.execControls.stepIntoButtonText");
    private static final String continueButtonText =
        Config.getString("debugger.execControls.continueButtonText");
    private static final String terminateButtonText =
        Config.getString("debugger.execControls.terminateButtonText");

    // === instance ===

    @OnThread(Tag.FX)
    private Stage window;
    @OnThread(Tag.FXPlatform)
    private BorderPane fxContent;

    // the display for the list of active threads; may be null if there is no list (i.e. the
    // "single thread" mode is active)
    private ComboBox<DebuggerThreadDetails> threadList;

    @OnThread(Tag.FXPlatform)
    private ListView<SourceLocation> stackList;
    private ListView<VarDisplayInfo> staticList, localList, instanceList;
    private Button stopButton, stepButton, stepIntoButton, continueButton, terminateButton;

    // the Project that owns this debugger
    private final Project project;

    // A flag to keep track of whether a stack frame selection was performed
    // explicitly via the gui or as a result of a debugger event
    private boolean autoSelectionEvent = false; 
    
    /**
     * Fields from these classes (key from map) are only shown if they are in the corresponding whitelist
     * of fields (corresponding value from map)
     */
    @OnThread(Tag.Any) // Rarely modified
    private Map<String, Set<String>> restrictedClasses = Collections.emptyMap();

    private final SimpleBooleanProperty showingProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty hideSystemThreads = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty cannotStepOrContinue = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty cannotHalt = new SimpleBooleanProperty(true);
    
    // The currently selected thread
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private DebuggerThreadDetails selectedThread;


    /**
     * Create a window to view and interact with a debug VM. The window optionally shows a thread pane
     * from which the user can select a thread to control; otherwise the thread selection is controlled
     * programmatically.
     * 
     * @param project  the project this window is associated with
     * @param debugger the debugger this window is debugging
     * @param debuggerThreads  an observable list of all threads that should be displayed by the debugger,
     *                         or null if the thread pane should not be displayed.
     */
    public ExecControls(Project project, Debugger debugger,
            ObservableList<DebuggerThreadDetails> debuggerThreads)
    {
        if (project == null || debugger == null) {
            throw new NullPointerException("project or debugger null in ExecControls");
        }

        this.project = project;
        this.window = new Stage();
        window.setTitle(Config.getApplicationName() + ":  " + Config.getString("debugger.execControls.windowTitle"));
        BlueJTheme.setWindowIconFX(window);
        createWindowContent(debuggerThreads);
        TilePane buttons = new TilePane(Orientation.HORIZONTAL, stopButton, stepButton, stepIntoButton, continueButton, terminateButton);
        buttons.setPrefColumns(buttons.getChildren().size());
        JavaFXUtil.addStyleClass(buttons, "debugger-buttons");
        this.fxContent = new BorderPane();
        BorderPane vars = new BorderPane();
        vars.setTop(labelled(staticList, staticTitle));
        SplitPane varSplit = new SplitPane(labelled(instanceList, instanceTitle), labelled(localList, localTitle));
        varSplit.setOrientation(Orientation.VERTICAL);
        vars.setCenter(varSplit);
        
        // There are two possible pane layouts: with thread list and without.
        BorderPane lhsPane;
        if (debuggerThreads != null)
        {
            lhsPane = new BorderPane(labelled(stackList, stackTitle), labelled(threadList, threadTitle), null, null, null);
            JavaFXUtil.addStyleClass(threadList, "debugger-thread-combo");
        }
        else
        {
            lhsPane = new BorderPane(labelled(stackList, stackTitle), null, null, null, null);
        }
        JavaFXUtil.addStyleClass(lhsPane, "debugger-thread-and-stack");
        
        fxContent.setTop(makeMenuBar());
        fxContent.setCenter(new SplitPane(lhsPane, vars));
        fxContent.setBottom(buttons);
        JavaFXUtil.addStyleClass(fxContent, "debugger");
        // Menu bar will be added later:
        Scene scene = new Scene(fxContent);
        Config.addDebuggerStylesheets(scene);
        window.setScene(scene);
        Config.loadAndTrackPositionAndSize(window, "bluej.debugger");
        window.setOnShown(e -> {
            DataCollector.debuggerChangeVisible(project, true);
            showingProperty.set(true);
        });
        window.setOnHidden(e -> {
            DataCollector.debuggerChangeVisible(project, false);
            showingProperty.set(false);
        });
        // showingProperty should mirror the window state.  Note that it
        // can be set either externally as a request to show the window,
        // or internally as an update of the state, so we must be careful
        // not to end up in an infinite loop:
        JavaFXUtil.addChangeListenerPlatform(showingProperty, show -> {
            if (show && !window.isShowing())
            {
                window.show();
            }
            else if (!show && window.isShowing())
            {
                window.hide();
            }
        });

    }

    private static Node labelled(Node content, String title)
    {
        Label titleLabel = new Label(title);
        JavaFXUtil.addStyleClass(titleLabel, "debugger-section-title");
        BorderPane borderPane = new BorderPane(content, titleLabel, null, null, null);
        JavaFXUtil.addStyleClass(borderPane, "debugger-section");
        return borderPane;
    }

    /**
     * Sets the restricted classes - classes for which only some fields should be displayed.
     * 
     * @param restrictedClasses a map of class name to a set of white-listed fields.
     */
    public void setRestrictedClasses(Map<String, Set<String>> restrictedClasses)
    {
        this.restrictedClasses = restrictedClasses;
    }
    
    public Map<String, Set<String>> getRestrictedClasses()
    {
        HashMap<String, Set<String>> copy = new HashMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> e : restrictedClasses.entrySet())
        {
            copy.put(e.getKey(), new HashSet<String>(e.getValue()));
        }
        return copy;
    }

    /**
     * Make sure that a particular thread is displayed and the details are up-to-date.
     * Note that if the controls window is invisible this will not show it. 
     * 
     * @param  dt  the thread to highlight in the thread
     *             tree and whose status we want to display.
     */
    public void selectThread(final DebuggerThread dt)
    {
        if (threadList != null)
        {
            if (dt.isKnownSystemThread())
            {
                hideSystemThreads.set(false);
            }
            
            DebuggerThreadDetails details = threadList.getItems().stream()
                    .filter(d -> d.isThread(dt))
                    .findFirst().orElse(null);
            if (details != null)
            {
                threadList.getSelectionModel().select(details);
            }
        }
        else if (getSelectedThreadDetails() == null || ! dt.sameThread(getSelectedThreadDetails().getThread()))
        {
            project.getDebugger().runOnEventHandler(() -> {
                DebuggerThreadDetails threadDetails = new DebuggerThreadDetails(dt);
                Platform.runLater(() -> selectedThreadChanged(threadDetails));
            });
        }
    }

    /**
     * Update the details displayed for the given thread (if they are currently displayed).
     */
    @OnThread(Tag.VMEventHandler)
    public void updateThreadDetails(DebuggerThread dt)
    {
        DebuggerThreadDetails sel = getSelectedThreadDetails();
        if (sel != null && sel.isThread(dt))
        {
            if (isSingleThreadMode())
            {
                sel.update();
            }
            setThreadDetails(sel);
        }
    }

    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    private boolean isSingleThreadMode()
    {
        return threadList == null;
    }

    @OnThread(Tag.FXPlatform)
    private void selectedThreadChanged(DebuggerThreadDetails dt)
    {
        if (dt == null)
        {
            synchronized (this)
            {
                selectedThread = null;
            }
            cannotHalt.set(true);
            cannotStepOrContinue.set(true);
            stackList.getItems().clear();
        }
        else
        {
            synchronized (this)
            {
                selectedThread = dt;
            }
            project.getDebugger().runOnEventHandler(() -> setThreadDetails(dt));
        }
    }

    /**
     * Display the details for the currently selected thread.
     * These details include showing the threads stack, and displaying 
     * the details for the top stack frame.
     */
    @OnThread(Tag.VMEventHandler)
    private void setThreadDetails(DebuggerThreadDetails dt)
    {
        //Copy the list because we may alter it:
        List<SourceLocation> stack = new ArrayList<>(dt.getThread().getStack());
        List<SourceLocation> filtered = Arrays.asList(getFilteredStack(stack));

        boolean isSuspended = dt.isSuspended();
        Platform.runLater(() -> {
            cannotHalt.set(isSuspended);
            cannotStepOrContinue.set(!isSuspended);

            stackList.getItems().setAll(filtered);
            if (filtered.size() > 0)
            {
                // show details of top frame
                autoSelectionEvent = true;
                stackList.getSelectionModel().select(0);
                autoSelectionEvent = false;
            }
        });
    }
    
    @OnThread(Tag.Any)
    public static SourceLocation [] getFilteredStack(List<SourceLocation> stack)
    {
        return null;
    }
    
    /**
     * Clear the display of thread details (stack and variables).
     */
    private void clearThreadDetails()
    {
        stackList.getItems().clear();
        staticList.getItems().clear();
        instanceList.getItems().clear();
        localList.getItems().clear();
    }

    /**
     * Make a stack frame in the stack display the selected stack frame.
     * This will cause this frame's details (local variables, etc.) to be
     * displayed, as well as the current source position being marked.
     */
    @OnThread(Tag.VMEventHandler)
    private void stackFrameSelectionChanged(DebuggerThread thread, int index, boolean showSource)
    {
        if (index >= 0) {
            setStackFrameDetails(thread, index);
            thread.setSelectedFrame(index);
                
            if (showSource) {
                String aClass = thread.getClass(index);
                String classSourceName = thread.getClassSourceName(index);
                int lineNumber = thread.getLineNumber(index);
                DebuggerObject currentObject = thread.getCurrentObject(index);
                Platform.runLater(() -> project.showSource(thread,
                        aClass,
                        classSourceName,
                        lineNumber,
                        currentObject));
            }
        }
    }

    /**
     * Display the detail information (current object fields and local var's)
     * for a specific stack frame.
     */
    @OnThread(Tag.VMEventHandler)
    private void setStackFrameDetails(DebuggerThread thread, int frameNo)
    {
        
    }

    /**
     * Create and arrange the GUI components.
     * @param debuggerThreads
     */
    private void createWindowContent(ObservableList<DebuggerThreadDetails> debuggerThreads)
    {
        
    }

    // The label is <html><center>...<br>...</html> (silly, really)
    // so we remove the tags here:
    private static String removeHTML(String label)
    {
        return label.replace("<html>", "").replace("<center>", "").replace("<br>", "\n").replace("</html>", "");
    }

    private boolean showThread(DebuggerThreadDetails thread)
    {
        if (hideSystemThreads.get())
            return !thread.getThread().isKnownSystemThread();
        else
            return true;
    }

    private ListView<VarDisplayInfo> makeVarListView()
    {
        ListView<VarDisplayInfo> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.setCellFactory(lv -> {
            return new VarDisplayCell(project, window);
        });
        return listView;
    }

    /**
     * Create the debugger's menubar, all menus and items.
     */
    private MenuBar makeMenuBar()
    {
        MenuBar menubar = new MenuBar();
        menubar.setUseSystemMenuBar(true);
        Menu menu = new Menu(Config.getString("terminal.options"));

        
        if (!Config.isGreenfoot()) {
            MenuItem systemThreadItem = JavaFXUtil.makeCheckMenuItem(Config.getString("debugger.hideSystemThreads"), hideSystemThreads, null);
            menu.getItems().add(systemThreadItem);
            menu.getItems().add(new SeparatorMenuItem());
        }
        menu.getItems().add(JavaFXUtil.makeMenuItem(Config.getString("close"), this::hide, new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN)));

        menubar.getMenus().add(menu);
        return menubar;
    }

    public void show()
    {
        window.show();
        window.toFront();
    }

    public void hide()
    {
        window.hide();
    }

    @OnThread(Tag.Any)
    public synchronized DebuggerThreadDetails getSelectedThreadDetails()
    {
        return selectedThread;
    }

    public BooleanProperty showingProperty()
    {
        return showingProperty;
    }

    /**
     * Action to halt the selected thread.
     */
    private class StopAction
    {
        public StopAction()
        {
            
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            DebuggerThreadDetails details = getSelectedThreadDetails();
            if (details == null)
                return;
            clearThreadDetails();
            if (!details.isSuspended()) {
                project.getDebugger().runOnEventHandler(() -> details.getThread().halt());
            }
        }
    }
        
    /**
     * Action to step through the code.
     */
    private class StepAction
    {
        public StepAction()
        {
            
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            DebuggerThreadDetails details = getSelectedThreadDetails();
            if (details == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (details.isSuspended()) {
                project.getDebugger().runOnEventHandler(() -> details.getThread().step());
            }
            project.updateInspectors();
        }
    }

    private static Node makeStepIcon()
    {
        Polygon arrowShape = makeScaledUpArrow(false);
        JavaFXUtil.addStyleClass(arrowShape, "step-icon-arrow");
        Rectangle bar = new Rectangle(28, 6);
        JavaFXUtil.addStyleClass(bar, "step-icon-bar");
        VBox vBox = new VBox(arrowShape, bar);
        JavaFXUtil.addStyleClass(vBox, "step-icon");
        return vBox;
    }

    private static Polygon makeScaledUpArrow(boolean shortTail)
    {
        Polygon arrowShape = Config.makeArrowShape(shortTail);
        JavaFXUtil.scalePolygonPoints(arrowShape, 1.5, true);
        return arrowShape;
    }

    private static Node makeContinueIcon()
    {
        Polygon arrowShape1 = makeScaledUpArrow(true);
        Polygon arrowShape2 = makeScaledUpArrow(true);
        Polygon arrowShape3 = makeScaledUpArrow(true);
        JavaFXUtil.addStyleClass(arrowShape1, "continue-icon-arrow");
        JavaFXUtil.addStyleClass(arrowShape2, "continue-icon-arrow");
        JavaFXUtil.addStyleClass(arrowShape3, "continue-icon-arrow");
        arrowShape1.setOpacity(0.2);
        arrowShape2.setOpacity(0.5);
        Pane pane = new Pane(arrowShape1, arrowShape2, arrowShape3);
        arrowShape2.setLayoutX(2.0);
        arrowShape2.setLayoutY(6.0);
        arrowShape3.setLayoutX(4.0);
        arrowShape3.setLayoutY(12.0);
        return pane;
    }

    /**
     * Action to "step into" the code.
     */
    private class StepIntoAction
    {
        public StepIntoAction()
        {
            
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            DebuggerThreadDetails details = getSelectedThreadDetails();
            if (details == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (details.isSuspended()) {
                project.getDebugger().runOnEventHandler(() -> details.getThread().stepInto());
            }
        }
    }

    private static Node makeStepIntoIcon()
    {
        SVGPath path = new SVGPath();
        // See http://jxnblk.com/paths/?d=M2%2016%20Q24%208%2038%2016%20L40%2010%20L48%2026%20L32%2034%20L34%2028%20Q22%2022%206%2028%20Z
        path.setContent("M2 16 Q24 8 38 16 L40 10 L48 26 L32 34 L34 28 Q22 22 6 28 Z");
        path.setScaleX(0.75);
        path.setScaleY(0.85);
        JavaFXUtil.addStyleClass(path, "step-into-icon");
        return new Group(path);
    }

    /**
     * Action to continue a halted thread. 
     */
    private class ContinueAction
    {
        public ContinueAction()
        {
            
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            DebuggerThreadDetails details = getSelectedThreadDetails();
            if (details == null)
                return;
            clearThreadDetails();
            project.removeStepMarks();
            if (details.isSuspended()) {
                project.getDebugger().runOnEventHandler(() -> details.getThread().cont());
                DataCollector.debuggerContinue(project, details.getThread().getName());
            }
        }
    }

    /**
     * Action to terminate the program, restart the VM.
     */
    private class TerminateAction
    {
        public TerminateAction()
        {
            
        }
        
        public void actionPerformed(boolean viaContextMenu)
        {
            try {
                clearThreadDetails();
                
                // throws an illegal state exception
                // if we press this whilst we are already
                // restarting the remote VM
                project.restartVM();
                DataCollector.debuggerTerminate(project);
            }
            catch (IllegalStateException ise) { }
        }
    }

    private static Node makeTerminateIcon()
    {
        Polygon s = new Polygon(
            5, 0,
            15, 10,
            25, 0,
            30, 5,
            20, 15,
            30, 25,
            25, 30,
            15, 20,
            5, 30,
            0, 25,
            10, 15,
            0, 5
        );
        JavaFXUtil.addStyleClass(s, "terminate-icon");
        return s;
    }


    /**
     * A cell in a list view which has a variable's type, name and value.  (And optionally, access modifier)
     */
    private static class VarDisplayCell extends javafx.scene.control.ListCell<VarDisplayInfo>
    {
        private final Label access = new Label();
        private final Label type = new Label();
        private final Label name = new Label();
        private final Label value = new Label();
        private final BooleanProperty nonEmpty = new SimpleBooleanProperty();
        private static final Image objectImage =
                Config.getImageAsFXImage("image.eval.object");
        // A property so that we can listen for it changing from null to/from non-null:
        private final SimpleObjectProperty<Supplier<DebuggerObject>> fetchObject = new SimpleObjectProperty<>(null);

        public VarDisplayCell(Project project, Window window)
        {
            // Only visible when there is a relevant object reference which can be inspected:
            ImageView objectImageView = new ImageView(objectImage);
            JavaFXUtil.addStyleClass(objectImageView, "debugger-var-object-ref");
            objectImageView.visibleProperty().bind(fetchObject.isNotNull());
            objectImageView.managedProperty().bind(objectImageView.visibleProperty());
            objectImageView.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1)
                    inspect(project, window, objectImageView);
            });

            // The spacing is added via CSS, not by space characters:
            HBox hBox = new HBox(access, type, name, new Label("="), objectImageView, this.value);
            hBox.visibleProperty().bind(nonEmpty);
            hBox.styleProperty().bind(PrefMgr.getEditorFontCSS(false));
            JavaFXUtil.addStyleClass(hBox, "debugger-var-cell");
            JavaFXUtil.addStyleClass(access, "debugger-var-access");
            JavaFXUtil.addStyleClass(type, "debugger-var-type");
            JavaFXUtil.addStyleClass(name, "debugger-var-name");
            JavaFXUtil.addStyleClass(value, "debugger-var-value");
            setGraphic(hBox);

            // Double click anywhere on the row does an object inspection, as it used to:
            hBox.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                {
                    inspect(project, window, objectImageView);
                }
            });
        }

        @OnThread(Tag.FXPlatform)
        private void inspect(Project project, Window window, Node sourceNode)
        {
            if (fetchObject.get() != null)
            {
                project.getInspectorInstance(fetchObject.get().get(), null, null, null, window, sourceNode);
            }
        }

        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        protected void updateItem(VarDisplayInfo item, boolean empty)
        {
            super.updateItem(item, empty);
            nonEmpty.set(!empty);
            if (empty)
            {
                access.setText("");
                type.setText("");
                name.setText("");
                value.setText("");
                fetchObject.set(null);
            }
            else
            {
                access.setText(item.getAccess());
                type.setText(item.getType());
                name.setText(item.getName());
                value.setText(item.getValue());
                fetchObject.set(item.getFetchObject());
            }
        }
    }
}
