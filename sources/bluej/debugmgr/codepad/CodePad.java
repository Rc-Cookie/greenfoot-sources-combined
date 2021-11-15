/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2016,2017,2018  Michael Kolling and John Rosenberg
 
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

package bluej.debugmgr.codepad;

import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.IndexHistory;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.ValueCollection;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.javafx.FXPlatformRunnable;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A code pad which can evaluate fragments of Java code.
 * 
 * In JavaFX this is built using a ListView.  ListViews have virtualised
 * cells, which means that only enough rows are created to display
 * the currently visible portion of the scroll pane, not every row in the list.
 * So if the user has a history 200 lines long, but only 8 lines are visible
 * in the codepad, then no matter where they scroll to, only 8 display lines will be
 * created, not 200.
 * 
 * ListViews allow editing of any individual cell, but we hack it here so that
 * (a) only the last row can be edited, and (b) the last row is *always* in an editing state.
 * This means the users see one text field at the bottom of the list, and rows
 * above that with the history (styled as we like).
 */
@OnThread(Tag.FXPlatform)
@SuppressWarnings("unused")
public class CodePad extends VBox
    implements ValueCollection
{
    /**
     * The list view containing all the history items:
     */
    private final ListView<HistoryRow> historyView;

    /**
     * The edit field for input
     */
    private final TextField inputField;
    /**
     * The pane on which to draw the arrows indicating what happens
     * to clicked objects.
     */
    private final Pane arrowOverlay;
    private BooleanBinding shadowShowing;

    /**
     * A data item which backs a single row in the code pad.
     * This might be the currently edited row (the last row), or
     * a read-only item detailing a past command or command outcome;
     */
    private abstract static @OnThread(Tag.FX) class HistoryRow
    {
        // Text content of the row
        private final String text;

        // Different styles used for the rows
        @OnThread(Tag.Any)
        public static enum RowStyle
        {
            COMMAND_PARTIAL("bj-codepad-cmd-partial"),
            COMMAND_END("bj-codepad-cmd-end"),
            ERROR("bj-codepad-error"),
            OUTPUT("bj-codepad-output");

            private final String pseudo;

            public String getPseudoClass()
            {
                return pseudo;
            }

            private RowStyle(String pseudo)
            {
                this.pseudo = pseudo;
            }
        }

        public HistoryRow(String text)
        {
            this.text = text;
        }

        public final String getText() { return text; }

        @Override
        public String toString()
        {
            return getText();
        }

        public abstract Node getGraphic();
        /** Gets the graphical style that should be used for displaying this row. */
        public abstract RowStyle getStyle();
    }

    @OnThread(Tag.FX)
    public static abstract class IndentedRow extends HistoryRow
    {
        // Crude way of making sure all lines are spaced the same as ones with an object image;
        // use an invisible rectangle as a spacer:
        protected Rectangle r;

        public IndentedRow(String text)
        {
            super(text);
            r  = new Rectangle(objectImage.getWidth(), objectImage.getHeight());
            r.setVisible(false);
        }

        /** Gets the graphic to display alongside the row */
        @Override
        public Node getGraphic() { return r; }
    }

    // Handy array with all the different row pseudo-class styles.
    private static final String[] allRowStyles;
    static {
        allRowStyles = new String[HistoryRow.RowStyle.values().length];
        for (int i = 0; i < HistoryRow.RowStyle.values().length; i++)
        {
            allRowStyles[i] = HistoryRow.RowStyle.values()[i].getPseudoClass();
        }
    }

    /**
     * A row with a previously entered command.  This may be a single
     * complete row (e.g. 1+2) or it may be part of a multi-row
     * command.  We have a different pseudo-class for the last row
     * of commands, but currently don't use it differently in the CSS file.
     */
    @OnThread(Tag.FX)
    private static class CommandRow extends HistoryRow
    {
        private final boolean isFinalLine;
        public CommandRow(String text, boolean isFinalLine)
        {
            super(text);
            this.isFinalLine = isFinalLine;
        }

        // No indent spacer on command rows in our current style:
        @Override
        public Node getGraphic()
        {
            return null;
        }

        @Override
        public RowStyle getStyle()
        {
            return isFinalLine ? RowStyle.COMMAND_END : RowStyle.COMMAND_PARTIAL;
        }
    }

    /**
     * The successful output of a previous command.  It may or may not
     * have an object as an output.
     */
    @OnThread(Tag.FX)
    private class OutputSuccessRow extends IndentedRow
    {
        private final ImageView graphic;
        private Path arrow;
        private FXPlatformRunnable cancelAddToBench;

        public OutputSuccessRow(String text, ObjectInfo objInfo)
        {
            super(text);
            graphic = null;
        }

        // Graphic is an object icon if applicable, otherwise
        // we use the invisible spacer from the parent:
        @Override
        public Node getGraphic()
        {
            return graphic != null ? graphic : super.getGraphic();
        }

        @Override
        public RowStyle getStyle()
        {
            return RowStyle.OUTPUT;
        }
    }

    /**
     * A row with an error output of a previous command.
     */
    @OnThread(Tag.FX)
    private static class ErrorRow extends IndentedRow
    {
        public ErrorRow(String text)
        {
            super(text);
        }

        @Override
        public RowStyle getStyle()
        {
            return RowStyle.ERROR;
        }
    }

    private static final String nullLabel = "null";
    
    private static final String uninitializedWarning = Config.getString("pkgmgr.codepad.uninitialized");

    private static final Image objectImage =
            Config.getImageAsFXImage("image.eval.object");
    private static final Image objectImageHighlight =
            Config.getImageAsFXImage("image.eval.object");
    
    private final Object frame;
    @OnThread(Tag.FX)
    private String currentCommand = "";
    @OnThread(Tag.FX)
    private IndexHistory history;
    private Invoker invoker = null;
    private Object textParser = null;
    
    // Keeping track of invocation
    private boolean firstTry;
    private boolean wrappedResult;
    private String errorMessage;

    private boolean busy = false;

    private List<CodepadVar> localVars = new ArrayList<CodepadVar>();
    private List<CodepadVar> newlyDeclareds;
    private List<String> autoInitializedVars;
    // The action which removes the hover state on the object icon
    private Runnable removeHover;

    public CodePad(Object frame, Pane arrowOverlay)
    {
        this.arrowOverlay = null;
        this.frame = null;
        historyView = null;
        inputField = null;
    }

    private void copySelectedRows()
    {
        // If they right click on background with no items selected,
        // copy all items:
        if (historyView.getSelectionModel().isEmpty())
            historyView.getSelectionModel().selectAll();
        String copied = historyView.getSelectionModel().getSelectedItems().stream().map(HistoryRow::getText).collect(Collectors.joining("\n"));
        Clipboard.getSystemClipboard().setContent(Collections.singletonMap(DataFormat.PLAIN_TEXT, copied));
        historyView.getSelectionModel().clearSelection();
    }

    /**
     * Clear the local variables.
     */
    public void clearVars()
    {
        
    }
    
    //   --- ValueCollection interface ---
    
    /*
     * @see bluej.debugmgr.ValueCollection#getValueIterator()
     */
    public Iterator<CodepadVar> getValueIterator()
    {
        return localVars.iterator();
    }
    
    /*
     * @see bluej.debugmgr.ValueCollection#getNamedValue(java.lang.String)
     */
    public NamedValue getNamedValue(String name)
    {
        return null;
    }
    
    /**
     * Search for a named local variable, but do not fall back to the object
     * bench if it cannot be found (return null in this case).
     * 
     * @param name  The name of the variable to search for
     * @return    The named variable, or null
     */
    private NamedValue getLocalVar(String name)
    {
        Iterator<CodepadVar> i = localVars.iterator();
        while (i.hasNext()) {
            NamedValue nv = (NamedValue) i.next();
            if (nv.getName().equals(name))
                return nv;
        }
        
        // not found
        return null;
    }
    
    private class CodePadResultWatcher implements ResultWatcher
    {
        private final String command;

        public CodePadResultWatcher(String command)
        {
            this.command = command;
        }

        /*
                 * @see bluej.debugmgr.ResultWatcher#beginExecution()
                 */
        @Override
        @OnThread(Tag.FXPlatform)
        public void beginCompile()
        {
        }

        /*
         * @see bluej.debugmgr.ResultWatcher#beginExecution()
         */
        @Override
        @OnThread(Tag.FXPlatform)
        public void beginExecution(InvokerRecord ir)
        {
           
        }

        /*
         * @see bluej.debugmgr.ResultWatcher#putResult(bluej.debugger.DebuggerObject, java.lang.String, bluej.testmgr.record.InvokerRecord)
         */
        @Override
        @OnThread(Tag.FXPlatform)
        public void putResult(final DebuggerObject result, final String name, final InvokerRecord ir)
        {
            
        }

        private void updateInspectors()
        {
            
        }

        /**
         * An invocation has failed - here is the error message
         */
        @Override
        public void putError(String message, InvokerRecord ir)
        {
            
        }

        /**
         * A runtime exception occurred.
         */
        @Override
        public void putException(ExceptionDescription exception, InvokerRecord ir)
        {
            
        }

        /**
         * The remote VM terminated before execution completed (or as a result of
         * execution).
         */
        @Override
        public void putVMTerminated(InvokerRecord ir)
        {
            
        }
    }
    
    /**
     * Remove the newly declared variables from the value collection.
     * (This is needed if compilation fails, or execution bombs with an exception).
     */
    private void removeNewlyDeclareds()
    {
        if (newlyDeclareds != null) {
            Iterator<CodepadVar> i = newlyDeclareds.iterator();
            while (i.hasNext()) {
                localVars.remove(i.next());
            }
            newlyDeclareds = null;
        }
    }
    
    //   --- end of ResultWatcher interface ---
    
    /**
     * Show an error message, and allow further command input.
     */
    private void showErrorMsg(final String message)
    {
        error("Error: " + message);
        completeExecution();
    }
    
    /**
     * Show an exception message, and allow further command input.
     */
    private void showExceptionMsg(final String message)
    {
        error("Exception: " + message);
        completeExecution();
    }
    
    /**
     * Execution of the current command has finished (one way or another).
     * Allow further command input.
     */
    private void completeExecution()
    {
        inputField.setEditable(true);
        busy = false;
    }

    /**
     * Record part of a command
     * @param s
     */
    private void command(String s, boolean isFinalLine)
    {
        addRow(new CommandRow(s, isFinalLine));
    }

    private void addRow(HistoryRow row)
    {
        historyView.getSelectionModel().clearSelection();
        historyView.getItems().add(row);
        historyView.scrollTo(historyView.getItems().size() - 1);
    }

    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    private void output(String s)
    {
        addRow(new OutputSuccessRow(s, null));
    }
    
    /**
     * Write a (non-error) message to the text area.
     * @param s The message
     */
    private void objectOutput(String s, ObjectInfo objInfo)
    {
        addRow(new OutputSuccessRow(s, objInfo));
    }
    
    /**
     * Write an error message to the text area.
     * @param s The message
     */
    private void error(String s)
    {
        addRow(new ErrorRow(s));
    }

    public void clear()
    {
        clearVars();
    }

    /**
     * Clear the CodePad after closing the project that the only one is opened,
     * When opening a new project, the CodePad appears again and it is clear.
     */
    public void clearHistoryView()
    {
        historyView.getItems().clear();
    }

    private void executeCommand(String command)
    {
        
    }

    private void softReturn()
    {
        String line = inputField.getText();
        if (line.trim().isEmpty())
            return; // Don't allow entry of blank lines
        currentCommand += line + " ";
        history.add(line);
        command(line, false);
        inputField.setText("");
    }

    private void historyBack()
    {
        String line = history.getPrevious();
        if(line != null) {
            setInput(line);
        }
    }

    private void setInput(String line)
    {
        inputField.setText(line);
        // When going back in history, seems best to put cursor at the end of the field
        // but by default it gets put at the beginning when setting new text.:
        inputField.end();
    }

    private void historyForward()
    {
        String line = history.getNext();
        if(line != null) {
            setInput(line);
        }
    }

    public void focusInputField()
    {
        inputField.requestFocus();
    }

    @OnThread(Tag.Any)
    final class ObjectInfo {
        DebuggerObject obj;
        InvokerRecord ir;
        
        /**
         * Create an object holding information about an invocation.
         */
        public ObjectInfo(DebuggerObject obj, InvokerRecord ir) {
            this.obj = obj;
            this.ir = ir;
        }
    }
    
    final class CodepadVar implements NamedValue {
        
        String name;
        boolean finalVar;
        boolean initialized = false;
        JavaType type;
        
        public CodepadVar(String name, JavaType type, boolean finalVar)
        {
            this.name = name;
            this.finalVar = finalVar;
            this.type = type;
        }
        
        public String getName()
        {
            return name;
        }
        
        public JavaType getGenType()
        {
            return type;
        }
        
        public boolean isFinal()
        {
            return finalVar;
        }
        
        public boolean isInitialized()
        {
            return initialized;
        }
        
        public void setInitialized()
        {
            initialized = true;
        }
    }
    
}
