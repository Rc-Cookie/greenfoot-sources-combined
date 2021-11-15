/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014,2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import bluej.debugger.DebuggerObject;
import bluej.views.CallableView;
import javafx.application.Platform;

import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.Config;
import bluej.compiler.FXCompileObserver;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerThread;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.CallHistory;
import bluej.editor.Editor;
import bluej.extensions.SourceType;
import bluej.extensions.event.CompileEvent;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget;
import bluej.pkgmgr.target.ReadmeTarget;
import bluej.pkgmgr.target.Target;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import bluej.utility.Utility;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A Java package (collection of Java classes).
 * 
 * @author Michael Kolling
 * @author Axel Schmolitzky
 * @author Andrew Patterson
 */
@SuppressWarnings("unused")
public final class Package
{
    /** message to be shown on the status bar */
    static final String compiling = Config.getString("pkgmgr.compiling");
    /** message to be shown on the status bar */
    static final String compileDone = Config.getString("pkgmgr.compileDone");
    /** message to be shown on the status bar */
    static final String chooseUsesTo = Config.getString("pkgmgr.chooseUsesTo");
    /** message to be shown on the status bar */
    static final String chooseInhTo = Config.getString("pkgmgr.chooseInhTo");

    /**
     * the name of the package file in a package directory that holds
     * information about the package and its targets.
     */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private PackageFile packageFile;
    
    /** Readme file name */
    public static final String readmeName = "README.TXT";

    /** error code */
    public static final int NO_ERROR = 0;
    /** error code */
    public static final int FILE_NOT_FOUND = 1;
    /** error code */
    public static final int ILLEGAL_FORMAT = 2;
    /** error code */
    public static final int COPY_ERROR = 3;
    /** error code */
    public static final int CLASS_EXISTS = 4;
    /** error code */
    public static final int CREATE_ERROR = 5;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private final List<Target> targetsToPlace = new ArrayList<>();
    // Has this package been sent for data recording yet?
    private boolean recorded = false;

    /** Reason code for displaying source line */
    private enum ShowSourceReason
    {
        STEP_OR_HALT,    // a step or other halt 
        BREAKPOINT_HIT,  // a breakpoint was hit
        FRAME_SELECTED;   // the stack frame was selected in the debugger
        
        /**
         * Check whether this reason corresponds to a suspension event (breakpoint/step/etc).
         */
        public boolean isSuspension()
        {
            return this == STEP_OR_HALT || this == BREAKPOINT_HIT;
        }
    }
    
    /**
     * In the top left corner of each package we have a fixed target - either a
     * ParentPackageTarget or a ReadmeTarget. These are there locations
     */
    public static final int FIXED_TARGET_X = 10;
    public static final int FIXED_TARGET_Y = 10;

    /** the Project this package is in */
    private final Project project;

    /**
     * the parent Package object for this package or null if this is the unnamed
     * package ie. the root of the package tree
     */
    private final Package parentPackage;

    /** base name of package (eg util) ("" for the unnamed package) */
    private final String baseName;

    /**
     * this properties object contains the properties loaded off disk for this
     * package, or the properties which were most recently saved to disk for
     * this package
     */
    private volatile Object lastSavedProps = null;

    /** all the targets in a package */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private final Object targets;

    /** Holds the choice of "from" target for a new dependency */
    @OnThread(Tag.FXPlatform)
    private DependentTarget fromChoice;

    /** the CallHistory of a package */
    private CallHistory callHistory;

    /**
     * needed when debugging with breakpoints to see if the editor window needs
     * to be brought to the front
     */
    private String lastSourceName = "";
    
    /** determines the maximum length of the CallHistory of a package */
    public static final int HISTORY_LENGTH = 6;
    
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Object editor;

    private PackageUI ui;
    
    @OnThread(Tag.FXPlatform)    
    private List<?> listeners = new ArrayList<>();

    //package-visible
    List<?> getUsesArrows()
    {
        return usesArrows;
    }

    //package-visible
    List<?> getExtendsArrows()
    {
        return extendsArrows;
    }

    @OnThread(value = Tag.FXPlatform)
    private final List<?> usesArrows = new ArrayList<>();

    @OnThread(value = Tag.FXPlatform)
    private final List<?> extendsArrows = new ArrayList<>();
    
    /** True if we currently have a compile queued up waiting for debugger to become idle */
    @OnThread(Tag.FXPlatform)
    private boolean waitingForIdleToCompile = false;
    
    /** Whether we have issued a package compilation, and not yet seen its conclusion */
    private boolean currentlyCompiling = false;
    
    /** Whether a compilation has been queued (behind the current compile job). Only one compile can be queued. */
    private boolean queuedCompile = false;
    private CompileReason queuedReason;
    
    private final List<FXCompileObserver> compileObservers = new ArrayList<>();
    
    /** File pointing at the directory for this package */
    @OnThread(Tag.Any)
    private File dir;

    /* ------------------- end of field declarations ------------------- */

    /**
     * Create a package of a project with the package name of baseName (ie
     * reflect) and with a parent package of parent (which may represent
     * java.lang for instance) If the package file (bluej.pkg) is not found, an
     * IOException is thrown.
     */
    public Package(Project project, String baseName, Package parent)
        throws IOException
    {
        if (parent == null)
            throw new NullPointerException("Package must have a valid parent package");

        if (baseName.length() == 0)
            throw new IllegalArgumentException("unnamedPackage must be created using Package(project)");

        if (!JavaNames.isIdentifier(baseName))
            throw new IllegalArgumentException(baseName + " is not a valid name for a Package");

        this.project = project;
        this.baseName = baseName;
        this.parentPackage = parent;
        this.targets = null;

        init();
    }

    /**
     * Create the unnamed package of a project If the package file (bluej.pkg)
     * is not found, an IOException is thrown.
     */
    public Package(Project project)
        throws IOException
    {
        this.project = project;
        this.baseName = "";
        this.parentPackage = null;
        this.targets = null;
        init();
    }

    private void init()
        throws IOException
    {
        callHistory = new CallHistory(HISTORY_LENGTH);
        dir = new File(project.getProjectDir(), getRelativePath().getPath());
        load();
    }

    @OnThread(Tag.Any)
    public boolean isUnnamedPackage()
    {
        return parentPackage == null;
    }

    /**
     * Return the project this package belongs to.
     */
    @OnThread(Tag.Any)
    public Project getProject()
    {
        return project;
    }

    @OnThread(value = Tag.Any,requireSynchronized = true)
    private Object singleBPackage;  // Every Package has none or one BPackage
    
    /**
     * Return the extensions BPackage associated with this Package.
     * There should be only one BPackage object associated with each Package.
     * @return the BPackage associated with this Package.
     */
    @OnThread(Tag.Any)
    public synchronized final Object getBPackage ()
    {
        return null;
    }


    /**
     * Get the unique identifier for this package (it's directory name at
     * present)
     */
    @OnThread(Tag.Any)
    public String getId()
    {
        return getPath().getPath();
    }

    /**
     * Return this package's base name (eg util) ("" for the unnamed package)
     */
    @OnThread(Tag.Any)
    public String getBaseName()
    {
        return baseName;
    }

    /**
     * Return the qualified name of an identifier in this package (eg
     * java.util.Random if given Random)
     */
    @OnThread(Tag.Any)
    public String getQualifiedName(String identifier)
    {
        if (isUnnamedPackage())
            return identifier;
        else
            return getQualifiedName() + "." + identifier;
    }

    /**
     * Return the qualified name of the package (eg. java.util) ("" for the
     * unnamed package)
     */
    @OnThread(Tag.Any)
    public String getQualifiedName()
    {
        Package currentPkg = this;
        String retName = "";

        while (!currentPkg.isUnnamedPackage()) {
            if (retName.equals("")) {
                retName = currentPkg.getBaseName();
            }
            else {
                retName = currentPkg.getBaseName() + "." + retName;
            }

            currentPkg = currentPkg.getParent();
        }

        return retName;
    }

    /**
     * get the readme target for this package
     *  
     */
    public synchronized ReadmeTarget getReadmeTarget()
    {
        return null;
    }

    /**
     * Construct a path for this package relative to the project.
     * 
     * @return The relative path.
     */
    private File getRelativePath()
    {
        Package currentPkg = this;
        File retFile = new File(currentPkg.getBaseName());

        /*
         * loop through our parent packages constructing a relative path for
         * this file
         */
        while (!currentPkg.isUnnamedPackage()) {
            currentPkg = currentPkg.getParent();

            retFile = new File(currentPkg.getBaseName(), retFile.getPath());
        }

        return retFile;
    }

    /**
     * Return a file object of the directory location of this package.
     * 
     * @return The file object representing the full path to the packages
     *         directory
     */
    @OnThread(Tag.Any)
    public File getPath() 
    {
        return dir;
    }

    /**
     * Return our parent package or null if we are the unnamed package.
     */
    @OnThread(Tag.Any)
    public Package getParent()
    {
        return parentPackage;
    }

    /**
     * Returns the sub-package if this package is "boring". Our definition of
     * boring is that the package has no classes in it and only one sub package.
     * If this package is not boring, this method returns null.
     */
    protected synchronized Package getBoringSubPackage()
    {
        return null;
    }

    /**
     * Return an array of package objects which are nested one level below us.
     * 
     * @param getUncached   should be true if unopened packages should be included
     */
    protected synchronized List<Package> getChildren(boolean getUncached)
    {
        return null;
    }

    public void setStatus(String msg)
    {
        
    }

    /**
     * Sets the PackageEditor for this package.
     * @param ed The PackageEditor.  Non-null when opening, null when
     *           closing.
     */
    @OnThread(Tag.FXPlatform)
    void setEditor(Object ed)
    {
        
    }
    
    /**
     * Get the editor for this package, as a PackageEditor. This should be considered deprecated;
     * use getUI() instead if possible. May return null.
     */
    @Deprecated
    @OnThread(Tag.Any)
    public synchronized Object getEditor()
    {
        return null;
    }
    
    /**
     * Set the UI controller for this package.
     */
    public void setUI(PackageUI ui)
    {
        this.ui = ui;
    }
    
    /**
     * Retrieve the UI controller for this package. (May return null if no UI has been set; however,
     * most operations requiring the UI should be performed in contexts where the UI has been set, so
     * it should normally be safe to assume non-null return).
     */
    public PackageUI getUI()
    {
        return ui;
    }

    /**
     * Add a listener for this package.
     */
    public synchronized void addListener(Object pl)
    {
        
    }
    
    /**
     * Remove a listener for this package.
     */
    public synchronized void removeListener(Object pl)
    {
        
    }
    
    /**
     * Fire a "package closed" event to listeners.
     */
    @OnThread(Tag.FXPlatform)
    private void fireClosedEvent()
    {
        
    }
    
    /**
     * Fire a "graph changed" event to listeners.
     */
    @OnThread(Tag.FXPlatform)
    private void fireChangedEvent()
    {
        
    }
    
    /**
     * Get the package properties, as most recently saved. The returned Properties set should be considered
     * immutable.
     */
    @OnThread(Tag.Any)
    public Properties getLastSavedProperties()
    {
        return null;
    }

    /**
     * Get the currently selected Targets. It will return an empty array if no
     * target is selected.
     * 
     * @return the currently selected array of Targets.
     */
    @OnThread(Tag.Any)
    public synchronized List<Target> getSelectedTargets()
    {
        return Utility.filterList(getVertices(), Target::isSelected);
    }

    /**
     * Search a directory for Java source and class files and add their names to
     * a set which is returned. Will delete any __SHELL files which are found in
     * the directory and will ignore any single .class files which do not
     * contain public classes.
     * 
     * The returned set is guaranteed to be only valid Java identifiers.
     */
    private Set<String> findTargets(File path)
    {
        return null;
    }

    /**
     * Load the elements of a package from a specified directory. If the package
     * file (bluej.pkg) is not found, an IOException is thrown.
     * 
     * <p>This does not cause targets to be loaded. Use refreshPackage() for that.
     */
    public synchronized void load()
        throws IOException
    {
        
    }
    
    /**
     * Refresh the targets and dependency arrows in the package, based on whatever
     * is actually on disk.
     */
    public void refreshPackage()
    {
        
    }
    
    /**
     * Returns the file containing information about the package.
     * For BlueJ this is package.bluej (or for older versions bluej.pkg) 
     * and for Greenfoot it is greenfoot.project.
     */
    private PackageFile getPkgFile()
    {
        File dir = getPath();
        return PackageFileFactory.getPackageFile(dir);
    }

    /**
     * Position a target which has been added, based on the layout file
     * (if an entry exists) or find a suitable position otherwise.
     * 
     * @param t  the target to position
     */
    public void positionNewTarget(Target t)
    {
        
    }
    
    /**
     * Add our immovable targets (the readme file, and possibly a link to the
     * parent package)
     */ 
    private void addImmovableTargets()
    {
        
    }

    /**
     * Reload a package.
     * 
     * This means we check the existing directory contents and compare it
     * against the targets we have in the package. Any new directories or java
     * source is added to the package. This function will not remove targets
     * that have had their corresponding on disk counterparts removed.
     * 
     * Any new source files will have their package lines updated to match the
     * package we are in.
     */
    public void reload()
    {
        
    }
    
    /**
     * ReRead the pkg file and update the position of the targets in the graph
     * @throws IOException
     *
     */
    public void reReadGraphLayout() throws IOException
    {
        
    }

    @OnThread(Tag.Any)
    public void repaint()
    {
        
    }

    /**
     * Save this package to disk. The package is saved to the standard package
     * file.
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void save(Properties frameProperties)
    {
        
    }

    /**
     * Import a source file into this package as a new class target. Returns an
     * error code: NO_ERROR - everything is fine FILE_NOT_FOUND - file does not
     * exist ILLEGAL_FORMAT - the file name does not end in ".java" CLASS_EXISTS -
     * a class with this name already exists COPY_ERROR - could not copy
     */
    public int importFile(File aFile)
    {
        return 0;
    }

    public ClassTarget addClass(String className)
    {
        // create class icon (ClassTarget) for new class
        ClassTarget target = new ClassTarget(this, className);
        addTarget(target);

        // make package line in class source match our package
        try {
            target.enforcePackage(getQualifiedName());
        }
        catch (IOException ioe) {
            Debug.message(ioe.getLocalizedMessage());
        }

        return target;
    }

    /**
     * Add a new package target to this package.
     * 
     * @param packageName The basename of the package to add
     */
    public Object addPackage(String packageName)
    {
        return null;
    }

    @OnThread(Tag.Any)
    public Debugger getDebugger()
    {
        return getProject().getDebugger();
    }

    /**
     * Loads a class using the current project class loader.
     * Return null if the class cannot be loaded.
     */
    public Class<?> loadClass(String className)
    {
        return getProject().loadClass(className);
    }

    @OnThread(Tag.Any)
    public synchronized List<Target> getVertices()
    {
        return null;
    }


    /**
     * Return a List of all ClassTargets that have the role of a unit test.
     */
    public synchronized List<ClassTarget> getTestTargets()
    {
        return null;
    }
    
    /**
     * Find and compile all uncompiled classes, and get reports of compilation
     * status/results via the specified CompileObserver.
     * <p>
     * In general this should be called only when the debugger is
     * in the idle state (or at least not when executing user code). A new
     * project classloader will be created which can be used to load the
     * newly compiled classes, once they are ready.
     * 
     * @param compObserver  An observer to be notified of compilation progress.
     *                  The callback methods will be called on the Swing EDT.
     *                  The 'endCompile' method will always be called; other
     *                  methods may not be called if the compilation is aborted
     *                  (sources cannot be saved etc).
     */
    public void compile(FXCompileObserver compObserver, CompileReason reason, CompileType type)
    {
        Set<ClassTarget> toCompile = new HashSet<ClassTarget>();

        try
        {
            List<ClassTarget> classTargets;
            // build the list of targets that need to be compiled
            synchronized (this)
            {
                classTargets = getClassTargets();
            }
            for (ClassTarget ct : classTargets)
            {
                if (!ct.isCompiled() && !ct.isQueued())
                {
                    ct.ensureSaved();
                    toCompile.add(ct);
                    ct.setQueued(true);
                }
            }

            if (!toCompile.isEmpty())
            {
                if (type.keepClasses())
                {
                    project.removeClassLoader();
                    project.newRemoteClassLoaderLeavingBreakpoints();
                }
                ArrayList<FXCompileObserver> observers = new ArrayList<>(compileObservers);
                if (compObserver != null)
                {
                    observers.add(compObserver);
                }
                doCompile(toCompile, new PackageCompileObserver(observers), reason, type);
            }
            else {
                if (compObserver != null) {
                    compObserver.endCompile(new CompileInputFile[0], true, type, -1);
                }
            }
        }
        catch (IOException ioe) {
            // Abort compile
            Debug.log("Error saving class before compile: " + ioe.getLocalizedMessage());
            for (ClassTarget ct : toCompile) {
                ct.setQueued(false);
            }
            if (compObserver != null) {
                compObserver.endCompile(new CompileInputFile[0], false, type, -1);
            }
        }
    }
    
    /**
     * Find and compile all uncompiled classes.
     * <p>
     * In general this should be called only when the debugger is
     * in the idle state (or at least not when executing user code). A new
     * project classloader will be created which can be used to load the
     * newly compiled classes, once they are ready.
     */
    public void compile(CompileReason reason, CompileType type)
    {
        if (! currentlyCompiling) { 
            currentlyCompiling = true;
            compile(new FXCompileObserver() {
                // The return of this method will be ignored,
                // as PackageCompileObserver which chains to us, ignores it
                @Override
                @OnThread(Tag.FXPlatform)
                public boolean compilerMessage(Diagnostic diagnostic, CompileType type) { return false; }
                
                @Override
                @OnThread(Tag.FXPlatform)
                public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence) { }
                
                @Override
                @OnThread(Tag.FXPlatform)
                public void endCompile(CompileInputFile[] sources, boolean succesful, CompileType type2, int compilationSequence)
                {
                    // This will be called on the Swing thread.
                    currentlyCompiling = false;
                    if (queuedCompile) {
                        queuedCompile = false;
                        compile(queuedReason, type);
                        queuedReason = null;
                    }
                }
            }, reason, type);
        }
        else {
            queuedCompile = true;
            queuedReason = reason;
        }
    }
    
    /**
     * Compile a single class.
     */
    public void compile(ClassTarget ct, CompileReason reason, CompileType type)
    {
        compile(ct, false, null, reason, type);
    }
    
    /**
     * Compile a single class.
     */
    public void compile(ClassTarget ct, boolean forceQuiet, FXCompileObserver compObserver, CompileReason reason, CompileType type)
    {
        if (!checkCompile()) {
            return;
        }

        ClassTarget assocTarget = (ClassTarget) ct.getAssociation();
        if (assocTarget != null && ! assocTarget.hasSourceCode()) {
            assocTarget = null;
        }

        // we don't want to try and compile if it is a class target without src
        // it may be better to avoid calling this method on such targets
        if (!ct.hasSourceCode()) {
            ct = null;
        }

        if (ct != null || assocTarget != null) {
            if (type.keepClasses())
            {
                project.removeClassLoader();
                project.newRemoteClassLoaderLeavingBreakpoints();
            }

            if (ct != null) {
                ArrayList<FXCompileObserver> chainedObservers = new ArrayList<>(compileObservers);
                if (compObserver != null)
                {
                    chainedObservers.add(compObserver);
                }
                FXCompileObserver observer;
                if (forceQuiet) {
                    observer = new QuietPackageCompileObserver(chainedObservers);
                } else {
                    observer = new PackageCompileObserver(chainedObservers);
                }
                searchCompile(ct, observer, reason, type);
            }

            if (assocTarget != null) {
                searchCompile(assocTarget, new QuietPackageCompileObserver(Collections.emptyList()), reason, type);
            }
        }
    }

    /**
     * Compile a single class quietly.
     */
    public void compileQuiet(ClassTarget ct, CompileReason reason, CompileType type)
    {
        if (!isDebuggerIdle()) {
            return;
        }

        searchCompile(ct, new QuietPackageCompileObserver(Collections.emptyList()), reason, type);
    }

    /**
     * Force compile of all classes. Called by user function "rebuild".
     */
    public void rebuild()
    {
        
    }

    /**
     * Have all editors in this package save the file the are showing.
     * Called when doing a cvs operation
     */
    public void saveFilesInEditors() throws IOException
    {
        // Because we call editor.save() on targets, which can result in
        // a renamed class target, we need to iterate through a copy of
        // the collection - hence the new ArrayList call here:
        List<ClassTarget> classTargets;
        synchronized (this)
        {
            classTargets = new ArrayList<>(getClassTargets());
        }
        for (ClassTarget ct : classTargets) {
            Editor ed = ct.getEditor();
            // Editor can be null eg. class file and no src file
            if(ed != null) {
                ed.save();
            }
        }
    }
    
    /**
     * Compile a class together with its dependencies, as necessary.
     */
    private void searchCompile(ClassTarget t, FXCompileObserver observer, CompileReason reason, CompileType type)
    {
        
    }

    /**
     * Compile every Target in 'targetList'. Every compilation goes through this method.
     * All targets in the list should have been saved beforehand.
     */
    private void doCompile(Collection<ClassTarget> targetList, FXCompileObserver edtObserver, CompileReason reason, CompileType type)
    {
        
    }

    /**
     * Returns true if the debugger is not busy. This is true if it is either
     * IDLE, or has not been completely constructed (NOTREADY).
     */
    public boolean isDebuggerIdle()
    {
        Debugger debugger = getDebugger();
        if (debugger == null) {
            // This method can be called during Project construction, when the debugger
            // has not been created yet. Return true in this case, since the debugger
            // is considered idle while the remote VM is starting.
            return true;
        }
        int status = debugger.getStatus();
        return (status == Debugger.IDLE) || (status == Debugger.NOTREADY);
    }

    /**
     * Check whether it's okay to compile and display a message about it.
     */
    private boolean checkCompile()
    {
        if (isDebuggerIdle())
            return true;

        // The debugger is NOT idle, show a message about it.
        showMessage("compile-while-executing");
        return false;
    }
    
    /**
     * Compile the package, but only when the debugger is in an idle state.
     * @param specificTarget The single classtarget to compile; if null then will compile whole package.
     */
    public void compileOnceIdle(ClassTarget specificTarget, CompileReason reason, CompileType type)
    {
        if (! waitingForIdleToCompile) {
            if (isDebuggerIdle())
            {
                if (specificTarget == null)
                    compile(reason, type);
                else
                    compile(specificTarget, reason, type);
            }
            else {
                waitingForIdleToCompile = true;
                // No lambda as we need to also remove:
                DebuggerListener dlistener = new DebuggerListener() {
                    @Override
                    @OnThread(Tag.Any)
                    public void processDebuggerEvent(DebuggerEvent e, boolean skipUpdate)
                    {
                        if (e.getNewState() == Debugger.IDLE)
                        {
                            getDebugger().removeDebuggerListener(this);
                            // We call compileOnceIdle, not compile, because we might not still be idle
                            // by the time we run on the GUI thread, so we may have to do the whole
                            // thing again:
                            Platform.runLater(() -> {
                                if (waitingForIdleToCompile) {
                                    waitingForIdleToCompile = false;
                                    compileOnceIdle(specificTarget, reason, type);
                                }
                            });
                        }
                    }
                };
                
                getDebugger().addDebuggerListener(dlistener);
                
                // Potential race: the debugger may have gone idle just before we added the listener.
                // Check for that now:
                if (isDebuggerIdle()) {
                    waitingForIdleToCompile = false;
                    compile(reason, type);
                    getDebugger().removeDebuggerListener(dlistener);
                }
            }
        }
    }

    /**
     * Generate documentation for this package.
     * 
     * @return "" if everything was alright, an error message otherwise.
     */
    public String generateDocumentation()
    {
        // This implementation currently just delegates the generation to
        // the project this package is part of.
        return project.generateDocumentation();
    }

    /**
     * Generate documentation for class in this ClassTarget.
     * 
     * @param ct
     *            the class to generate docs for
     */
    public void generateDocumentation(ClassTarget ct)
    {
        // editor file is already saved: no need to save it here
        String filename = ct.getSourceFile().getPath();
        project.generateDocumentation(filename);
    }

    /**
     * Re-initialize breakpoints, necessary after a new class loader is
     * installed.
     */
    public void reInitBreakpoints()
    {
        List<ClassTarget> classTargets;
        synchronized (this)
        {
            classTargets = getClassTargets();
        }
        for (ClassTarget target : classTargets)
        {
            target.reInitBreakpoints();
        }
    }

    /**
     * Remove all step marks in all classes.
     */
    public void removeStepMarks()
    {
        List<ClassTarget> classTargets;
        synchronized (this)
        {
            classTargets = new ArrayList<>(getClassTargets());
        }
        for (ClassTarget target : classTargets)
        {
            target.removeStepMark();
        }
        if (getUI() != null)
        {
            getUI().highlightObject(null);
        }
    }

    public synchronized void addTarget(Target t)
    {
        
    }

    public synchronized void removeTarget(Target t)
    {
        
    }

    /**
     * Changes the Target identifier. Targets are stored in a hashtable with
     * their name as the key. If class name changes we need to remove the target
     * and add again with the new key.
     */
    public synchronized void updateTargetIdentifier(Target t, String oldIdentifier, String newIdentifier)
    {
        
    }

    /**
     * remove the arrow representing the given dependency
     * 
     * @param d  the dependency to remove
     */
    @OnThread(Tag.FXPlatform)
    public void removeArrow(Object d)
    {
        
    }


    /**
     * A user initiated addition of an "implements" clause from a class to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddImplementsClassDependency(ClassTarget from, ClassTarget to)
    {
        
    }

    /**
     * A user initiated addition of an "extends" clause from an interface to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddExtendsInterfaceDependency(ClassTarget from, ClassTarget to)
    {
        
    }

    /**
     * A user initiated addition of an "extends" clause from a class to
     * a class
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddExtendsClassDependency(ClassTarget from, ClassTarget to)
    {
        
    }

    /**
     * A user initiated removal of a dependency
     *
     * @param d  an instance of an Implements or Extends dependency
     */
    public void userRemoveDependency(Object d)
    {
        
    }
    
    /**
     * Lay out the arrows between targets.
     */
    @OnThread(Tag.FXPlatform)
    private void recalcArrows()
    {
        for (Target t : getVertices())
        {
            if (t instanceof DependentTarget) {
                DependentTarget dt = (DependentTarget) t;

                dt.recalcInUses();
                dt.recalcOutUses();
            }
        }
    }

    /**
     * Return the target with name "identifierName".
     * 
     * @param identifierName
     *            the unique name of a target.
     * @return the target with name "tname" if existent, null otherwise.
     */
    @OnThread(Tag.Any)
    public synchronized Target getTarget(String identifierName)
    {
        return null;
    }

    /**
     * Return the dependent target with name "identifierName".
     * 
     * @param identifierName
     *            the unique name of a target.
     * @return the target with name "tname" if existent and if it is a
     *         DependentTarget, null otherwise.
     */
    public synchronized DependentTarget getDependentTarget(String identifierName)
    {
        return null;
    }

    
    /**
     * Returns an ArrayList of ClassTargets holding all targets of this package.
     * @return a not null but possibly empty array list of ClassTargets for this package.
     */
    @OnThread(Tag.Any)
    public synchronized final ArrayList<ClassTarget> getClassTargets()
    {
        return null;
    }

    /**
     * Return a List of Strings with names of all classes in this package.
     */
    public synchronized List<String> getAllClassnames()
    {
        return Utility.mapList(getClassTargets(), ClassTarget::getBaseName);
    }

    /**
     * Return a List of Strings with names of all classes in this package that
     * has accompanying source.
     */
    public synchronized List<String> getAllClassnamesWithSource()
    {
        return null;
    }

    /**
     * Test whether a file instance denotes a BlueJ or Greenfoot package directory depending on which mode we are in.
     * 
     * @param f
     *            the file instance that is tested for denoting a BlueJ package.
     * @return true if f denotes a directory and a BlueJ package.
     */
    @OnThread(Tag.Any)
    public static boolean isPackage(File f)
    {
        return false;
    }
    
    /**
     * Test whether this name is the name of a package file.
     */
    @OnThread(Tag.Any)
    public static boolean isPackageFileName(String name)
    {
        return false;
    }

    /**
     * Called when in an interesting state (e.g. adding a new dependency) and a
     * target is selected. Calling with 'null' as parameter resets to idle state.
     */
    @OnThread(Tag.Any)
    public void targetSelected(Target t)
    {
        /*
        if(t == null) {
            if(getState() != S_IDLE) {
                setState(S_IDLE);
                setStatus(" ");
            }
            return;
        }

        switch(getState()) {
            case S_CHOOSE_USES_FROM :
                if (t instanceof DependentTarget) {
                    fromChoice = (DependentTarget) t;
                    setState(S_CHOOSE_USES_TO);
                    setStatus(chooseUsesTo);
                }
                else {
                    setState(S_IDLE);
                    setStatus(" ");
                }
                break;

            case S_CHOOSE_USES_TO :
                if (t != fromChoice && t instanceof DependentTarget) {
                    setState(S_IDLE);
                    addDependency(new UsesDependency(this, fromChoice, (DependentTarget) t), true);
                    setStatus(" ");
                }
                break;

            case S_CHOOSE_EXT_FROM :

                if (t instanceof DependentTarget) {
                    fromChoice = (DependentTarget) t;
                    setState(S_CHOOSE_EXT_TO);
                    setStatus(chooseInhTo);
                }
                else {
                    setState(S_IDLE);
                    setStatus(" ");
                }
                break;

            case S_CHOOSE_EXT_TO :
                if (t != fromChoice) {
                    setState(S_IDLE);
                    if (t instanceof ClassTarget && fromChoice instanceof ClassTarget) {

                        ClassTarget from = (ClassTarget) fromChoice;
                        ClassTarget to = (ClassTarget) t;

                        // if the target is an interface then we have an
                        // implements dependency
                        if (to.isInterface()) {
                            Dependency d = new ImplementsDependency(this, from, to);

                            if (from.isInterface()) {
                                userAddImplementsInterfaceDependency(d);
                            }
                            else {
                                userAddImplementsClassDependency(d);
                            }

                            addDependency(d, true);
                        }
                        else {
                            // an extends dependency can only be from a class to
                            // another class
                            if (!from.isInterface() && !to.isEnum() && !from.isEnum()) {
                                Dependency d = new ExtendsDependency(this, from, to);
                                userAddExtendsClassDependency(d);
                                addDependency(d, true);
                            }
                            else {
                                // TODO display an error dialog or status
                            }
                        }
                    }
                    setStatus(" ");
                }
                break;

            default :
                // e.g. deleting arrow - selecting target ignored
                break;
        }
        */
    }

    /**
     * Use the dialog manager to display an error message. The PkgMgrFrame is
     * used to find a parent window so we can correctly offset the dialog.
     */
    public void showError(String msgId)
    {
        
    }

    /**
     * Use the dialog manager to display a message. The PkgMgrFrame is used to
     * find a parent window so we can correctly offset the dialog.
     */
    public void showMessage(String msgId)
    {
        
    }

    /**
     * Use the dialog manager to display a message with text. The PkgMgrFrame is
     * used to find a parent window so we can correctly offset the dialog.
     */
    public void showMessageWithText(String msgId, String text)
    {
        
    }

    /**
     * Don't remember the last shown source anymore.
     */
    public void forgetLastSource()
    {
        lastSourceName = "";
    }

    /**
     * A thread has hit a breakpoint, done a step or selected a frame in the debugger. Display the source
     * code with the relevant line highlighted.
     *
     * Note: source name is the unqualified name of the file (no path attached)
     * 
     * @return true if the debugger display is already taken care of, or
     * false if you still want to show the ExecControls window afterwards.
     */
    @OnThread(Tag.FXPlatform)
    private boolean showSource(DebuggerThread thread, String sourcename, int lineNo, ShowSourceReason reason, String msg, DebuggerObject currentObject)
    {
        boolean bringToFront = !sourcename.equals(lastSourceName);
        lastSourceName = sourcename;

        // showEditorMessage:
        Editor targetEditor = editorForTarget(new File(getPath(), sourcename).getAbsolutePath(), bringToFront);
        if (targetEditor != null)
        {
            if (getUI() != null)
            {
                getUI().highlightObject(currentObject);
            }
            
            return targetEditor.setStepMark(lineNo, msg, reason.isSuspension(), thread);
        }
        else if (reason == ShowSourceReason.BREAKPOINT_HIT) {
            showMessageWithText("break-no-source", sourcename);
        }
        return false;
    }

    /**
     * Show the specified line of the specified source file. Open the editor if necessary.
     * @param sourcename  The source file to show
     * @param lineNo      The line number to show
     * @return  true if the editor was the most recent editor to have a message displayed
     */
    public void showSource(String sourcename, int lineNo)
    {
        String msg = " ";
        
        boolean bringToFront = !sourcename.equals(lastSourceName);
        lastSourceName = sourcename;

        showEditorMessage(new File(getPath(), sourcename).getPath(), lineNo, msg, false, bringToFront);
    }
    
    /**
     * An interface for message "calculators" which can produce enhanced diagnostic messages when
     * given a reference to the editor in which a compilation error occurred.
     */
    public static interface MessageCalculator
    {
        /**
         * Produce a diagnostic message for the given editor.
         * This should produce something half-way helpful if null is passed.
         * 
         * @param e  The editor where the original error occurred (null if it cannot be determined).
         */
        public String calculateMessage(Editor e);
    }
    
    /**
     * Attempt to display (in the corresponding editor) an error message associated with a
     * specific line in a class. This is done by opening the class's source, highlighting the line
     * and showing the message in the editor's information area. If the filename specified does
     * not exist, the message is not shown.
     * 
     * @return true if the message was displayed; false if there was no suitable class.
     */
    private boolean showEditorMessage(String filename, int lineNo, final String message,
            boolean beep, boolean bringToFront)
    {
        Editor targetEditor = editorForTarget(filename, bringToFront);
        if (targetEditor == null)
        {
            Debug.message("Error or exception for source not in project: " + filename + ", line " +
                    lineNo + ": " + message);
            return false;
        }

        targetEditor.displayMessage(message, lineNo, 0);
        return true;
    }

    /**
     * Find or open the Editor for a given source file. The editor is opened and displayed if it is not
     * currently visible. If the source file is in another package, a package editor frame will be
     * opened for that package.
     * 
     * @param filename   The source file name, which should be a full absolute path
     * @param bringToFront  True if the editor should be brought to the front of the window z-order
     * @return  The editor for the given source file, or null if there is no editor.
     */
    private Editor editorForTarget(String filename, boolean bringToFront)
    {
        Target t = getTargetForSource(filename);
        if (! (t instanceof ClassTarget)) {
            return null;
        }

        ClassTarget ct = (ClassTarget) t;
        
        Editor targetEditor = ct.getEditor();
        if (targetEditor != null) {
            if (! targetEditor.isOpen() || bringToFront) {
                ct.open();;
            }
        }
        
        return targetEditor;
    }
    
    /**
     * Find the target for a given source file. If the target is in another package, a package editor
     * frame is opened for the package (if not open already).
     * 
     * @param filename  The source file name
     * @return  The corresponding target, or null if the target doesn't exist.
     */
    private Target getTargetForSource(String filename)
    {
        return null;
    }
    
    /**
     * An enumeration for indicating whether a compilation diagnostic was actually displayed to the
     * user.
     */
    private static enum ErrorShown
    {
        // Reminds me of http://thedailywtf.com/Articles/What_Is_Truth_0x3f_.aspx :-)
        ERROR_SHOWN, ERROR_NOT_SHOWN, EDITOR_NOT_FOUND
    }
    
    /**
     * Display a compiler diagnostic (error or warning) in the appropriate editor window.
     * 
     * @param diagnostic   The diagnostic to display
     * @param messageCalc  The message "calculator", which returns a modified version of the message;
     *                     may be null, in which case the original message is shown unmodified.
     * @param errorIndex The index of the error (first is 0, second is 1, etc)
     * @param compileType The type of the compilation which caused the error.
     */
    private ErrorShown showEditorDiagnostic(Diagnostic diagnostic, MessageCalculator messageCalc, int errorIndex, CompileType compileType)
    {
        String fileName = diagnostic.getFileName();
        if (fileName == null) {
            return ErrorShown.EDITOR_NOT_FOUND;
        }
        
        Target target = getTargetForSource(fileName);
        if (! (target instanceof ClassTarget)) {
            return ErrorShown.EDITOR_NOT_FOUND;
        }
        
        ClassTarget t = (ClassTarget) target;

        Editor targetEditor = t.getEditor();
        if (targetEditor != null) {
            if (messageCalc != null) {
                diagnostic.setMessage(messageCalc.calculateMessage(targetEditor));
            }
            
            if (project.isClosing()) {
                return ErrorShown.ERROR_NOT_SHOWN;
            }
            boolean shown = t.showDiagnostic(diagnostic, errorIndex, compileType);
            return shown ? ErrorShown.ERROR_SHOWN : ErrorShown.ERROR_NOT_SHOWN;
        }
        else {
            Debug.message(t.getDisplayName() + ", line" + diagnostic.getStartLine() +
                    ": " + diagnostic.getMessage());
            return ErrorShown.EDITOR_NOT_FOUND;
        }
    }

    /**
     * A breakpoint in this package was hit.
     */
    @OnThread(Tag.FXPlatform)
    public void hitBreakpoint(DebuggerThread thread, String classSourceName, int lineNumber, DebuggerObject currentObject)
    {
        String msg = null;
        if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT)) {
            msg = Config.getString("debugger.accessibility.breakpoint");
            msg = msg.replace("$", thread.getName());
        }
        
        if (!showSource(thread, classSourceName, lineNumber, ShowSourceReason.BREAKPOINT_HIT, msg, currentObject))
        {
            getProject().getExecControls().show();
            getProject().getExecControls().selectThread(thread);
        }
    }

    /**
     * Execution stopped by someone pressing the "halt" button or we have just
     * done a "step".
     */
    @OnThread(Tag.FXPlatform)
    public void hitHalt(DebuggerThread thread, String classSourceName, int lineNumber, DebuggerObject currentObject, boolean breakpoint)
    {
        String msg = null;
        if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT)) {
            msg = breakpoint ? Config.getString("debugger.accessibility.breakpoint") : Config.getString("debugger.accessibility.paused");
            msg = msg.replace("$", thread.getName());
        }
        
        ShowSourceReason reason = breakpoint ? ShowSourceReason.BREAKPOINT_HIT : ShowSourceReason.STEP_OR_HALT;
        if (!showSource(thread, classSourceName, lineNumber, reason, msg, currentObject))
        {
            getProject().getExecControls().show();
            getProject().getExecControls().selectThread(thread);
        }
    }

    /**
     * Display a source file from this package at the specified position.
     */
    @OnThread(Tag.FXPlatform)
    public void showSourcePosition(DebuggerThread thread, String sourceName, int lineNumber, DebuggerObject currentObject)
    {
        showSource(thread, sourceName, lineNumber, ShowSourceReason.FRAME_SELECTED, null, currentObject);
    }
    
    /**
     * Display an exception message. This is almost the same as "errorMessage"
     * except for different help texts.
     */
    public void exceptionMessage(ExceptionDescription exception)
    {
        String text = exception.getClassName();
        if (text == null) {
            reportException(exception.getText());
            return;
        }
        
        String message = text + ":\n" + exception.getText();
        List<SourceLocation> stack = exception.getStack();
        
        if ((stack == null) || (stack.size() == 0)) {
            // Stack empty or missing. This can happen when an exception is
            // thrown from the code pad for instance.
            return;
        }

        // using the stack, try to find the source code
        boolean done = false;
        Iterator<SourceLocation> iter = stack.iterator();
        boolean firstTime = true;

        while (!done && iter.hasNext()) {
            SourceLocation loc = iter.next();
            String locFileName = loc.getFileName();
            if (locFileName != null) {
                String filename = new File(getPath(), locFileName).getPath();
                int lineNo = loc.getLineNumber();
                done = showEditorMessage(filename, lineNo, message, true, true);
                if (firstTime && !done) {
                    message += " (in " + loc.getClassName() + ")";
                    firstTime = false;
                }
            }
        }
        if (!done) {
            SourceLocation loc = stack.get(0);
            showMessageWithText("error-in-file", loc.getClassName() + ":" + loc.getLineNumber() + "\n" + message);
        }
    }
    
    /**
     * Displays the given class at the given line number (due to an exception, usually clicked-on stack trace).
     * 
     *  Simpler than the other exceptionMessage method because it requires less details 
     */
    public void exceptionMessage(String className, int lineNumber)
    {
        showEditorMessage(className, lineNumber, "", false, true);
    }

    /**
     * Report an execption. Usually, we do this through "errorMessage", but if
     * we cannot make sense of the message format, and thus cannot figure out
     * class name and line number, we use this way.
     */
    public void reportException(String text)
    {
        showMessageWithText("exception-thrown", text);
    }

    /**
     * Use the resource name in order to return the path of the jar file
     * containing the given resource.
     * <p>
     * If it is not in a jar file it returns the original resource path
     * (URL).
     * 
     * @param c  The class to get the path to
     * @return A string indicating the path of the jar file (if applicable
     * and if not, it returns the path/URL to the resource)
     */
    protected static String getResourcePath(Class<?> c)
    { 
        URL srcUrl = c.getResource(c.getSimpleName()+".class");
        try {
            if (srcUrl != null) {
                if (srcUrl.getProtocol().equals("file")) {
                    File srcFile = new File(srcUrl.toURI());
                    return srcFile.toString();
                }  
                if (srcUrl.getProtocol().equals("jar")){
                    //it should be of this format
                    //jar:file:/path!/class 
                    int classIndex = srcUrl.toString().indexOf("!");
                    String subUrl = srcUrl.toString().substring(4, classIndex);
                    if (subUrl.startsWith("file:")) {
                        return new File(new URI(subUrl)).toString();
                    }
                    
                    if (classIndex!=-1){
                        return srcUrl.toString().substring(4, classIndex);
                    }
                }
            }
            else {
                return null;
            }
        }
        catch (URISyntaxException uriSE) {
            // theoretically we can't get URISyntaxException; the URL should
            // be valid.
        }
        return srcUrl.toString();
    }
    
    /**
     * Check whether a loaded class was actually loaded from the specified class file
     * @param c  The loaded class
     * @param f  The class file to check against (should be a compiled .class file)
     * @return  True if the class was loaded from the specified file; false otherwise
     */
    public static boolean checkClassMatchesFile(Class<?> c, File f)
    {
        try {
            URL srcUrl = c.getResource(c.getSimpleName()+".class");
            if (srcUrl == null) {
                // If we weren't able to load the class file at all, it may have been
                // deleted; this happens when a class is added to a project, then
                // removed, and then another class is added with the same name.
                return true;
            }
            if (srcUrl != null && srcUrl.getProtocol().equals("file")) {
                File srcFile = new File(srcUrl.toURI());
                if (! f.equals(srcFile)) {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        catch (URISyntaxException uriSE) {
            // theoretically we can't get URISyntaxException; the URL should
            // be valid.
        }
        return true;
    }
    
    // ---- bluej.compiler.CompileObserver interfaces ----

    /**
     * Observe compilation jobs and change the PkgMgr interface elements as
     * compilation goes through different stages, but don't display the popups
     * for error/warning messages.
     * Also relay compilation events to any listening extensions.
     */
    private class QuietPackageCompileObserver
        implements FXCompileObserver
    {
        protected List<FXCompileObserver> chainedObservers;
        
        /**
         * Construct a new QuietPackageCompileObserver. The chained observers (if
         * non-empty list) are notified about each event.
         */
        public QuietPackageCompileObserver(List<FXCompileObserver> chainedObservers)
        {
            this.chainedObservers = new ArrayList<>(chainedObservers);
        }
        
        private void markAsCompiling(CompileInputFile[] sources, int compilationSequence)
        {
            for (int i = 0; i < sources.length; i++) {
                String fileName = sources[i].getJavaCompileInputFile().getPath();
                String fullName = getProject().convertPathToPackageName(fileName);

                if (fullName != null) {
                    Target t = getTarget(JavaNames.getBase(fullName));

                    if (t instanceof ClassTarget) {
                        ClassTarget ct = (ClassTarget) t;
                        ct.markCompiling(compilationSequence);
                    }
                }
            }
        }

        private void sendEventToExtensions(String filename, int [] errorPosition, String message, int eventType, CompileType type)
        {
            
        }

        /**
         * A compilation has been started. Mark the affected classes as being
         * currently compiled.
         */
        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence)
        {
            
        }

        @Override
        public boolean compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            int [] errorPosition = new int[4];
            errorPosition[0] = (int) diagnostic.getStartLine();
            errorPosition[1] = (int) diagnostic.getStartColumn();
            errorPosition[2] = (int) diagnostic.getEndLine();
            errorPosition[3] = (int) diagnostic.getEndColumn();
            if (diagnostic.getType() == Diagnostic.ERROR) {
                errorMessage(diagnostic.getFileName(), errorPosition, diagnostic.getMessage(), type);
            }
            else {
                warningMessage(diagnostic.getFileName(), errorPosition, diagnostic.getMessage(), type);
            }

            boolean shown = false;
            for (FXCompileObserver chainedObserver : chainedObservers)
            {
                // Don't inline the next two lines, as we
                // always want to call compilerMessage even if
                // a previous observer showed the method:
                boolean s = chainedObserver.compilerMessage(diagnostic, type);
                shown = shown || s;
            }
            return shown;
        }
        
        private void errorMessage(String filename, int [] errorPosition, String message, CompileType type)
        {
            // Send a compilation Error event to extensions.
            sendEventToExtensions(filename, errorPosition, message, CompileEvent.COMPILE_ERROR_EVENT, type);
        }

        private void warningMessage(String filename, int [] errorPosition, String message, CompileType type)
        {
            // Send a compilation Error event to extensions.
            sendEventToExtensions(filename, errorPosition, message, CompileEvent.COMPILE_WARNING_EVENT, type);
        }

        /**
         * Compilation has ended. Mark the affected classes as being normal
         * again.
         */
        @Override
        public void endCompile(CompileInputFile[] sources, boolean successful, CompileType type, int compilationSequence)
        {
            
        }
    }
    
    private static class MisspeltMethodChecker implements MessageCalculator
    {
        private static final int MAX_EDIT_DISTANCE = 2;
        private final String message;
        private int lineNumber;
        private int column;
        private Project project;

        public MisspeltMethodChecker(String message, int column, int lineNumber, Project project)
        {
            this.message = message;
            this.column = column;
            this.lineNumber = lineNumber;
            this.project = project;
        }
        
        private static String chopAtOpeningBracket(String name)
        {
            int openingBracket = name.indexOf('(');
            if (openingBracket >= 0)
                return name.substring(0,openingBracket);
            else
                return name;
        }

        private String getLine(Object e)
        {
            return null;
        }
        
        private int getLineStart(Object e)
        {
            return 0;
        }
        
        @Override
        public String calculateMessage(Editor e0)
        {
            return null;
        }
        
        /** 
         * Convert a column where a tab is counted as 8 to a column where a tab is counted
         * as 1
         */
        private static int convertColumn(String string, int column)
        {
            int ccount = 0; // count of characters
            int lpos = 0;   // count of columns (0 based)

            int tabIndex = string.indexOf('\t');
            while (tabIndex != -1 && lpos < column - 1) {
                lpos += tabIndex - ccount;
                ccount = tabIndex;
                if (lpos >= column - 1) {
                    break;
                }
                lpos = ((lpos + 8) / 8) * 8;  // tab!
                ccount += 1;
                tabIndex = string.indexOf('\t', ccount);
            }

            ccount += column - lpos;
            return ccount;
        }
    }

    /**
     * The same, but also display error/warning messages for the user
     */
    private class PackageCompileObserver extends QuietPackageCompileObserver
    {
        private int numErrors = 0;
        
        /**
         * Construct a new PackageCompileObserver. The chained observer (if specified)
         * is notified when the compilation ends.
         */
        public PackageCompileObserver(List<FXCompileObserver> chainedObservers)
        {
            super(chainedObservers);
        }
        
        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence)
        {
            numErrors = 0;
            super.startCompile(sources, reason, type, compilationSequence);
        }
        
        @Override
        public boolean compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            super.compilerMessage(diagnostic, type);
            if (diagnostic.getType() == Diagnostic.ERROR) {
                return errorMessage(diagnostic, type);
            }
            else {
                return warningMessage(diagnostic.getFileName(), (int) diagnostic.getStartLine(),
                        diagnostic.getMessage());
            }
        }
        
        /**
         * Display an error message associated with a specific line in a class.
         * This is done by opening the class's source, highlighting the line and
         * showing the message in the editor's information area.
         */
        private boolean errorMessage(Diagnostic diagnostic, CompileType type)
        {
            numErrors += 1;
            ErrorShown messageShown;

            if (diagnostic.getFileName() == null)
            {
                showMessageWithText("compiler-error", diagnostic.getMessage());
                return true;
            }
                
            String message = diagnostic.getMessage();
            // See if we can help the user a bit more if they've mis-spelt a method:
            if (message.contains("cannot find symbol") && message.contains("method")) {
                messageShown = showEditorDiagnostic(diagnostic,
                        new MisspeltMethodChecker(message,
                                (int) diagnostic.getStartColumn(),
                                (int) diagnostic.getStartLine(),
                                project), numErrors - 1, type);
            }
            else
            {
                messageShown = showEditorDiagnostic(diagnostic, null, numErrors - 1, type);
            }
            // Display the error message in the source editor
            switch (messageShown)
            {
            case EDITOR_NOT_FOUND:
                showMessageWithText("error-in-file", diagnostic.getFileName() + ":" +
                        diagnostic.getStartLine() + "\n" + message);
                return true;
            case ERROR_SHOWN:
                return true;
            default:
                return false;
            }
        }

        /**
         * Display a warning message: just a dialog box
         * The dialog accumulates messages until reset() is called, which is
         * done in the methods which the user can invoke to cause compilation
         * Thus all the warnings caused by a "compilation" can be accumulated
         * into a single dialog.
         * If searchCompile() built a single list, we wouldn't need to do this
         */
        private boolean warningMessage(String filename, int lineNo, String message)
        {
            return true;
        }
    }

    // ---- end of bluej.compiler.CompileObserver interfaces ----

    /**
     * closeAllEditors - closes all currently open editors within package Should
     * be run whenever a package is removed from PkgFrame.
     */
    public void closeAllEditors()
    {
        
    }

    /**
     * get history of invocation calls
     * 
     * @return CallHistory object
     */
    public CallHistory getCallHistory()
    {
        return callHistory;
    }

    /**
     * String representation for debugging.
     */
    public String toString()
    {
        return "Package:" + getQualifiedName();
    }

    public SourceType getDefaultSourceType()
    {
        // Our heuristic is: if the package contains any Stride files, the default is Stride,
        // otherwise it's Java
        if (getClassTargets().stream().anyMatch(c -> c.getSourceType() == SourceType.Stride))
            return SourceType.Stride;
        else
            return SourceType.Java;
    }


    public void addDependency(Object dependency)
    {
        
    }

    public void addDependency(Object d, boolean recalc)
    {
        
    }
    
    public void removeDependency(Object dependency, boolean recalc)
    {
        
    }

    /**
     * Call the given method or constructor.
     */
    public void callStaticMethodOrConstructor(CallableView view)
    {
        ui.callStaticMethodOrConstructor(view);
    }

    /**
     * Add an observer to listen to all compilations of this package.
     */
    public void addCompileObserver(FXCompileObserver fxCompileObserver)
    {
        compileObservers.add(fxCompileObserver);
    }

    /**
     * Checks if the class target has dependencies that have compilation errors
     * @param  classTarget the class target whom direct/indirect dependencies will be checked
     * @return true if any of the dependencies or their ancestors have a compilation error
     *         otherwise it returns false
     */
    public boolean checkDependecyCompilationError(ClassTarget classTarget)
    {
        return false;
    }
}
