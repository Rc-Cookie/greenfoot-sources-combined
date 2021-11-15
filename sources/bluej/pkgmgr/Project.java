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
package bluej.pkgmgr;

import bluej.Boot;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugger.DebuggerThreadListener;
import bluej.debugger.RunOnThread;
import bluej.debugmgr.ExecControls;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.editor.Editor;
import bluej.editor.stride.FXTabbedEditor;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.terminal.Terminal;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.FileUtility;
import bluej.utility.ImportScanner;
import bluej.utility.JavaNames;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;


/**
 * A BlueJ Project.
 *
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Andrew Patterson
 * @author  Bruce Quig
 */
@SuppressWarnings("unused")
public class Project implements DebuggerListener, DebuggerThreadListener, InspectorManager
{
    public static final int NEW_PACKAGE_DONE = 0;
    public static final int NEW_PACKAGE_EXIST = 1;
    public static final int NEW_PACKAGE_BAD_NAME = 2;
    public static final int NEW_PACKAGE_NO_PARENT = 3;
    public static final String projectLibDirName = "+libs";
    /** Property specifying location of JDK source */
    private static final String JDK_SOURCE_PATH_PROPERTY = "bluej.jdk.source";
    private static final String PROJECT_CHARSET_PROP = "project.charset";
    public static final String RUN_ON_THREAD_PROP = "project.invoke.thread";
    /**
     * Collection of all open projects. The canonical name of the project
     * directory (as a File object) is used as the key.
     */
    private static Map<File,Project> projects = new HashMap<File,Project>();
    
    /* ------------------- end of static declarations ------------------ */

    // instance fields
    /** the path of the project directory. */
    private final File projectDir;
    /** reference to the unnamed package */
    @OnThread(Tag.Any) private final Package unnamedPackage;
    /** Resolve javadoc for this project */
    private final Object javadocResolver;
    /** collection of open packages in this project
      (indexed by the qualifiedName of the package).
       The unnamed package ie root package of the package tree
       can be obtained by retrieving "" from this collection */
    private Map<String, Package> packages;
    /** the debugger for this project */
    @OnThread(Tag.Any)
    private final Debugger debugger;
    /** the ExecControls for this project */
    private ExecControls execControls = null;
    /** the Terminal for this project */
    private Terminal terminal = null;
    /** the documentation generator for this project. */
    private Object docuGenerator;
    /** when a project is opened, the user may specify a
       directory deep into the projects directory structure.
       BlueJ will correctly find the top of this package
       heirarchy but the bit of the name left over will be
       put into this variable
       ie if opening /home/user/foo/com/sun where
       /home/user/foo is the project directory, this variable
       will be set to com.sun */
    private String initialPackageName = "";
    /** This holds all object inspectors and class inspectors
        for a project. It should only hold object inspectors that
        have a wrapper on the object bench. Inspectors of fields of
        object inspectors should be handled at the object wrapper level */
    @OnThread(Tag.FXPlatform)
    private Map<Object,Inspector> inspectors;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private boolean inTestMode = false;
    private Object currentClassLoader;
    private List<URL> libraryUrls;
    // the TeamSettingsController for this project
    private Object teamSettingsController = null;
    private Object commitCommentsFrame = null;
    private Object updateFilesFrame = null;
    private Object statusFrame = null;
    /** If true, this project is connected with a source repository */
    /** If empty, not checked yet */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Optional<Boolean> isSharedProject = Optional.empty();

    // team actions
    private Object teamActions;
    /** Where to find JDK and other library sources (for extracting javadoc) */
    private List<DocPathEntry> sourcePath;

    /** Project character set for source files etc */
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private Charset characterSet;

    @OnThread(Tag.FX) private final List<FXTabbedEditor> fXTabbedEditors = new ArrayList<>();
    @OnThread(Tag.FX) private final List<Rectangle> fxCachedEditorSizes = new ArrayList<>();

    /** 1 second timer before starting auto-compile */
    @OnThread(Tag.FXPlatform)
    private Timeline compilerTimer;
    // We don't used synchronized here because we could deadlock:
    @OnThread(Tag.FXPlatform)
    private CompileReason latestCompileReason;
    // We don't used synchronized here because we could deadlock:
    @OnThread(Tag.FXPlatform)
    private CompileType latestCompileType;
    /** Packages scheduled for autocompilation */
    @OnThread(Tag.FXPlatform)
    private Set<Package> scheduledPkgs = new HashSet<>();
    /** Targets scheduled for autocompilation */
    @OnThread(Tag.FXPlatform)
    private Set<ClassTarget> scheduledTargets = new HashSet<>();

    /**
     * The threads currently running in the debugger for this project.  We need
     * to track this in Project because ExecControls may not be showing from the
     * outset, but we need the state of the threads to be available when
     * we do show ExecControls, so Project must keep track ready for ExecControls
     * to potentially be shown later on
     */
    private final ObservableList<DebuggerThreadDetails> threadListContents = FXCollections.observableArrayList();

    private Object singleBProject;  // Every Project has none or one BProject
    private boolean closing = false;
    /** The scanner for available imports.  May be null if not requested yet. */
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private ImportScanner importScanner;

    /** check if the project is a dvcs project**/
    private boolean isDVCS=false;
    private final Object shelfStorage;
    private final BooleanProperty terminalShowing = new SimpleBooleanProperty(false);
    private final BooleanProperty debuggerShowing = new SimpleBooleanProperty(false);
    // Which thread to run on.  null means we have never asked the user about it.
    private RunOnThread runOnThread;

    /* ------------------- end of field declarations ------------------- */

    /**
     * Construct a project in the directory projectDir.
     * This must contain the root bluej.pkg of a nested package
     * (this should by its nature be the unnamed package).
     */
    private Project(File projectDir) throws IOException
    {
        debugger = null;
        javadocResolver = null;
        this.projectDir = null;
        shelfStorage = null;
        unnamedPackage = null;
    }

    /**
     * Check if the path given is either a directory with a project file or if
     * it is the project file itself (project.greenfoot or package.bluej).
     *
     * @param projectPath a string representing the path to check. This can
     *            either be a directory name or the filename of a project
     *            file.
     */
    @OnThread(Tag.Any)
    public static boolean isProject(String projectPath)
    {
        File startingDir;

        try {
            startingDir = pathIntoStartingDirectory(projectPath);
        } catch (IOException ioe) {
            return false;
        }

        if (startingDir == null) {
            return false;
        }

        return (Package.isPackage(startingDir));
    }

    /**
     * Open a BlueJ project (or find an existing open project).
     *
     * @param projectPath
     *            a string representing the path to open. This can either be a
     *            directory name or the filename of a bluej.pkg file.
     * @return the Project representing the BlueJ project that has this
     *         directory within it or null if there were no bluej.pkg files in
     *         the specified directory.
     */
    public static Project openProject(String projectPath)
    {
        return null;
    }

    /**
     * CleanUp the mess left by a project that has now been closed and
     * throw it away.
     */
    public static void cleanUp(Project project)
    {
        
    }

    /**
     * Create a new project in the directory specified by projectPath.
     * This name must be a directory that does not already exist.
     *
     * @param   projectPath     a string representing the path in which
     *                          to make the new project
     * @return                  a boolean indicating success or failure
     */
    @OnThread(Tag.Any)
    public static boolean createNewProject(String projectPath)
    {
        if (projectPath != null) {
            // check whether name is already in use
            File dir = new File(projectPath);

            if (dir.exists() && (!dir.isDirectory() || dir.list().length > 0)) {
                return false;
            }

            if (dir.exists() || dir.mkdir()) {
                File newreadmeFile = new File(dir, Package.readmeName);

                PackageFile pkgFile = PackageFileFactory.getPackageFile(dir);
                try {
                    if (pkgFile.create()) {
                        Properties props = new Properties();
                        if (Config.isGreenfoot())
                        {
                            // Set the size to contain the default new world size, to avoid
                            // annoying sizing up and down when loading a new project:
                            props.put("mainWindow.width", "850");
                            props.put("mainWindow.height", "600");
                            // Must set x and y, or width and height don't take effect:
                            props.put("mainWindow.x", "40");
                            props.put("mainWindow.y", "40");
                        }
                        props.put(PROJECT_CHARSET_PROP, "UTF-8");
                        try {
                            pkgFile.save(props);
                            FileUtility.copyFile(Config.getTemplateFile(
                                    "readme"), newreadmeFile);
                            return true;
                        }
                        catch (IOException ioe) {
                            // TODO should propagate this exception
                            Debug.message("I/O error while creating project: " + ioe.getMessage());
                        }
                    }
                } catch (IOException ioe) {
                    // TODO should propagate this exception
                }
            }
        }

        Debug.message("Unable to create project directory: " + projectPath);
        return false;
    }

    /**
     * Returns the number of open projects
     */
    public static int getOpenProjectCount()
    {
        return projects.size();
    }

    /**
     * Gets the set of currently open projects. It is an accessor only.
     * @return a Set containing all open projects.
     */
    public static Collection<Project> getProjects()
    {
        return projects.values();
    }

    /**
     * Given a Projects key returns the Project objects describing this projects.
     */
    public static Project getProject(File projectKey)
    {
        return projects.get(projectKey);
    }

    /**
     * Helper function to take a path (either a directory or a file)
     * and return either the canonical path to the directory
     * (in the case of a bluej.pkg file passed in, return the directory containing
     * the file. Returns null if file is not a bluej.pkg file or if the
     * directory/file does not exist.
     */
    @OnThread(Tag.Any)
    private static File pathIntoStartingDirectory(String projectPath)
            throws IOException
    {
        File startingDir;

        startingDir = new File(projectPath).getCanonicalFile();

        if (startingDir.isDirectory()) {
            return startingDir;
        }

        /* allow a bluej.pkg file to be specified. In this case,
           we immediately find the parent directory and use that as the
           starting directory */
        if (startingDir.isFile()) {
            if (Package.isPackageFileName(startingDir.getName())) {
                return startingDir.getParentFile();
            }
        }

        return null;
    }

    /**
     * Attempts to add a library to the given list of libraries.
     * A valid library file is one that is a file, readable, ends either with zip or jar.
     * Before addition the file is transformed to a URL.
     * @param risul where to add the file
     * @param aFile the file to be added.
     */
    @OnThread(Tag.Any)
    private static final void attemptAddLibrary (List<URL> risul, File aFile)
    {
        if ( aFile == null ) return;

        // Is this a normal file and is it readable ?
        if ( ! (aFile.isFile() && aFile.canRead()) ) return;

        String libname = aFile.getName().toLowerCase();
        if ( ! (libname.endsWith(".jar") || libname.endsWith(".zip")) ) return;

        try {
            risul.add(aFile.toURI().toURL());
        }
        catch(MalformedURLException mue) {
            Debug.reportError("Project.attemptAddLibrary() malformaed file="+aFile);
        }
    }

    /**
     * Returns an array of URLs for all the JAR files located in the lib/userlib directory.
     * The result is calculated every time the method is called, in this way it is possible
     * to capture a change in the library content in a reasonable timing.
     *
     * @return  URLs of the discovered JAR files
     */
    @OnThread(Tag.Any)
    public static final List<URL> getUserlibContent()
    {
        List<URL> risul = new ArrayList<URL>();
        File userLibDir;

        // The userlib location may be specified in bluej.defs
        String userLibSetting = Config.getPropString("bluej.userlibLocation", null);
        if (userLibSetting == null) {
            userLibDir = new File(Boot.getBluejLibDir(), "userlib");
        }
        else {
            userLibDir = new File(userLibSetting);
        }

        File[] files = userLibDir.listFiles();
        if (files == null) {
            return risul;
        }

        for (int index = 0; index < files.length; index++) {
            attemptAddLibrary(risul, files[index]);
        }

        return risul;
    }

    /**
     * Get the character set that should be used for reading and writing source files
     * in this project. 
     */
    public synchronized Charset getProjectCharset()
    {
        return characterSet;
    }

    /**
     * Get the project properties to be written to storage when the project is saved.
     */
    @OnThread(Tag.FXPlatform)
    public synchronized Properties getProjectPropertiesCopy()
    {
        Properties p = new Properties();
        p.put(PROJECT_CHARSET_PROP, characterSet.name());
        if (runOnThread != null)
            p.put(RUN_ON_THREAD_PROP, runOnThread.name());
        return p;
    }

    /**
     * Restore project properties (called just after project is opened). 
     */
    private synchronized void loadProjectProperties(Properties props)
    {
        String charsetName = props.getProperty(PROJECT_CHARSET_PROP);
        if (charsetName != null)
        {
            try
            {
                characterSet = Charset.forName(charsetName);
            }
            catch (IllegalCharsetNameException icne)
            {
                Debug.message("Illegal project character set name: " + charsetName);
            }
            catch (UnsupportedCharsetException ucse)
            {
                Debug.message("Unsupported project character set: " + charsetName);
            }
        }
        if (characterSet == null)
        {
            characterSet = Charset.defaultCharset();
            props.put(PROJECT_CHARSET_PROP, characterSet.name());
        }

        String runOnThreadProp = props.getProperty(RUN_ON_THREAD_PROP);
        try
        {
            // Note that the null value is checked for explicitly and means "prompt if
            // running an FX application". So, rather than set to DEFAULT, leave as null
            // if the property isn't set:
            runOnThread = runOnThreadProp == null || runOnThreadProp.isEmpty() ?
                    null : RunOnThread.valueOf(runOnThreadProp);
        }
        catch (IllegalArgumentException iae)
        {
            // Property was set to an invalid setting
            Debug.message("Invalid run-on-thread setting: " + runOnThreadProp);
        }
    }

    /**
     * Update an inspector, make sure it's visible, and bring it to
     * the front.
     *
     * @param inspector  The inspector to update and show
     */
    @OnThread(Tag.FXPlatform)
    private void updateInspector(final Inspector inspector)
    {
        inspector.update();
        inspector.show();
        inspector.bringToFront();
    }

    /**
     * Return an ObjectInspector for an object.
     *
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to (may be null)
     * @param ir
     *            the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param parent
     *            The parent frame of this frame
     * @param animateFromCentre If non-null, animate from the centre of this node.
     * @return The Viewer value
     */
    @OnThread(Tag.FXPlatform)
    public ObjectInspector getInspectorInstance(DebuggerObject obj,
                                                String name, Package pkg, InvokerRecord ir, Window parent, Node animateFromCentre)
    {
        return null;
    }

    @OnThread(Tag.FXPlatform)
    private void animateInspector(Node animateFromCentre, Inspector inspector, boolean fromBottom)
    {
        // First we must get the root and make sure it's sized for our later calculations:
        Parent root = inspector.getContent();
        root.applyCss();
        root.layout();

        ScaleTransition t = null;
        // Odd JavaFX behaviour on Linux messes up animation, so don't animate on Linux:
        if (!Config.isLinux())
        {
            // Start it at zero size, animating to full size:
            root.setScaleX(0.0);
            root.setScaleY(0.0);
            t = new ScaleTransition(Duration.millis(600), root);
            t.setInterpolator(Interpolator.EASE_OUT);
            t.setToX(1.0);
            t.setToY(1.0);
            // To animate from left, need to start at position -0.5 of width, then animate to 0.0
            root.translateXProperty().bind(inspector.getScene().widthProperty().multiply(root.scaleXProperty().multiply(0.5).add(-0.5)));
            if (fromBottom)
            {
                // To animate from bottom, need to start at position 0.5 of height, then animate to 0.0
                root.translateYProperty().bind(inspector.getScene().heightProperty().multiply(root.scaleYProperty().multiply(-0.5).add(0.5)));
            } else
            {
                // To animate from top, need to start at position -0.5 of height, then animate to 0.0
                root.translateYProperty().bind(inspector.getScene().heightProperty().multiply(root.scaleYProperty().multiply(0.5).add(-0.5)));
            }
        }
        // Position its bottom left at centre of animateFromCentre:
        Scene afcScene = animateFromCentre.getScene();
        final Point2D windowCoord = new Point2D(afcScene.getWindow().getX(), afcScene.getWindow().getY());
        final Point2D sceneCoord = new Point2D(afcScene.getX(), afcScene.getY());
        final Point2D nodeCoord = animateFromCentre.localToScene(animateFromCentre.getBoundsInLocal().getWidth()/2.0, animateFromCentre.getBoundsInLocal().getHeight()/2.0);

        // Set position:
        inspector.setX(windowCoord.getX() + sceneCoord.getX() + nodeCoord.getX());
        inspector.setY(windowCoord.getY() + sceneCoord.getY() + nodeCoord.getY() + (fromBottom ? -root.prefHeight(-1) : 0));

        if (t != null)
        {
            t.play();
        }
    }

    /**
     * Get the inspector for the given object. Object can be a DebuggerObject, or a
     * fully-qualified class name.
     *
     * @param obj  The object whose inspector to retrieve
     * @return the inspector, or null if no inspector is open
     */
    @OnThread(Tag.FXPlatform)
    public Inspector getInspector(Object obj)
    {
        return inspectors.get(obj);
    }

    /**
     * Remove an inspector from the list of inspectors for this project
     * @param obj the inspector.
     */
    @OnThread(Tag.FXPlatform)
    public void removeInspector(DebuggerObject obj)
    {
        Inspector inspector = inspectors.remove(obj);
        DataCollector.inspectorHide(this, inspector);
    }

    /**
     * Remove an inspector from the list of inspectors for this project
     * @param obj the inspector. 
     */
    @OnThread(Tag.FXPlatform)
    public void removeInspector(DebuggerClass cls)
    {
        Inspector inspector = inspectors.remove(cls.getName());
        DataCollector.inspectorHide(this, inspector);
    }

    /**
     * Removes an inspector instance from the collection of inspectors
     * for this project. It firstly retrieves the inspector object and
     * then calls its doClose method.
     * @param obj
     */
    @OnThread(Tag.FXPlatform)
    public void removeInspectorInstance(Object obj)
    {
        Inspector inspect = getInspector(obj);

        if (inspect != null) {
            inspect.doClose(false);
        }
    }

    /**
     * Removes all inspector instances for this project.
     * This is used when VM is reset or the project is recompiled.
     *
     */
    @OnThread(Tag.FXPlatform)
    public void removeAllInspectors()
    {
        for (Inspector inspector : inspectors.values()) {
            inspector.hide();
            DataCollector.inspectorHide(this, inspector);
        }

        inspectors.clear();
    }

    /**
     * Return a ClassInspector for a class. The inspector is visible.
     *
     * @param name
     *            The name of this object or "null" if it is not on the object bench
     * @param getEnabled
     *            if false, the "get" button is permanently disabled
     * @param clss
     *            The class displayed by this viewer
     * @param pkg
     *            The package associated with the request (may be null)
     * @param parent
     *            The parent frame of this frame
     * @param animateFromCentre
     *            A node representing the initiator of the inspect action for animation purposes
     *            (may be null).
     * @return The Viewer value
     */
    @OnThread(Tag.FXPlatform)
    public ClassInspector getClassInspectorInstance(DebuggerClass clss,
                                                    Package pkg, Window parent, Node animateFromCentre)
    {
        return null;
    }

    /**
     * Return an ObjectInspector for an object. The inspector is visible.
     *
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param info
     *            The information about the the expression that gave this result
     * @param parent
     *            The parent frame of this frame
     * @return The Viewer value
     */
    @OnThread(Tag.FXPlatform)
    public ResultInspector getResultInspectorInstance(DebuggerObject obj,
                                                      String name, Package pkg, InvokerRecord ir, ExpressionInformation info,
                                                      javafx.stage.Window parent)
    {
        final ResultInspector inspector = new ResultInspector(obj, this, name, pkg, ir, info);
        inspectors.put(obj, inspector);

        inspector.initOwner(parent);
        inspector.centerOnOwner();
        inspector.show();
        inspector.bringToFront();

        return inspector;
    }

    /**
     * Iterates through all inspectors and updates them
     *
     */
    @OnThread(Tag.FXPlatform)
    public void updateInspectors()
    {
        for (Iterator<Inspector> it = inspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = it.next();
            inspector.update();
        }
    }

    /**
     * Return the name of the project.
     */
    @OnThread(Tag.Any)
    public String getProjectName()
    {
        return projectDir.getName();
    }

    /**
     * Return the location of the project.
     */
    @OnThread(Tag.Any)
    public File getProjectDir()
    {
        return projectDir;
    }

    /**
     * Return the source path for the project. The source path contains the JDK source,
     * if available, and the source for any other libraries which have been explicitly
     * added.
     */
    public List<DocPathEntry> getSourcePath()
    {
        return sourcePath;
    }

    /**
     * Get the project repository. If the user cancels the credentials dialog,
     * or this is not a team project, returns null.
     */
    public Object getRepository()
    {
        return null;
    }

    /**
     * A string which uniquely identifies this project
     */
    @OnThread(Tag.Any)
    public String getUniqueId()
    {
        return String.valueOf(new String("BJID" + getProjectDir().getPath()).hashCode());
    }

    /**
     * Get the name of the package represented by the directory which was specified
     * as the directory to open when this project was opened.
     */
    public String getInitialPackageName()
    {
        return initialPackageName;
    }

    /**
     * Get a reference to the "unnamed" package
     */
    @OnThread(Tag.Any)
    public Package getUnnamedPackage()
    {
        return unnamedPackage;
    }

    /**
     * Get an existing package from the project. The package is opened (i.e a new
     * Package object is constructed) if it's not already open. All parent packages on
     * the way to the root of the package tree will also be constructed.
     *
     * @param qualifiedName package name i.e. java.util or "" for unnamed package
     * @returns  the package, or null if the package doesn't exist (directory
     *           doesn't exist, or doesn't contain bluej.pkg file)
     */
    public Package getPackage(String qualifiedName)
    {
        Package existing = packages.get(qualifiedName);

        if (existing != null) {
            // The unnamed package is always already open, so that case is
            // handled here.
            return existing;
        }

        if (qualifiedName.length() > 0) {
            Package pkg;

            try {
                Package parent = getPackage(JavaNames.getPrefix(qualifiedName));

                if (parent != null) {
                    // Note, construction of the new package throws IOException if
                    // the directory or bluej.pkg file doesn't exist
                    pkg = new Package(this, JavaNames.getBase(qualifiedName),
                            parent);
                    packages.put(qualifiedName, pkg);
                    pkg.refreshPackage();
                } else { // parent package does not exist. How can it not exist ?
                    pkg = null;
                }
            } catch (IOException exc) {
                // the package did not exist in this project
                pkg = null;
            }

            return pkg;
        }

        // Default package is not in the package cache. This should never happen...
        throw new IllegalStateException("Project.getPackage()");
    }

    /**
     * Get all packages from the project. The returned collection is a live view
     * and should not be modified directly.
     * @return  all the packages in the current project.
     */
    public Collection<Package> getProjectPackages()
    {
        return packages.values();
    }

    /**
     * Return the extensions BProject associated with this Project.
     * There should be only one BProject object associated with each Project.
     * @return the BProject associated with this Project.
     */
    public synchronized final Object getBProject ()
    {
        return null;
    }

    /**
     * Returns a package from the project. The package must have already been
     * opened.
     *
     * @param qualifiedName package name ie java.util or "" for unnamed package
     * @return null if the named package cannot be found
     */
    public Package getCachedPackage(String qualifiedName)
    {
        return packages.get(qualifiedName);
    }

    /**
     * This creates package directories. For the given package, all
     * intermediate package directories (which do not already exist)
     * will be created. A bluej.pkg file will be created for each
     * directory (if it does not already exist).
     *
     * @param fullName  the fully qualified name of the package to create
     *                  directories for
     */
    public void createPackageDirectory(String fullName)
    {
        // construct the directory name for the new package
        StringTokenizer st = new StringTokenizer(fullName, ".");
        File newPkgDir = getProjectDir();

        while (st.hasMoreTokens())
            newPkgDir = new File(newPkgDir, st.nextToken());

        // now actually construct the directories and add the bluej
        // package marker files
        if (newPkgDir.isDirectory() || newPkgDir.mkdirs()) {
            st = new StringTokenizer(fullName, ".");
            newPkgDir = getProjectDir();
            PackageFile pkgFile = PackageFileFactory.getPackageFile(newPkgDir);
            try {
                pkgFile.create();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            while (st.hasMoreTokens()) {
                newPkgDir = new File(newPkgDir, st.nextToken());
                prepareCreateDir(newPkgDir);

                pkgFile = PackageFileFactory.getPackageFile(newPkgDir);
                try {
                    pkgFile.create();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns a new package with the given fully qualified name. Once
     * NEW_PACKAGE_DONE is returned you can use getPackage to get the actual
     * package.
     *
     * @param qualifiedName
     *            Ex. java.util or "" for unnamed package
     * @return Project.NEW_PACKAGE_DONE, Project.NEW_PACKAGE_EXIST,
     *         Project.NEW_PACKAGE_BAD_NAME
     */
    public int newPackage(String qualifiedName)
    {
        if (qualifiedName == null) {
            return NEW_PACKAGE_BAD_NAME;
        }

        Package existing = packages.get(qualifiedName);

        if (existing != null) {
            return NEW_PACKAGE_EXIST;
        }

        // The zero len (unqualified) package should always exist.
        if (qualifiedName.length() < 1) {
            return NEW_PACKAGE_BAD_NAME;
        }

        // The above named package does not exist, lets create it.
        try {
            Package parent = getPackage(JavaNames.getPrefix(qualifiedName));

            if (parent == null) {
                return NEW_PACKAGE_NO_PARENT;
            }

            // Before creating the package you have to create the directory
            // Maybe it should go into the new Package(...)
            createPackageDirectory(qualifiedName);

            Package pkg = new Package(this, JavaNames.getBase(qualifiedName),
                    parent);
            packages.put(qualifiedName, pkg);
            pkg.refreshPackage();
        } catch (IOException exc) {
            return NEW_PACKAGE_BAD_NAME;
        }

        return NEW_PACKAGE_DONE;
    }

    /**
     * Get the names of all packages in this project consisting of rootPackage
     * package and all packages nested below it.
     *
     * @param rootPackage
     *            the root package to consider in looking for nested packages
     * @return a List of String containing the fully qualified names of the
     *         packages.
     */
    private List<String> getPackageNames(Package rootPackage)
    {
        List<String> l = new LinkedList<String>();

        l.add(rootPackage.getQualifiedName());
        rootPackage.getChildren(true).forEach(p -> l.addAll(getPackageNames(p)));

        return l;
    }

    /**
     * Get the names of all packages in this project.
     *
     * @return  a List of String containing the fully qualified names
     *          of the packages in this project.
     */
    public List<String> getPackageNames()
    {
        return getPackageNames(getPackage(""));
    }

    /**
     * Generate documentation for the whole project.
     * @return "" if everything was alright, an error message otherwise.
     */
    public String generateDocumentation()
    {
        return null;
    }

    public String getDocumentationFile(String filename)
    {
        return null;
    }

    /**
     * Generate the documentation for the file in 'filename'
     * @param filename
     */
    public void generateDocumentation(String filename)
    {
        
    }

    /**
     * Save all open packages of this project. This doesn't save files open
     * in editor windows - use saveAllEditors() for that.
     */
    public void saveAll()
    {
        
    }

    /**
     * Request all open editor windows for the current project to save their
     * contents (if modified).
     */
    public void saveAllEditors() throws IOException
    {
        Iterator<Package> i = packages.values().iterator();
        IOException exception = null;

        while(i.hasNext()) {
            Package pkg = i.next();
            try {
                pkg.saveFilesInEditors();
            }
            catch(IOException ioe) {
                exception = ioe;
                Debug.reportError("Error while trying to save editor file:", ioe);
            }
        }

        if (exception != null) {
            // Propagate the exception - let the caller know that something went wrong.
            throw exception;
        }
    }

    /**
     * Reload all constructed packages of this project.
     *
     * <p>This function is used after a major change to the contents
     * of the project directory ie an import.
     */
    public void reloadAll()
    {
        packages.values().forEach(Package::reload);
    }

    /**
     * Make all open package editors clear their selection
     */
    public void clearAllSelections()
    {
        
    }

    /**
     * Make the package editors for this project select the targets given in the 
     * <code>targets</code> parameter. Pre-existing selections remain selected. Targets in a
     * package with no editor open will not be selected.
     *
     * @param targets  a list of Targets 
     */
    public void selectTargetsInGraphs(List<Target> targets)
    {
        
    }

    /**
     * Given a fully-qualified target name, return the target or null if the target
     * doesn't exist.
     *
     * <p>Use ReadmeTarget.README_ID ("@README") as the target base name to get the
     * readme target for a package.
     */
    public Target getTarget(String targetId)
    {
        String packageName = "";
        int index = targetId.lastIndexOf('.');
        if (index > 0) {
            packageName = targetId.substring(0, index);
            targetId = targetId.substring(index + 1);
        }
        Package p = getPackage(packageName);
        if (p == null) {
            return null;
        }
        Target target = p.getTarget(targetId);
        return target;
    }

    /**
     * Open the source editor for each target that is selected in its
     * package editor
     */
    public void openEditorsForSelectedTargets()
    {
        List<Target> selectedTargets = getSelectedTargets();
        for (Iterator<Target> i = selectedTargets.iterator(); i.hasNext(); ){
            Target target = i.next();
            if (target instanceof ClassTarget){
                ClassTarget classTarget = (ClassTarget) target;
                Editor editor = classTarget.getEditor();
                if (editor != null) {
                    editor.setEditorVisible(true, false);
                }
            }
        }
    }

    /**
     * Get a list of selected targets (in all packages)
     * @return a list of the selected targets
     */
    private List<Target> getSelectedTargets()
    {
        List<Target> selectedTargets = new LinkedList<Target>();
        List<String> packageNames = getPackageNames();
        for (String packageName : packageNames) {
            Package p = getPackage(packageName);
            selectedTargets.addAll(p.getSelectedTargets());
        }
        return selectedTargets;
    }

    /**
     * Explicitly restart the remote debug VM. The VM first gets shut down, and then
     * restarted.
     */
    public void restartVM()
    {
        
    }

    /**
     * The remote VM for this project has just been initialised and is ready now.
     */
    private void vmReady()
    {
        
    }

    /**
     * The remote VM for this project has just been closed. Remove everything in this
     * project that depended on that VM.
     */
    private void vmClosed()
    {
        // any calls to the debugger made by removeLocalClassLoader
        // will silently fail
        removeClassLoader();

        // The configured extra libraries may have changed, so
        // rebuild the class loader (do this now so the new loader
        // will be installed as soon as the VM has restarted).
        newRemoteClassLoader();

        libraryUrls = getLibrariesClasspath();
        debugger.setUserLibraries(libraryUrls.toArray(new URL[libraryUrls.size()]));

        // Breakpoints will be re-initialized once the new VM has
        // actually started.
    }

    /**
     * Removes the current classloader, and removes
     * references to classes loaded by it (this includes removing
     * the objects from all object benches of this project).
     */
    public void removeClassLoader()
    {
        
    }

    /**
     * Clears the objects from all object benches belonging to this project.
     */
    public void clearObjectBenches()
    {
        
    }

    /**
     * Creates a new debugging VM classloader.
     * Breakpoints are discarded.
     */
    public void newRemoteClassLoader()
    {
        getDebugger().newClassLoader(getClassLoader());
    }

    /**
     * Creates a new debugging VM classloader, leaving current breakpoints.
     */
    public void newRemoteClassLoaderLeavingBreakpoints()
    {
        getDebugger().newClassLoader(getClassLoader());
        packages.values().forEach(Package::reInitBreakpoints);
    }

    @OnThread(Tag.Any)
    public Debugger getDebugger()
    {
        return debugger;
    }

    public boolean hasExecControls()
    {
        return execControls != null;
    }

    public ExecControls getExecControls()
    {
        if (execControls == null) {
            execControls = new ExecControls(this, getDebugger(),
                    Config.isGreenfoot() ? null : threadListContents);
            debuggerShowing.bindBidirectional(execControls.showingProperty());
        }
        return execControls;
    }

    public boolean hasTerminal()
    {
        return terminal != null;
    }

    public Terminal getTerminal()
    {
        if (terminal == null) {
            terminal = new Terminal(this);
            terminalShowing.bindBidirectional(terminal.showingProperty());
        }
        return terminal;
    }

    /**
     * Loads a class using the current classLoader
     */
    public Class<?> loadClass(String className)
    {
        return null;
    }

    @OnThread(value = Tag.Any, ignoreParent = true)
    public synchronized boolean inTestMode()
    {
        return inTestMode;
    }

    public synchronized void setTestMode(boolean mode)
    {
        inTestMode = mode;
    }

    /**
     * Returns a list of URL having in it all libraries that are in the +libs directory
     * of this project.
     * @return a non null but possibly empty list of URL.
     */
    @OnThread(Tag.Any)
    protected List<URL> getPlusLibsContent ()
    {
        List<URL> risul = new ArrayList<URL>();

        // the subdirectory of the project which can hold project specific jars and zips
        File libsDirectory = new File(projectDir, projectLibDirName);

        // If it is not a directory or we cannot read it then there is nothing to do.
        if ( ! libsDirectory.isDirectory() || ! libsDirectory.canRead() )
            return risul;

        // the list of jars and zips we find
        File []libs = libsDirectory.listFiles();

        // If there are no files there then again just return.
        if ( libs==null || libs.length < 1 )
            return risul;

        // if we found any jar files in the libs directory then add their URLs
        for(int index=0; index<libs.length; index++) {
            attemptAddLibrary(risul, libs[index]);
        }
        return risul;
    }

    /**
     * Return a ClassLoader that should be used to load or reflect on the project classes.
     * The same BClassLoader object is returned until the Project is compiled or the content of the
     * user class list is changed, this is needed to load "compatible" classes in the same classloader space.
     *
     * @return a BClassLoader that provides class loading services for this Project.
     */
    public Object getClassLoader()
    {
        return null;
    }

    /**
     * Get the classpath for libraries - those specified in preferences, in the project's +libs,
     * in the BlueJ userlib folder, etc. This doesn't include the BlueJ runtime.
     *
     * <p>Most of the time, the list in {@code libraryUrls} should be used instead of calling this
     * method, as it represents the libraries known to the currently executing VM.
     */
    private List<URL> getLibrariesClasspath()
    {
        return null;
    }

    /**
     * Get an entity resolver which can be used to resolve symbols for this project.
     *
     * @return an entity resolver which resolves symbols from classes in this project,
     *         and from the classpath.
     */
    public Object getEntityResolver()
    {
        return null;
    }

    /**
     * Get a javadoc resolver, which can be used to retrieve comments for methods.
     */
    @OnThread(Tag.Any)
    public Object getJavadocResolver()
    {
        return null;
    }

    /**
     * Convert a filename into a fully qualified Java name.
     * Returns null if the file is outside the project
     * directory.
     *
     * The behaviour of this function is not guaranteed if
     * you pass in a directory name. It is meant for filenames
     * like /foo/bar/p1/s1/TestName.java
     */
    public String convertPathToPackageName(String pathname)
    {
        return JavaNames.convertFileToQualifiedName(getProjectDir(),
                new File(pathname));
    }

    public void removeStepMarks()
    {
        // remove step marks for all packages
        packages.values().forEach(Package::removeStepMarks);
    }

    // ---- DebuggerListener interface ----

    /**
     * A debugger event was fired. Analyse which event it was, and take
     * appropriate action.
     */
    @OnThread(Tag.VMEventHandler)
    public void processDebuggerEvent(final DebuggerEvent event, boolean skipUpdate)
    {
        
    }

    /**
     * Show the source code at a particular position
     */
    @OnThread(Tag.FXPlatform)
    public void showSource(DebuggerThread thread, String className, String sourceName, int lineNumber, DebuggerObject currentObject)
    {
        String packageName = JavaNames.getPrefix(className);
        Package pkg = getPackage(packageName);
        if (pkg != null) {
            pkg.showSourcePosition(thread, sourceName, lineNumber, currentObject);
        }
    }

    // ---- end of DebuggerListener interface ----

    /**
     * String representation for debugging.
     */
    public String toString()
    {
        return "Project:" + getProjectName();
    }

    /**
     * Removes a package (and any sub-packages) from the map of open
     * packages in the project.
     *
     * @param packageQualifiedName The qualified name of the package.
     */
    public void removePackage(String packageQualifiedName)
    {
        Package pkg = packages.get(packageQualifiedName);
        if (pkg != null) {
            pkg.getChildren(false).forEach(childPkg -> removePackage(childPkg.getQualifiedName()));
            packages.remove(packageQualifiedName);
        }
    }

    // ---- teamwork

    /**
     * Return the teamwork action group.
     */
    public Object getTeamActions()
    {
        return null;
    }

    /**
     * Determine if project is a team project. 
     * The method will look for the existence of the team configuration file
     * team.defs
     * @return true if the project is a team project
     */
    @OnThread(Tag.Any)
    public boolean isTeamProject()
    {
        return false;
    }

    /**
     * Get an array of Files that resides in the project folders.
     * @param includePkgFiles   true if package layout files should be included
     * @param includeDirs       true if directories should be included
     * @return List of File objects 
     */
    public Set<File> getFilesInProject(boolean includePkgFiles, boolean includeDirs)
    {
        Set<File> files = new LinkedHashSet<File>();
        if (includeDirs) {
            files.add(projectDir);
        }
        traverseDirsForFiles(files, projectDir, includePkgFiles, includeDirs);
        return files;
    }

    /**
     * Get the teams settings controller for this project. Returns null
     * if this is not a shared project.
     */
    public Object getTeamSettingsController()
    {
        return null;
    }

    /**
     * Set the team settings controller for this project. This makes the
     * project a shared project (unless the controller is null).
     */
    public void setTeamSettingsController(Object tsc)
    {
        
    }

    /**
     * Traverse the directory tree starting in dir an add all the encountered 
     * files to the List allFiles. The parameter includePkgFiles determine 
     * whether bluej.pkg files should be added to allFiles as well.
     * @param allFiles a List to which the method will add the files it meets.
     * @param dir the directory the search starts from
     * @param includePkgFiles if true, bluej.pkg files are included as well.
     */
    private void traverseDirsForFiles(Set<File> allFiles, File dir, boolean includePkgFiles,
                                      boolean includeDirs)
    {
        
    }

    /**
     * Get the team settings dialog for this project. Only call this if the
     * project is a shared project.
     */
    public Object getTeamSettingsDialog()
    {
        return null;
    }

    /**
     * Get the commit dialog for this project
     */
    public Object getCommitCommentsDialog()
    {
        return null;
    }

    /**
     * Get the update dialog for this project
     */
    public Object getUpdateDialog()
    {
        return null;
    }

    /**
     * Set this project as either shared or non-shared.
     */
    private void setProjectShared(boolean shared)
    {
        
    }

    /**
     * Find the package name of the package containing the given file.
     * Might return null if the file isn't in the package, or the directory the
     * file is in doesn't translate to a valid package name. However, may
     * return a valid package name which doesn't actually exist as a package
     * in the project.
     */
    public String getPackageForFile(File f)
    {
        File projdir = getProjectDir();

        // First find out the package name...
        String packageName = "";
        File parentDir = f.getParentFile();
        while (! parentDir.equals(projdir)) {
            String parentName = parentDir.getName();
            if (!JavaNames.isIdentifier(parentName)) {
                return null;
            }

            if (packageName.equals("")) {
                packageName = parentName;
            }
            else {
                packageName = parentName + "." + packageName;
            }
            parentDir = parentDir.getParentFile();
            if (parentDir == null) {
                // file not in project?
                return null;
            }
        }

        return packageName;
    }

    /**
     * Get the team status window associated with this project.
     */
    public Object getStatusWindow()
    {
        return null;
    }

    /**
     * Prepare for the deletion of a directory inside the project. This is
     * a notification which allows the team management code to save the
     * version control metadata elsewhere, if necessary.
     */
    public boolean prepareDeleteDir(File dir)
    {
        return false;
    }

    /**
     * Prepare for the creation of a directory inside the project. This is a 
     * notification which allows the team management code to perform any
     * necessary metadata actions.
     */
    public void prepareCreateDir(File dir)
    {
        
    }

    /**
     * Gets the FXTabbedEditor which should be used for adding new tabs
     */
    @OnThread(Tag.FX)
    public FXTabbedEditor getDefaultFXTabbedEditor()
    {
        return fXTabbedEditors.get(0);
    }

    public boolean isClosing()
    {
        return closing;
    }

    public void setClosing(boolean closing)
    {
        this.closing = closing;
    }

    @OnThread(Tag.Any)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type, Package pkg)
    {
        // We must use invokeLater, even if already on event queue,
        // to make sure all actions are resolved (e.g. auto-indent post-newline)
        Platform.runLater(() -> scheduleCompilation(immediate, reason, type, pkg, null));
    }

    @OnThread(Tag.Any)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type, ClassTarget target)
    {
        // We must use invokeLater, even if already on event queue,
        // to make sure all actions are resolved (e.g. auto-indent post-newline)
        Platform.runLater(() -> scheduleCompilation(immediate, reason, type, null, target));
    }

    /**
     * Schedules a compilation within the project, of the given package (if not null) and/or the given target (if not null)
     * @param immediate Whether to compile right now, or whether to compile after 1 seconds (this timer will be extended by
     *                  every later call of scheduleCompilation to 1s again)
     * @param reason Why we are compiling (used for Blackbox data collection)
     * @param type The type of compilation (used for deciding whether to keep class files, whether to open editors to show errors)
     * @param pkg The package in which to compile all targets (or null if don't want full-package compilation)
     * @param target The target to compile (or null if you don't want specific target compiled)
     *
     * Note: only one of pkg or target should be non-null.                  
     */
    @OnThread(Tag.FXPlatform)
    private void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type, Package pkg, ClassTarget target)
    {
        if (immediate)
        {
            // Take this package and target out of the list to compile later on:
            if (compilerTimer != null) {
                if (pkg != null)
                    scheduledPkgs.remove(pkg);
                if (target != null)
                    scheduledTargets.remove(target);
                // If nothing else to compile, cancel scheduled:
                if (scheduledPkgs.isEmpty() && scheduledTargets.isEmpty()) {
                    compilerTimer.stop();
                }
            }

            if (pkg != null)
                pkg.compileOnceIdle(null, reason, type);
            else if (target != null)
                target.getPackage().compileOnceIdle(target, reason, type);
        }
        else
        {
            if (pkg != null)
                scheduledPkgs.add(pkg);
            if (target != null)
                scheduledTargets.add(target);

            latestCompileReason = reason;
            latestCompileType = type;
            if (compilerTimer != null)
            {
                // Re-use existing timer, to avoid lots of reallocation:
                compilerTimer.stop();
                compilerTimer.playFromStart();
            }
            else
            {
                EventHandler<ActionEvent> listener = e -> {
                    Set<Package> pkgsToCompile;
                    Set<ClassTarget> targetsToCompile;

                    pkgsToCompile = scheduledPkgs;
                    scheduledPkgs = new HashSet<>();
                    targetsToCompile = scheduledTargets;
                    scheduledTargets = new HashSet<>();

                    for (Package p : pkgsToCompile)
                    {
                        p.compileOnceIdle(null, latestCompileReason, latestCompileType);
                    }
                    for (ClassTarget t : targetsToCompile)
                    {
                        t.getPackage().compileOnceIdle(t, latestCompileReason, latestCompileType);
                    }
                };
                compilerTimer = new Timeline(new KeyFrame(Duration.millis(1000), listener));
                compilerTimer.setCycleCount(1);
                compilerTimer.playFromStart();
            }
        }
    }

    @OnThread(Tag.Any)
    public synchronized ImportScanner getImportScanner()
    {
        // We don't construct one until asked:
        if (importScanner == null)
            importScanner = new ImportScanner(this);
        return importScanner;
    }

    @OnThread(Tag.FXPlatform)
    public FXTabbedEditor createNewFXTabbedEditor()
    {
        FXTabbedEditor ed = new FXTabbedEditor(Project.this, recallFxPosition(fXTabbedEditors.size()));
        ed.initialise();
        fXTabbedEditors.add(ed);
        fXTabbedEditors.forEach(FXTabbedEditor::updateMoveMenus);
        return ed;
    }

    @OnThread(Tag.FX)
    public List<FXTabbedEditor> getAllFXTabbedEditorWindows()
    {
        return Collections.unmodifiableList(fXTabbedEditors);
    }

    @OnThread(Tag.FX)
    public void removeFXTabbedEditor(FXTabbedEditor fxTabbedEditor)
    {
        // Update size cache.  Just update the one being removed, by putting
        // at the position where the next open editor will take from:
        // Make it long enough to contain such an element:
        while (fxCachedEditorSizes.size() < fXTabbedEditors.size())
            fxCachedEditorSizes.add(0, null);
        fxCachedEditorSizes.set(fXTabbedEditors.size() - 1, new Rectangle(fxTabbedEditor.getX(), fxTabbedEditor.getY(), fxTabbedEditor.getWidth(), fxTabbedEditor.getHeight()));

        // Only remove if we have other non-tutorial windows left, otherwise retain us
        // because we are the last window standing:
        if (fXTabbedEditors.stream().anyMatch(ed -> ed != fxTabbedEditor && !ed.hasTutorial()))
        {
            fxTabbedEditor.cleanup();
            fXTabbedEditors.remove(fxTabbedEditor);
        }
        // Update the move menus to remove the closed window as a move target:
        fXTabbedEditors.forEach(FXTabbedEditor::updateMoveMenus);
    }

    @OnThread(Tag.FXPlatform)
    public void saveEditorLocations(Properties props)
    {
        for (int i = 0; i < fXTabbedEditors.size(); i++)
        {
            saveEditorLocation(props, fXTabbedEditors.get(i), "editor.fx." + i);
        }
        // Also put out the cached sizes after the end of the editors:
        for (int i = fXTabbedEditors.size(); i < fxCachedEditorSizes.size(); i++)
        {
            saveEditorLocation(props, fxCachedEditorSizes.get(i), "editor.fx." + i);
        }
    }

    @OnThread(Tag.FXPlatform)
    private void saveEditorLocation(Properties props, FXTabbedEditor editor, String prefix)
    {
        props.put(prefix + ".x", String.valueOf(editor.getX()));
        props.put(prefix + ".y", String.valueOf(editor.getY()));
        props.put(prefix + ".width", String.valueOf(editor.getWidth()));
        props.put(prefix + ".height", String.valueOf(editor.getHeight()));
    }

    @OnThread(Tag.FXPlatform)
    private void saveEditorLocation(Properties props, Rectangle rect, String prefix)
    {
        if (rect == null)
            return;

        props.put(prefix + ".x", String.valueOf((int)rect.getX()));
        props.put(prefix + ".y", String.valueOf((int)rect.getY()));
        props.put(prefix + ".width", String.valueOf((int)rect.getWidth()));
        props.put(prefix + ".height", String.valueOf((int)rect.getHeight()));
    }

    public void setAllEditorStatus(String status)
    {
        fXTabbedEditors.forEach(fte -> fte.setTitleStatus(status));
    }

    @OnThread(Tag.FX)
    private Rectangle recallPosition(String prefix, List<Rectangle> cache, int index)
    {
        // First check if we have a cache since we've been opened:
        if (index < cache.size() && cache.get(index) != null) {
            return cache.get(index);
        }

        // Add the number on:
        Properties props = unnamedPackage.getLastSavedProperties();
        prefix = prefix + "." + index;
        int x = Integer.parseInt(props.getProperty(prefix +  ".x", "-1"));
        int y = Integer.parseInt(props.getProperty(prefix + ".y", "-1"));
        int width = Integer.parseInt(props.getProperty(prefix + ".width", "-1"));
        int height = Integer.parseInt(props.getProperty(prefix + ".height", "-1"));
        if (x >= 0 && y >= 0 && width > 100 && height > 100) {
            return new Rectangle(x, y, width, height);
        }
        else {
            return null;
        }
    }

    @OnThread(Tag.FX)
    private Rectangle recallFxPosition(int index)
    {
        return recallPosition("editor.fx", fxCachedEditorSizes, index);
    }

    @OnThread(Tag.FX)
    public Object getShelfStorage()
    {
        return null;
    }

    public BooleanProperty terminalShowing()
    {
        return terminalShowing;
    }

    public BooleanProperty debuggerShowing()
    {
        return debuggerShowing;
    }


    @Override
    @OnThread(Tag.VMEventHandler)
    public void threadStateChanged(DebuggerThread thread, boolean shouldDisplay)
    {
        Platform.runLater(() -> {
            for (int i = 0; i < threadListContents.size(); i++)
            {
                DebuggerThreadDetails details = threadListContents.get(i);
                if (details.isThread(thread))
                {
                    getDebugger().runOnEventHandler(() -> details.update());
                    break;
                }
            }

            if (hasExecControls())
            {
                ExecControls controls = getExecControls();
                getDebugger().runOnEventHandler(() -> controls.updateThreadDetails(thread));
                if (shouldDisplay)
                {
                    controls.selectThread(thread);
                }
            }
        });
    }

    @Override
    @OnThread(Tag.Any)
    public void clearThreads()
    {
        Platform.runLater(() -> {
            threadListContents.clear();
        });
    }

    @Override
    @OnThread(Tag.VMEventHandler)
    public void addThread(DebuggerThread thread)
    {
        DebuggerThreadDetails details = new DebuggerThreadDetails(thread);
        Platform.runLater(() -> {
            threadListContents.add(details);
        });
    }

    @Override
    @OnThread(Tag.VMEventHandler)
    public void removeThread(DebuggerThread thread)
    {
        DebuggerThreadDetails details = new DebuggerThreadDetails(thread);
        Platform.runLater(() -> {
            threadListContents.remove(details);
        });
    }

    /**
     * Gets the setting for this project as to which thread constructor/method invocations will run on.
     */
    public RunOnThread getRunOnThread()
    {
        return runOnThread;
    }

    /**
     * Sets the setting for this project as to which thread constructor/method invocations will run on.
     * This is communicated to the currently running debug VM, and will also be remembered if the debug
     * VM is restarted (and will be saved to the project properties on exit, for next load).
     */
    public void setRunOnThread(RunOnThread runOnThread)
    {
        this.runOnThread = runOnThread;
        debugger.setRunOnThread(runOnThread == null ? RunOnThread.DEFAULT : runOnThread);
    }

    /**
     * We fetch the display details on the debugger thread,
     * not from the FX thread, and this object allows us to capture some
     * of the thread details at one point in time and keep
     * the snapshot: namely the suspended state and the
     * display string.  This prevents us seeing "future" updates
     * which have happened in the debugger but not yet made
     * their way to the GUI.
     */
    public static class DebuggerThreadDetails
    {
        private final DebuggerThread debuggerThread;
        private String debuggerThreadDisplay;
        @OnThread(value = Tag.Any, requireSynchronized = true)
        private boolean suspended;

        @OnThread(Tag.VMEventHandler)
        public DebuggerThreadDetails(DebuggerThread dt)
        {
            this.debuggerThread = dt;
            update();
        }
        
        /**
         * Update details based on the current state of the thread.
         */
        @OnThread(Tag.VMEventHandler)
        public synchronized void update()
        {
            this.debuggerThreadDisplay = debuggerThread.toString();
            this.suspended = debuggerThread.isSuspended();
        }

        @Override
        public boolean equals(Object o)
        {
            // Equality is solely dependent on the thread, not the display:
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DebuggerThreadDetails that = (DebuggerThreadDetails) o;

            return debuggerThread.equals(that.debuggerThread);
        }

        @Override
        public int hashCode()
        {
            return debuggerThread.hashCode();
        }

        @Override
        public String toString()
        {
            return debuggerThreadDisplay;
        }

        @OnThread(Tag.Any)
        public boolean isThread(DebuggerThread dt)
        {
            return debuggerThread.equals(dt);
        }

        /**
         * Gets whether the thread was suspended when this DebuggerThreadDetails
         * object was created.  Should be used in preference to getThread().isSuspended()
         * because it is set in a controlled manner by update(), whereas getThread().isSuspended()
         * is live and may return in effect a "future" value (i.e. one which has
         * occurred in the debugger but not yet reached the GUI, and is queued
         * to be processed after this event).
         */
        @OnThread(Tag.Any)
        public synchronized boolean isSuspended()
        {
            return suspended;
        }

        @OnThread(Tag.Any)
        public DebuggerThread getThread()
        {
            return debuggerThread;
        }
    }

}
