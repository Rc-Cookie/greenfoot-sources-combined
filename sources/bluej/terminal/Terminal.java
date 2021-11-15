/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014,2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.terminal;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.DebuggerTerminal;
import bluej.debugmgr.ExecutionEvent;
import bluej.pkgmgr.Project;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Utility;

import java.io.Reader;
import java.io.Writer;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The Frame part of the Terminal window used for I/O when running programs
 * under BlueJ.
 *
 * @author  Michael Kolling
 * @author  Philip Stevens
 */
@SuppressWarnings("unused")
public final class Terminal
    implements DebuggerTerminal
{
    private static final int MAX_BUFFER_LINES = 200;
    private Object errorScrollPane;

    private static interface TextAreaStyle
    {
        public String getCSSClass();
    }

    // The style for text in the stdout pane: was it output by the program, or input by the user?
    // Or third option: details about method recording
    private static enum StdoutStyle implements TextAreaStyle
    {
        OUTPUT("terminal-output"), INPUT("terminal-input"), METHOD_RECORDING("terminal-method-record");

        private final String cssClass;

        private StdoutStyle(String cssClass)
        {
            this.cssClass = cssClass;
        }

        public String getCSSClass()
        {
            return cssClass;
        }
    }

    private static enum StderrStyleType
    {
        NORMAL("terminal-error"), LINKED_STACK_TRACE("terminal-stack-link"), FOREIGN_STACK_TRACE("terminal-stack-foreign");

        private final String cssClass;

        private StderrStyleType(String cssClass)
        {
            this.cssClass = cssClass;
        }

        public String getCSSClass()
        {
            return cssClass;
        }
    }

    private static class StderrStyle implements TextAreaStyle
    {
        private final StderrStyleType type;
        private final ExceptionSourceLocation exceptionSourceLocation;

        private StderrStyle(StderrStyleType type)
        {
            this.type = type;
            this.exceptionSourceLocation = null;
        }

        public StderrStyle(ExceptionSourceLocation exceptionSourceLocation)
        {
            this.type = StderrStyleType.LINKED_STACK_TRACE;
            this.exceptionSourceLocation = exceptionSourceLocation;
        }

        public static final StderrStyle NORMAL = new StderrStyle(StderrStyleType.NORMAL);
        public static final StderrStyle FOREIGN_STACK_TRACE = new StderrStyle(StderrStyleType.FOREIGN_STACK_TRACE);

        @Override
        public String getCSSClass()
        {
            return type.getCSSClass();
        }
    }

    private static final String WINDOWTITLE = Config.getApplicationName() + ": " + Config.getString("terminal.title");

    private static final String RECORDMETHODCALLSPROPNAME = "bluej.terminal.recordcalls";
    private static final String CLEARONMETHODCALLSPROPNAME = "bluej.terminal.clearscreen";
    private static final String UNLIMITEDBUFFERINGCALLPROPNAME = "bluej.terminal.buffering";

    private final String title;

    // -- instance --

    private final Project project;
    
    private final Object text;
    private Object errorText = null;
    private final TextField input;
    private final SplitPane splitPane;
    private boolean isActive = false;
    private static BooleanProperty recordMethodCalls =
            Config.getPropBooleanProperty(RECORDMETHODCALLSPROPNAME);
    private static BooleanProperty clearOnMethodCall =
            Config.getPropBooleanProperty(CLEARONMETHODCALLSPROPNAME);
    private static BooleanProperty unlimitedBufferingCall =
            Config.getPropBooleanProperty(UNLIMITEDBUFFERINGCALLPROPNAME);
    private boolean newMethodCall = true;
    private boolean errorShown = false;
    private final InputBuffer buffer;
    private final BooleanProperty showingProperty = new SimpleBooleanProperty(false);

    @OnThread(Tag.Any) private final Reader in = new TerminalReader();
    @OnThread(Tag.Any) private final Writer out = new TerminalWriter(false);
    @OnThread(Tag.Any) private final Writer err = new TerminalWriter(true);

    private Stage window;

    /**
     * Create a new terminal window with default specifications.
     */
    public Terminal(Project project)
    {
        this.title = WINDOWTITLE + " - " + project.getProjectName();
        this.project = project;
        buffer = null;
        input = null;
        splitPane = null;
        text = null;
    }

    /**
     * Copy whichever of the stdout/stderr panes actually has a selection.
     */
    private void doCopy()
    {
        
    }

    private void sendInput(boolean eof)
    {
        
    }

    private void applyStyle(Object t, TextAreaStyle s)
    {
        
    }

    /**
     * Show or hide the Terminal window.
     */
    public void showHide(boolean show)
    {
        DataCollector.showHideTerminal(project, show);

        if (show)
        {
            window.show();
            input.requestFocus();
        }
        else
        {
            window.hide();
        }
    }
    
    public void dispose()
    {
        showHide(false);
        window = null;
    }

    /**
     * Return true if the window is currently displayed.
     */
    public boolean isShown()
    {
        return window.isShowing();
    }

    /**
     * Make the input field active, or not
     */
    public void activate(boolean active)
    {
        if(active != isActive) {
            input.setEditable(active);
            isActive = active;
        }
    }

    /**
     * Clear the terminal.
     */
    public void clear()
    {
        
    }

    /**
     * Save the terminal text to file.
     */
    public void save()
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public void print()
    {
        
    }

    /**
     * Write some text to the terminal.
     */
    private <S extends TextAreaStyle> void writeToPane(Object pane, String s, S style)
    {
        
    }

    private <S extends TextAreaStyle> void trimToMaxBufferLines(Object pane)
    {
        
    }

    /**
     * Prepare the terminal for I/O.
     */
    private void prepare()
    {
        if (newMethodCall) {   // prepare only once per method call
            showHide(true);
            newMethodCall = false;
        }
        else if (Config.isGreenfoot()) {
            // In greenfoot new output should always show the terminal
            if (!window.isShowing()) {
                showHide(true);
            }
        }
    }

    /**
     * An interactive method call has been made by a user.
     */
    private void methodCall(String callString)
    {
        
    }

    /**
     * Check if "clear on method call" option is selected.
     */
    public boolean clearOnMethodCall()
    {
        return clearOnMethodCall.getValue();
    }

    private static <S> Object styled(String text, S style)
    {
        return null;
    }

    private void constructorCall(InvokerRecord ir)
    {
        
    }
    
    private void methodResult(ExecutionEvent event)
    {
        
    }

    /**
     * Looks through the contents of the terminal for lines
     * that look like they are part of a stack trace.
     */
    private void scanForStackTrace()
    {
        
    }



    /**
     * Return the input stream that can be used to read from this terminal.
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Reader getReader()
    {
        return in;
    }


    /**
     * Return the output stream that can be used to write to this terminal
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Writer getWriter()
    {
        return out;
    }

    /**
     * It implements the method on the interface DebuggerTerminal which is called
     * when there is reading request from the terminal on the remote virtual machine
     */
    @OnThread(Tag.Any)
    public void showOnInput()
    {
        Platform.runLater(() -> {
            if (!this.isShown()) {
                this.showHide(true);
            }

            if (this.isShown()) {
                Utility.bringToFrontFX(window);
                input.requestFocus();
            }
        });
    }

    /**
     * Return the output stream that can be used to write error output to this terminal
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Writer getErrorWriter()
    {
        return err;
    }

    // ---- BlueJEventListener interface ----

    /**
     * Called when a BlueJ event is raised. The event can be any BlueJEvent
     * type. The implementation of this method should check first whether
     * the event type is of interest an return immediately if it isn't.
     *
     * @param eventId  A constant identifying the event. One of the event id
     *                 constants defined in BlueJEvent.
     * @param arg      An event specific parameter. See BlueJEvent for
     *                 definition.
     * @param prj      A project where the event happens
     */
    public void blueJEvent(int eventId, Object arg, Project prj)
    {
        
    }

    // ---- make window frame ----

    /**
     * Show the errorPane for error output
     */
    private void showErrorPane()
    {
        
    }
    
    /**
     * Hide the pane with the error output.
     */
    private void hideErrorPane()
    {
        
    }


    public BooleanProperty showingProperty()
    {
        return showingProperty;
    }
    
    /**
     * Create the terminal's menubar, all menus and items.
     */
    private MenuBar makeMenuBar()
    {
        MenuBar menubar = new MenuBar();
        menubar.setUseSystemMenuBar(true);
        Menu menu = new Menu(Config.getString("terminal.options"));
        MenuItem clearItem = new MenuItem(Config.getString("terminal.clear"));
        clearItem.setOnAction(e -> clear());
        clearItem.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN));

        MenuItem copyItem = new MenuItem(Config.getString("terminal.copy"));
        copyItem.setOnAction(e -> doCopy());
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));

        MenuItem saveItem = new MenuItem(Config.getString("terminal.save"));
        saveItem.setOnAction(e -> save());
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));

        MenuItem printItem = new MenuItem(Config.getString("terminal.print"));
        printItem.setOnAction(e -> print());
        printItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN));

        menu.getItems().addAll(clearItem, copyItem, saveItem, printItem, new SeparatorMenuItem());

        CheckMenuItem autoClear = new CheckMenuItem(Config.getString("terminal.clearScreen"));
        autoClear.selectedProperty().bindBidirectional(clearOnMethodCall);

        CheckMenuItem recordCalls = new CheckMenuItem(Config.getString("terminal.recordCalls"));
        recordCalls.selectedProperty().bindBidirectional(recordMethodCalls);

        CheckMenuItem unlimitedBuffering = new CheckMenuItem(Config.getString("terminal.buffering"));
        unlimitedBuffering.selectedProperty().bindBidirectional(unlimitedBufferingCall);

        menu.getItems().addAll(autoClear, recordCalls, unlimitedBuffering);

        MenuItem closeItem = new MenuItem(Config.getString("terminal.close"));
        closeItem.setOnAction(e -> showHide(false));
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));

        menu.getItems().addAll(new SeparatorMenuItem(), closeItem);

        menubar.getMenus().add(menu);
        return menubar;
    }

    public Stage getWindow()
    {
        return window;
    }

    /**
     * Cleanup any resources or listeners the terminal has created/registered.
     * Called when the project is closing.
     */
    public void cleanup()
    {
        
    }

    /**
     * A Reader which reads from the terminal.
     */
    @OnThread(Tag.Any)
    private class TerminalReader extends Reader
    {
        public int read(char[] cbuf, int off, int len)
        {
            int charsRead = 0;

            while(charsRead < len) {
                cbuf[off + charsRead] = buffer.getChar();
                charsRead++;
                if(buffer.isEmpty())
                    break;
            }
            return charsRead;
        }

        @Override
        public boolean ready()
        {
            return ! buffer.isEmpty();
        }
        
        public void close() { }
    }

    /**
     * A writer which writes to the terminal. It can be flagged for error output.
     * The idea is that error output could be presented differently from standard
     * output.
     */
    @OnThread(Tag.Any)
    private class TerminalWriter extends Writer
    {
        private boolean isErrorOut;
        
        TerminalWriter(boolean isError)
        {
            super();
            isErrorOut = isError;
        }

        public void write(final char[] cbuf, final int off, final int len)
        {
            
        }

        public void flush() { }

        public void close() { }
    }
}
