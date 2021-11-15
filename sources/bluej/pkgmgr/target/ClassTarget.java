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
package bluej.pkgmgr.target;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.TypeVariable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import bluej.Config;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerResult;
import bluej.debugger.gentype.Reflective;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.editor.Editor;
import bluej.extensions.BClass;
import bluej.extensions.BClassTarget;
import bluej.extensions.SourceType;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.ProjectUtils;
import bluej.pkgmgr.target.role.AbstractClassRole;
import bluej.pkgmgr.target.role.ClassRole;
import bluej.pkgmgr.target.role.EnumClassRole;
import bluej.pkgmgr.target.role.InterfaceClassRole;
import bluej.pkgmgr.target.role.StdClassRole;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ResizableCanvas;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class target in a package, i.e. a target that is a class file built from
 * Java source code
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 */
@OnThread(Tag.FXPlatform)
@SuppressWarnings("unused")
public class ClassTarget extends DependentTarget
    implements InvokeListener
{
    final static int MIN_WIDTH = 60;
    final static int MIN_HEIGHT = 30;
    private final static String editStr = Config.getString("pkgmgr.classmenu.edit");
    private final static String compileStr = Config.getString("pkgmgr.classmenu.compile");
    private final static String inspectStr = Config.getString("pkgmgr.classmenu.inspect");
    private final static String removeStr = Config.getString("pkgmgr.classmenu.remove");
    private final static String convertToJavaStr = Config.getString("pkgmgr.classmenu.convertToJava");
    private final static String convertToStrideStr = Config.getString("pkgmgr.classmenu.convertToStride");
    private final static String duplicateClassStr = Config.getString("pkgmgr.classmenu.duplicate");
    private final static String createTestStr = Config.getString("pkgmgr.classmenu.createTest");

    private static final String STEREOTYPE_OPEN = "\u00AB"; //"<<";
    private static final String STEREOTYPE_CLOSE = "\u00BB"; //">>";
    private static final double RESIZE_CORNER_GAP = 4;

    // temporary file name extension to trick windows if changing case only in
    // class name
    private static String TEMP_FILE_EXTENSION = "-temp";

    // the role object represents the changing roles that are class
    // target can have ie changing from applet to an interface etc
    // 'role' should never be null
    // role should be accessed using getRole() and set using
    // setRole(). A role should not contain important state information
    // because role objects are thrown away at a whim.
    private ClassRole role = new StdClassRole();

    // a flag indicating whether an editor, when opened for the first
    // time, should display the interface of this class
    private boolean openWithInterface = false;

    // cached information obtained by parsing the source code
    // automatically becomes invalidated when the source code is
    // edited
    private Object sourceInfo = null;
    
    // caches whether the class is abstract. Only accurate when the
    // classtarget state is normal (ie. the class is compiled).
    private boolean isAbstract;
    
    // a flag indicating whether an editor should have the naviview expanded/collapsed
    private Optional<Boolean> isNaviviewExpanded = Optional.empty();

    private final List<Integer> cachedBreakpoints = new ArrayList<>();
    
    // flag to prevent recursive calls to analyseDependancies()
    private boolean analysing = false;
    
    // Whether the current compilation is invalid due to edits since compilation began
    private boolean compilationInvalid = false;

    private boolean isMoveable = true;
    private SourceType sourceAvailable;
    // Part of keeping track of number of editors opened, for Greenfoot phone home:
    private boolean hasBeenOpened = false;

    private String typeParameters = "";
    
    //properties map to store values used in the editor from the props (if necessary)
    private Map<String,String> properties = new HashMap<String,String>();
    // Keep track of whether the editor is open or not; we get a lot of
    // potential open events, and don't want to keep recording ourselves as re-opening
    private boolean recordedAsOpen = false;
    private boolean visible = true;
    public static final String MENU_STYLE_INBUILT = "class-action-inbuilt";
    private static String[] pseudos;

    // The body of the class target which goes hashed, etc:
    @OnThread(Tag.FX)
    private ResizableCanvas canvas;
    private Label stereotypeLabel;
    private boolean isFront = true;
    @OnThread(Tag.FX)
    private static Image greyStripeImage;
    @OnThread(Tag.FX)
    private static Image redStripeImage;
    private static final int GREY_STRIPE_SEPARATION = 12;
    // How far between rows of stripes:
    private static final int RED_STRIPE_SEPARATION = 16;
    private static final int STRIPE_THICKNESS = 3;
    @OnThread(Tag.FX)
    private static final Color RED_STRIPE = Color.rgb(170, 80, 60);
    @OnThread(Tag.FX)
    private static final Color GREY_STRIPE = Color.rgb(158, 139, 116);
    private boolean showingInterface;
    private boolean drawingExtends = false;
    private Label nameLabel;
    private Label noSourceLabel;

    /**
     * Create a new class target in package 'pkg'.
     * 
     * @param pkg Description of the Parameter
     * @param baseName Description of the Parameter
     */
    public ClassTarget(Package pkg, String baseName)
    {
        this(pkg, baseName, null);
    }

    /**
     * Create a new class target in package 'pkg'.
     * 
     * @param pkg Description of the Parameter
     * @param baseName Description of the Parameter
     * @param template Description of the Parameter
     */
    public ClassTarget(Package pkg, String baseName, String template)
    {
        super(pkg, baseName);

        if (pseudos == null)
        {
            pseudos = Utility.mapList(Arrays.<Class<? extends ClassRole>>asList(StdClassRole.class, UnitTestClassRole.class, AbstractClassRole.class, InterfaceClassRole.class, EnumClassRole.class), ClassTarget::pseudoFor).toArray(new String[0]);
        }

        JavaFXUtil.addStyleClass(pane, "class-target");
        JavaFXUtil.addStyleClass(pane, "class-target-id-" + baseName);

        nameLabel = new Label(baseName);
        JavaFXUtil.addStyleClass(nameLabel, "class-target-name");
        nameLabel.setMaxWidth(9999.0);
        stereotypeLabel = new Label();
        stereotypeLabel.setMaxWidth(9999.0);
        stereotypeLabel.visibleProperty().bind(stereotypeLabel.textProperty().isNotEmpty());
        stereotypeLabel.managedProperty().bind(stereotypeLabel.textProperty().isNotEmpty());
        JavaFXUtil.addStyleClass(stereotypeLabel, "class-target-extra");
        pane.setTop(new VBox(stereotypeLabel, nameLabel));
        canvas = new ResizableCanvas() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void resize(double width, double height)
            {
                super.resize(width, height);
                redraw();
            }
        };
        pane.setCenter(canvas);

        // We need to add label to the stack pane element
        // to be used later for visual indication that class lacks source
        noSourceLabel = new Label("");
        StackPane stackPane = new StackPane(pane.getCenter(), noSourceLabel);
        StackPane.setAlignment(noSourceLabel, Pos.TOP_CENTER);
        StackPane.setAlignment(canvas, Pos.CENTER);
        pane.setCenter(stackPane);

        // This must come after GUI init because it might try to affect GUI:
        calcSourceAvailable();

        // we can take a guess at what the role is going to be for the
        // object based on the start of the template name. If we get this
        // wrong, its no great shame as it'll be fixed the first time they
        // successfully analyse/compile the source.
        if (template != null) {
            if (template.startsWith("unittest")) {
                setRole(new UnitTestClassRole(true));
            }
            else if (template.startsWith("abstract")) {
                setRole(new AbstractClassRole());
            }
            else if (template.startsWith("interface")) {
                setRole(new InterfaceClassRole());
            }
            else if (template.startsWith("enum")) {
                setRole(new EnumClassRole());
            }
            else {
                setRole(new StdClassRole());
            }

        }
        JavaFXUtil.addChangeListener(canvas.sceneProperty(), scene -> {
            JavaFXUtil.runNowOrLater(() -> {
                nameLabel.applyCss();
                updateSize();
            });
        });
    }
    
    /**
     * Check whether the class has source, and of what type.
     */
    private void calcSourceAvailable()
    {
        if (getFrameSourceFile().canRead())
        {
            sourceAvailable = SourceType.Stride;
            noSourceLabel.setText("");
        }
        else if (getJavaSourceFile().canRead())
        {
            sourceAvailable = SourceType.Java;
            noSourceLabel.setText("");
        }
        else
        {
            sourceAvailable = SourceType.NONE;
            // Can't have been modified since compile since there's no source to modify:
            setState(State.COMPILED);
            noSourceLabel.setText("(" + Config.getString("classTarget.noSource") + ")");
        }
    }

    @OnThread(Tag.SwingIsFX)
    private BClass singleBClass;  // Every Target has none or one BClass
    @OnThread(Tag.SwingIsFX)
    private BClassTarget singleBClassTarget; // Every Target has none or one BClassTarget
    // Set from Swing thread but read on FX for display:
    
    /**
     * Return the extensions BProject associated with this Project.
     * There should be only one BProject object associated with each Project.
     * @return the BProject associated with this Project.
     */
    @OnThread(Tag.SwingIsFX)
    public final BClass getBClass ()
    {
        return null;
    }

    /**
     * Returns the {@link BClassTarget} associated with this {@link ClassTarget}.
     * There should be only one {@link BClassTarget} object associated with
     * each {@link ClassTarget}.
     * 
     * @return The {@link BClassTarget} associated with this {@link ClassTarget}.
     */
    @OnThread(Tag.SwingIsFX)
    public final BClassTarget getBClassTarget()
    {
        return null;
    }


    /**
     * Return the target's name, including the package name. eg.
     * bluej.pkgmgr.Target
     * 
     * @return The qualifiedName value
     */
    @OnThread(Tag.Any)
    public String getQualifiedName()
    {
        return getPackage().getQualifiedName(getBaseName());
    }

    /**
     * Return the target's base name (ie the name without the package name). eg.
     * Target
     * 
     * @return The baseName value
     */
    @OnThread(Tag.Any)
    public String getBaseName()
    {
        return getIdentifierName();
    }

    /**
     * Return information about the source of this class.
     * 
     * @return The source info object.
     */
    public Object getSourceInfo()
    {
        return null;
    }

    /**
     * Get a reflective for the type represented by this target.
     * 
     * @return  A suitable reflective, or null.
     */
    public Reflective getTypeReflective()
    {
        return null;
    }
    
    /**
     * Returns the text which the target is displaying as its label. For normal
     * classes this is just the identifier name. For generic classes the generic
     * parameters are shown as well
     * 
     * @return The displayName value
     */
    @Override
    public String getDisplayName()
    {
        return getBaseName() + getTypeParameters();
    }

    /**
     * Returns the type parameters for a generic class as declared in the source
     * file.
     * 
     * @return The typeParameters value
     */
    private String getTypeParameters()
    {
        return typeParameters;
    }

    /**
     * Change the state of this target. The target will be repainted to show the
     * new state.
     * 
     * @param newState The new state value
     */
    @Override
    public void setState(State newState)
    {
        
    }

    /**
     * Compilation of the class represented by this target has begun.
     * 
     * @param compilationSequence   compilation sequence identifier which can be used to associate
     *                              related compilation events.
     */
    public void markCompiling(int compilationSequence)
    {
        // The results of compilation will be invalid if the editor contents have not been saved:
        compilationInvalid = (editor != null) ? editor.isModified() : false; 
        
        if (getState() == State.HAS_ERROR)
        {
            setState(State.NEEDS_COMPILE);
        }

        if (getSourceType() == SourceType.Stride)
        {
            getEditor(); // Create editor if necessary
        }
        if (editor != null)
        {
            if (editor.compileStarted(compilationSequence))
            {
                setState(State.HAS_ERROR);
            }
        }
    }

    /**
     * Return the role object for this class target.
     * 
     * @return The role value
     */
    public ClassRole getRole()
    {
        return role;
    }

    /**
     * Set the role for this class target.
     * 
     * <p>Avoids changing over the role object if the new one is of the same type.
     * 
     * @param newRole The new role value
     */
    protected final void setRole(ClassRole newRole)
    {
        if (role == null || role.getRoleName() != newRole.getRoleName()) {
            role = newRole;

            String select = pseudoFor(role.getClass());
            String stereotype = role.getStereotypeLabel();
            boolean shouldBeFront = role == null || !(role instanceof UnitTestClassRole);
            isFront = shouldBeFront;
            JavaFXUtil.selectPseudoClass(pane, Arrays.asList(pseudos).indexOf(select), pseudos);
            if (stereotype != null)
                stereotypeLabel.setText(STEREOTYPE_OPEN + stereotype + STEREOTYPE_CLOSE);
            else
                stereotypeLabel.setText("");
        }
    }

    @OnThread(Tag.Any)
    private static String pseudoFor(Class<? extends ClassRole> aClass)
    {
        // AbstractClassRole becomes bj-abstract, etc
        String name = aClass.getSimpleName();
        if (name.endsWith("ClassRole"))
            name = name.substring(0, name.length() - "ClassRole".length());
        return "bj-" + name.toLowerCase();
    }

    /**
     * Test if a given class is a Junit 4 test class.
     * 
     * <p>In Junit4, test classes can be of any type.
     * The only way to test is to check if it has one of the following annotations:
     * @Before, @Test or @After
     * 
     * @param cl class to test
     */
    public static boolean isJunit4TestClass(Class<?> cl)
    {
        return false;
    }
    
    /**
     * Use a variety of tests to determine what our role is.
     * 
     * <p>All tests must be very quick and should not rely on any significant
     * computation (ie. reparsing). If computation is required, the existing
     * role will do for the time being.
     * 
     * @param cl Description of the Parameter
     */
    public void determineRole(Class<?> cl)
    {
        
    }

    /**
     * Load existing information about this class target
     * 
     * @param props the properties object to read
     * @param prefix an internal name used for this target to identify its
     *            properties in a properties file used by multiple targets.
     * @exception NumberFormatException Description of the Exception
     */
    @Override
    public void load(Properties props, String prefix)
    {
        super.load(props, prefix);

        // try to determine if any role was set when we saved
        // the class target. Be careful here as if you add role types
        // you need to add them here as well.
        String type = props.getProperty(prefix + ".type");

        String intf = props.getProperty(prefix + ".showInterface");
        openWithInterface = Boolean.valueOf(intf).booleanValue();

        if (UnitTestClassRole.UNITTEST_ROLE_NAME.equals(type)) {
            setRole(new UnitTestClassRole(false));
        }
        else if (UnitTestClassRole.UNITTEST_ROLE_NAME_JUNIT4.equals(type)) {
            setRole(new UnitTestClassRole(true));
        }
        else if (AbstractClassRole.ABSTRACT_ROLE_NAME.equals(type)) {
            setRole(new AbstractClassRole());
        }
        else if (InterfaceClassRole.INTERFACE_ROLE_NAME.equals(type)) {
            setRole(new InterfaceClassRole());
        }
        else if (EnumClassRole.ENUM_ROLE_NAME.equals(type)) {
            setRole(new EnumClassRole());
        }

        getRole().load(props, prefix);
        String value=props.getProperty(prefix + ".naviview.expanded");
        if (value!=null){
            setNaviviewExpanded(Boolean.parseBoolean(value));
            setProperty(NAVIVIEW_EXPANDED_PROPERTY, String.valueOf(value));
        }
        
        typeParameters = "";
        // parameters will be corrected when class is analysed

        cachedBreakpoints.clear();
        try
        {
            for (int i = 0; ; i++)
            {
                String s = props.getProperty(prefix + ".breakpoint." + Integer.toString(i), "");
                if (s != null && !s.isEmpty())
                {
                    cachedBreakpoints.add(Integer.parseInt(s));
                } else
                    break;
            }
        } catch (NumberFormatException e)
        {
            Debug.reportError("Error parsing breakpoint line number", e);
        }
    }

    /**
     * Save information about this class target
     * 
     * 
     * @param props the properties object to save to
     * @param prefix an internal name used for this target to identify its
     *            properties in a properties file used by multiple targets.
     */
    @Override
    public void save(Properties props, String prefix)
    {
        
    }

    /**
     * Notification that the source file may have been updated, and so we should
     * reload.
     */
    public void reload()
    {
        calcSourceAvailable();
        if (sourceAvailable != SourceType.NONE) {
            if (editor != null) {
                editor.reloadFile();
            }
            else {
                analyseSource();
            }
        }
    }
    
    /**
     * Check if the compiled class and the source are up to date.
     * (Specifically, check if recompilation is not needed. This will
     * always be considered true if the target has no source).
     * 
     * @return true if they are in sync (or there is no source); otherwise false.
     */
    public boolean upToDate()
    {
        // check if the class file is up to date
        File src = getSourceFile();
        File clss = getClassFile();

        // if just a .class file with no src, it better be up to date
        if (sourceAvailable == SourceType.NONE) {
            return true;
        }

        if (!clss.exists() || (src.exists() && (src.lastModified() > clss.lastModified()))) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the source file was modified in the future, and if so, set the modification time
     * to now.
     */
    public void fixSourceModificationDate()
    {
        // if just a .class file with no src, it better be up to date
        if (sourceAvailable == SourceType.NONE) {
            return;
        }

        File src = getSourceFile();

        // if the src file has last-modified date in the future, then set the last-modified date of
        // the source file to the current time.
        long now = Instant.now().toEpochMilli();
        // Tiny bit of leeway just in case of clock syncs, etc:
        if (src.exists() && (src.lastModified() > now + 1000))
        {
            src.setLastModified(now);
            if (editor != null)
            {
                // Important to use the File's lastModified here, rather than the now variable.
                // Some file systems (e.g. Mac) round to nearest second, so the set/get combo may
                // return a different result.  We want the one from the file system:
                editor.setLastModified(src.lastModified());
            }
        }
    }

    /**
     * Mark this class as modified, and mark all dependent classes too
     */
    public void invalidate()
    {
        
    }

    /**
     * Verify whether this class target is an interface class
     * 
     * @return true if class target is an interface class, else returns false
     */
    public boolean isInterface()
    {
        return (getRole() instanceof InterfaceClassRole);
    }

    /**
     * Verify whether this class target is an unit test class
     * 
     * @return true if class target is a unit test class, else returns false
     */
    public boolean isUnitTest()
    {
        return (getRole() instanceof UnitTestClassRole);
    }
    
    /**
     * Verify whether this class target represents an Enum
     * 
     * @return true if class target represents an Enum, else false
     */
    public boolean isEnum()
    {
        return (getRole() instanceof EnumClassRole);
    }

    /**
     * Check whether this class target represents an abstract class. This
     * can be true regardless of the role (unit test, applet, standard class).
     * 
     * The return is only valid if isCompiled() is true.
     */
    public boolean isAbstract()
    {
        return isAbstract;
    }


    // --- EditableTarget interface ---

    /**
     * Tell whether we have access to the source for this class.
     * 
     * @return Description of the Return Value
     */
    public boolean hasSourceCode()
    {
        return sourceAvailable != SourceType.NONE;
    }
    
    public SourceType getSourceType()
    {
        return sourceAvailable;
    }

    /**
     * @return the name of the Java file this target corresponds to. In the case of a Stride target this is
     *          the file generated during compilation.
     */
    public File getJavaSourceFile()
    {
        return new File(getPackage().getPath(), getBaseName() + "." + SourceType.Java.toString().toLowerCase());
    }
    
    /**
     * @return the name of the Stride file this target corresponds to. This is only valid for Stride targets.
     */
    public File getFrameSourceFile()
    {
        return new File(getPackage().getPath(), getBaseName() + "." + SourceType.Stride.toString().toLowerCase());
    }
    
    @SuppressWarnings("incomplete-switch")
    @Override
    public File getSourceFile()
    {
        switch (sourceAvailable)
        {
            case Java: return getJavaSourceFile();
            case Stride: return getFrameSourceFile();
        }
        return null;
    }

    public boolean isVisible()
    {
        return visible;
    }

    /**
     * Mark the class as having compiled, either successfully or not.
     */
    public void markCompiled(boolean successful, CompileType compileType)
    {
        if (compilationInvalid)
        {
            // We pass "classesKept" as false since the generated classes are invalid now:
            editor.compileFinished(successful, false);
            return;
        }
        
        if (successful && compileType.keepClasses())
        {
            // If the src file has last-modified date in the future, fix the date.
            // this will remove "uncompiled" stripes on the class
            fixSourceModificationDate();
            
            // Empty class files should not be marked compiled,
            // even though compilation is "successful".
            boolean newCompiledState = upToDate();
            newCompiledState &= !hasKnownError();
            if (newCompiledState)
            {
                setState(State.COMPILED);
            }
        }

        if (editor != null)
        {
            editor.compileFinished(successful, compileType.keepClasses());
            if (isCompiled())
            {
                editor.setCompiled(true);
            }
        }
        
        // Note: we assume that errors have already been marked, so there's no need to mark
        // an error state now for an unsuccessful compilation.
    }

    public static class SourceFileInfo
    {
        public final File file;
        public final SourceType sourceType;

        public SourceFileInfo(File file, SourceType sourceType)
        {
            this.file = file;
            this.sourceType = sourceType;
        }
    }

    /**
     * If this is a Java class, returns the .java source file only.
     * If this is a Stride class, returns the .stride and .java source files, *in that order*.
     * This is a strict requirement in the call in DataCollectorImpl, do not change the order.
     */
    public Collection<SourceFileInfo> getAllSourceFilesJavaLast()
    {
        List<SourceFileInfo> list = new ArrayList<>();
        if (sourceAvailable.equals(SourceType.Stride)) {
            list.add(new SourceFileInfo(getFrameSourceFile(), SourceType.Stride));
        }
        list.add(new SourceFileInfo(getJavaSourceFile(), SourceType.Java));
        return list;
    }

    /**
     * @return the name of the context(.ctxt) file this target corresponds to.
     */
    public File getContextFile()
    {
        return new File(getPackage().getPath(), getBaseName() + ".ctxt");
    }

    /**
     * @return the name of the class (.class) file this target corresponds to.
     */
    public File getClassFile()
    {
        return new File(getPackage().getPath(), getBaseName() + ".class");
    }

    /**
     * Get the name of the documentation (.html) file corresponding to this target.
     */
    public File getDocumentationFile()
    {
        // We ask for Java source file, regardless of source type,
        // because we're only using it to derive the .html file with the same stub:
        String filename = getJavaSourceFile().getPath();
        String docFilename = getPackage().getProject().getDocumentationFile(filename);
        return new File(docFilename);
    }
    
    /**
     * Get a list of .class files for inner classes.
     */
    public File [] getInnerClassFiles()
    {
        File[] files = getPackage().getPath().listFiles(new InnerClassFileFilter());
        return files;
    }

    /**
     * A filter to find inner class files.
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    class InnerClassFileFilter
        implements FileFilter
    {
        /**
         * Description of the Method
         * 
         * @param pathname Description of the Parameter
         * @return Description of the Return Value
         */
        @Override
        public boolean accept(File pathname)
        {
            return pathname.getName().startsWith(getBaseName() + "$");
        }
    }

    /**
     * Get the editor associated with this class.
     * @return the editor object associated with this target. May be null if
     *         there was a problem opening this editor.
     */
    @Override
    public Editor getEditor()
    {
        boolean withInterface;
        withInterface = this.openWithInterface;
        return getEditor(withInterface);
    }

    /**
     * Gets the editor, if it is already open.  If not open, returns
     * null (without attempting to open it, in contrast to getEditor())
     */
    public Editor getEditorIfOpen()
    {
        return editor;
    }

    /**
     * Get an editor for this class, either in source view or interface view.
     * 
     * @param showInterface Determine whether to show interface view or 
     *         source view in the editor.
     * @return the editor object associated with this target. May be null if
     *         there was a problem opening this editor.
     */
    private Editor getEditor(boolean showInterface)
    {
        return null;
    }

    /**
     * Records that the editor for this class target has been opened
     * (i.e. actually made visible on screen).  Further calls after
     * the first call will be ignored.
     */
    private void recordEditorOpen()
    {
        if (!hasBeenOpened)
        {
            hasBeenOpened = true;
            switch (sourceAvailable)
            {
                case Java:
                    Config.recordEditorOpen(Config.SourceType.Java);
                    break;
                case Stride:
                    Config.recordEditorOpen(Config.SourceType.Stride);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Ensure that the source file of this class is up-to-date (i.e.
     * that any possible unsaved changes in an open editor window are
     * saved).
     *
     * <p>This can cause saveEvent() to be generated, which might move
     * the class to a new package (if the package line has been changed).
     */
    @Override
    public void ensureSaved() throws IOException
    {
        // When creating a Stride class, we need to load the editor
        // in order to save and generate the Java code (or at least,
        // that's an easy way to do it).  Not necessary for Java classes:
        if(editor == null && sourceAvailable == SourceType.Stride) {
            getEditor();
        }
        super.ensureSaved();
    }

    // --- end of EditableTarget interface ---

    // --- user interface function implementation ---

    /**
     * Open an inspector window for the class represented by this target.
     * 
     * @param parent Parent window.
     * @param animateFromCentre Animate from centre of this node.
     */
    private void inspect(Window parent, Node animateFromCentre)
    {
        
    }

    // --- EditorWatcher interface ---

    @Override
    public void modificationEvent(Editor editor)
    {
        
    }

    @Override
    public void saveEvent(Editor editor)
    {
        ClassInfo info = analyseSource();
        if (info != null) {
            updateTargetFile(info);
        }
        determineRole(null);
    }

    @Override
    public String breakpointToggleEvent(int lineNo, boolean set)
    {
        return null;
    }
    
    @Override
    public void clearAllBreakpoints()
    {
        Package pkg = getPackage();
        if (pkg != null) // Can happen during removal
            pkg.getDebugger().removeBreakpointsForClass(getQualifiedName());
    }

    // --- end of EditorWatcher interface ---

    /**
     * Remove all breakpoints in this class.
     */
    public void removeBreakpoints()
    {
        if (editor != null) {
            editor.removeBreakpoints();
        }
    }
    
    /**
     * Re-initialize the breakpoints which have been set in this
     * class.
     */
    public void reInitBreakpoints()
    {
        if (editor != null && isCompiled()) {
            editor.reInitBreakpoints();
        }
        else if (isCompiled() && sourceAvailable == SourceType.Stride)
        {
            // In Stride, breakpoints persist with the saved source,
            // and we should set them even if the editor is not open.
            // So we cache them as properties and use that here if
            // the editor has not been opened yet:
            List<Integer> breakpoints;
            breakpoints = new ArrayList<>(this.cachedBreakpoints);
            for (Integer line : breakpoints)
            {
                breakpointToggleEvent(line, true);
            }
        }
    }
    
    /**
     * Remove the step mark in this case
     * (the mark in the editor that shows where execution is)
     */
    public void removeStepMark()
    {
        if (editor != null) {
            editor.removeStepMark();
        }
    }

    /**
     * Gets the compiled attribute of the ClassTarget object
     * 
     * @return The compiled value
     */
    public boolean isCompiled()
    {
        return getState() == State.COMPILED;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type)
    {
        if (Config.isGreenfoot() && type == CompileType.EXPLICIT_USER_COMPILE)
        {
            // We compile the package rather than just the class for explicit compiles in
            // Greenfoot, but mark this target as modified first so that we do also compile
            // this class even if we wouldn't otherwise (and can report the result to the
            // editor, which is expecting to receive it):
            markModified();
            getPackage().getProject().scheduleCompilation(immediate, reason, type, getPackage());
        }
        else
        {
            getPackage().getProject().scheduleCompilation(immediate, reason, type, this);
        }
    }

    /**
     * Called when this class target has just been successfully compiled.
     * 
     * We load the compiled class if possible and check if the compilation has
     * resulted in it taking a different role (ie abstract to applet)
     */
    public void analyseAfterCompile()
    {
        Class<?> cl = getPackage().loadClass(getQualifiedName());

        determineRole(cl);
        analyseDependencies(cl);
        analyseTypeParams(cl);
    }

    /**
     * generates a source code skeleton for this class
     */
    public boolean generateSkeleton(String template, SourceType sourceType)
    {
        return false;
    }

    /**
     * Inserts a package deceleration in the source file of this class, only if it
     * is not correct or if it does't exist. Also, the default package will be ignored.
     * 
     * @param packageName the package's name
     * @exception IllegalArgumentException if the package name is not a valid java identifier
     */
    public void enforcePackage(String packageName)
        throws IOException
    {
        
    }

    /**
     * Analyse the source code, and save retrieved information.
     * This includes comments and parameter names for methods/constructors,
     * class name, type parameters, etc.
     * <p>
     * Also causes the class role (normal class, unit test, etc) to be
     * guessed based on the source.
     * <p>
     * Note: this should only be called once the containing package is loaded, not
     * before. All classes must be present in the package or dependency information
     * will be generated incorrectly during parsing.
     */
    public ClassInfo analyseSource()
    {
        return null;
    }
    
    /**
     * Change file name and package to match that found in the source file.
     * @param info  The information from source analysis
     */
    private void updateTargetFile(ClassInfo info)
    {
        if (analyseClassName(info)) {
            if (nameEqualsIgnoreCase(info.getName())) {
                // this means file has same name but different case
                // to trick Windows OS to do a name change we need to
                // rename to temp name and then rename to desired name
                doClassNameChange(info.getName() + TEMP_FILE_EXTENSION);
            }
            doClassNameChange(info.getName());
        }
        if (analysePackageName(info)) {
            doPackageNameChange(info.getPackage());
        }
    }

    /**
     * Sets the typeParameters attribute of the ClassTarget object
     * 
     * @param info The new typeParameters value
     */
    private void setTypeParameters(ClassInfo info)
    {
        String newTypeParameters = "";
        if (info.hasTypeParameter()) {
            Iterator<String> i = info.getTypeParameterTexts().iterator();
            newTypeParameters = "<" + i.next();
           
            while (i.hasNext()) {
                newTypeParameters += "," + i.next();
            }
            newTypeParameters += ">";
        }
        if (!newTypeParameters.equals(typeParameters))
        {
            typeParameters = newTypeParameters;
            updateDisplayName();
        }
    }

    /**
     * Analyses class name of Classtarget with that of parsed src file. Aim is
     * to detect any textual changes of class name and modify resources to suit
     * 
     * 
     * @param info contains parsed class information
     * @return true if class name is different
     */
    public boolean analyseClassName(ClassInfo info)
    {
        String newName = info.getName();

        if ((newName == null) || (newName.length() == 0)) {
            return false;
        }

        return (!getBaseName().equals(newName));
    }

    /**
     * Check whether the package name has been changed by comparing the package
     * name in the information from the parser with the current package name
     */
    private boolean analysePackageName(ClassInfo info)
    {
        String newName = info.getPackage();

        return (!getPackage().getQualifiedName().equals(newName));
    }

    /**
     * Analyse the current dependencies in the source code and update the
     * dependencies in the graphical display accordingly.
     */
    private void analyseDependencies(ClassInfo info)
    {
        
    }

    /**
     * Analyse the current dependencies in the compiled class and update the
     * dependencies in the graphical display accordingly.
     */
    public void analyseDependencies(Class<?> cl)
    {
        if (cl != null) {
            removeInheritDependencies();

            Class<?> superClass = cl.getSuperclass();
            if (superClass != null) {
                setSuperClass(superClass.getName());
            }

            Class<?> [] interfaces = cl.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                addInterface(interfaces[i].getName());
            }
        }
    }
    
    /**
     * Analyse the type parameters from the compiled class and update the display name.
     */
    public <T> void analyseTypeParams(Class<T> cl)
    {
        if (cl != null) {
            String oldTypeParams = typeParameters;
            TypeVariable<Class<T>> [] tvars = cl.getTypeParameters();
            if (tvars.length == 0) {
                typeParameters = "";
            }
            else
            {
                boolean isFirst = true;
                typeParameters = "<";
                for (TypeVariable<?> tvar : tvars) {
                    if (! isFirst) {
                        typeParameters += ",";
                    }
                    isFirst = false;
                    typeParameters += tvar.getName();
                }
                typeParameters += ">";
            }
            
            if (! typeParameters.equals(oldTypeParams)) {
                updateDisplayName();
            }
        }
    }
    
    /**
     * Set the superclass. This adds an extends dependency to the appropriate class.
     * The old extends dependency (if any) must be removed separately.
     * 
     * @param superName  the fully-qualified name of the superclass
     */
    private void setSuperClass(String superName)
    {
        
    }
    
    /**
     * Add an interface. This adds an implements dependency to the appropriate interface.
     */
    private void addInterface(String interfaceName)
    {
        
    }
    
    /**
     * Notification that the class represented by this class target has changed name.
     */
    private boolean doClassNameChange(String newName)
    {
        return false;
    }

    /**
     * Update the displayed class name (which includes type parameters).
     */
    public void updateDisplayName()
    {
        String newDisplayName = getDisplayName();
        updateSize();
        nameLabel.setText(newDisplayName);
        setDisplayName(newDisplayName);
    }
    
    /**
     * Delete all the source files (edited and generated) for this target.
     */
    private void deleteSourceFiles()
    {
        if (getSourceType().equals(SourceType.Stride)) {
            getJavaSourceFile().delete();
        }
        getSourceFile().delete();
    }

    /**
     * Checks for ClassTarget name equality if case is ignored.
     * 
     * 
     * @param newName
     * @return true if name is equal ignoring case.
     */
    private boolean nameEqualsIgnoreCase(String newName)
    {
        return (getBaseName().equalsIgnoreCase(newName));
    }

    /**
     * Change the package of a class target to something else.
     * 
     * @param newName the new fully qualified package name
     */
    private void doPackageNameChange(String newName)
    {
        Project proj = getPackage().getProject();

        Package dstPkg = proj.getPackage(newName);

        boolean packageInvalid = dstPkg == null;
        boolean packageNameClash = dstPkg != null && dstPkg.getTarget(getBaseName()) != null;

        if (packageInvalid)
        {
            DialogManager.showErrorFX(null, "package-name-invalid");
        }
        else
        {
            // fix for bug #382. Potentially could clash with a package
            // in the destination package with the same name
            if (packageNameClash)
            {
                DialogManager.showErrorFX(null, "package-name-clash");
                // fall through to enforcePackage, below.
            }
            else if (DialogManager.askQuestionFX(null, "package-name-changed") == 0)
            {
                dstPkg.importFile(getSourceFile());
                prepareForRemoval();
                getPackage().removeTarget(this);
                close();
                return;
            }
        }

        // all non working paths lead here.. lets fix the package line
        // up so it is back to what we expect
        try
        {
            enforcePackage(getPackage().getQualifiedName());
            getEditor().reloadFile();
        }
        catch (IOException ioe)
        {
        }
    }

    /**
     * Resizes the class so the entire classname + type parameter are visible
     *  
     */
    private void updateSize()
    {
        String displayName = getDisplayName();
        int width = calculateWidth(nameLabel, displayName);
        // Don't make size smaller if user has already resized
        // to larger than is needed for text width:
        setSize(Math.max(width, (int)pane.getPrefWidth()), getHeight());
        repaint();
    }

    /**
     * Post the context menu for this target.
     * 
     * @param x  the x coordinate for the menu, relative to graph editor
     * @param y  the y coordinate for the menu, relative to graph editor
     */
    public void popupMenu(int x, int y, Object graphEditor)
    {
        
    }

    /**
     * Creates a popup menu for this class target.
     * 
     * @param extMgr
     * @param cl class object associated with this class target
     * @return the created popup menu object
     */
    protected void withMenu(Class<?> cl, ClassRole roleRef, SourceType source, boolean docExists, FXPlatformConsumer<ContextMenu> withMenu, Object extMgr)
    {
        
    }

    private void putFXLaunchResult(Object ed, Window fxWindow, CompletableFuture<FXPlatformSupplier<DebuggerResult>> result)
    {
        result.thenAccept(new Consumer<FXPlatformSupplier<DebuggerResult>>()
        {
            @Override
            @OnThread(Tag.Worker)
            public void accept(FXPlatformSupplier<DebuggerResult> supplier)
            {
                
            }
        });
    }

    /**
     * Action which creates a test
     */
    @OnThread(Tag.FXPlatform)
    public class CreateTestAction extends MenuItem
    {
        /**
         * Constructor for the CreateTestAction object
         */
        public CreateTestAction()
        {
            super(createTestStr);
            setOnAction(e -> actionPerformed(e));
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
        
        @OnThread(Tag.FXPlatform)
        private void actionPerformed(ActionEvent e)
        {
            
        }
    }

    /**
     * Action to open the editor for a classtarget
     */
    @OnThread(Tag.FXPlatform)
    private class EditAction extends MenuItem
    {
        public EditAction(boolean enable)
        {
            super(editStr);
            setOnAction(e -> open());
            setDisable(!enable);
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
    }

    /**
     * Action to compile a classtarget
     */
    @OnThread(Tag.FXPlatform)
    private class CompileAction extends MenuItem
    {
        public CompileAction(boolean enable)
        {
            super(compileStr);
            setOnAction(e -> {
                getPackage().compile(ClassTarget.this, CompileReason.USER, CompileType.EXPLICIT_USER_COMPILE);
            });
            setDisable(!enable);
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
    }

    /**
     * Action to remove a classtarget from its package
     */
    @OnThread(Tag.FXPlatform)
    private class RemoveAction extends MenuItem
    {
        public RemoveAction()
        {
            super(removeStr);
            setOnAction(e -> actionPerformed(e));
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }

        @OnThread(Tag.FXPlatform)
        private void actionPerformed(ActionEvent e)
        {
            
        }
    }

    /**
     * Action to inspect the static members of a class
     */
    @OnThread(Tag.FXPlatform)
    public class InspectAction extends MenuItem
    {
        private final Node animateFromCentreOverride;

        /**
         * Create an action to inspect a class (i.e. static members, not inspecting an instance)
         * 
         * @param enable Should the action be enabled?
         * @param parentOverride If non-null, use this as parent.  If null, use PkgMgrFrame window.
         * @param animateFromCentreOverride If non-null, animate from centre of this node.  If null, use ClassTarget's GUI node
         */
        public InspectAction(boolean enable, Node animateFromCentreOverride)
        {
            super(inspectStr);
            this.animateFromCentreOverride = animateFromCentreOverride;
            setOnAction(e -> actionPerformed(e));
            setDisable(!enable);
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }

        @OnThread(Tag.FXPlatform)
        private void actionPerformed(ActionEvent e)
        {
            if (checkDebuggerState())
            {
                Window parent = getPackage().getUI().getStage();
                Node animateFromCentre = animateFromCentreOverride != null ? animateFromCentreOverride : getNode();

                inspect(parent, animateFromCentre);
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    public class ConvertToJavaAction extends MenuItem
    {
        private final Window parentWindow;
        
        public ConvertToJavaAction(Window parentWindow)
        {
            super(convertToJavaStr);
            this.parentWindow = parentWindow;
            setOnAction(this::actionPerformed);
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }

        private void actionPerformed(ActionEvent e)
        {
            if (JavaFXUtil.confirmDialog("convert.to.java.title", "convert.to.java.message", parentWindow, true))
            {
                convertStrideToJava();
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    public class ConvertToStrideAction extends MenuItem
    {
        public ConvertToStrideAction(Window parentWindow)
        {
            super(convertToStrideStr);
            setOnAction(e -> promptAndConvertJavaToStride(parentWindow));
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
    }

    /**
     * A menu item to invoke a class duplication. This is valid for
     * Java and Stride classes.
     */
    @OnThread(Tag.FXPlatform)
    private class DuplicateClassAction extends MenuItem
    {
        public DuplicateClassAction()
        {
            super(duplicateClassStr);
            setOnAction(event -> duplicate());
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
    }

    /**
     * Converts this Java ClassTarget to Stride, as long as the user
     * says yes to the dialog that this method shows.
     *
     * If warnings (e.g. package-private access converted to protected)
     * are encountered during the conversion, a dialog is shown to the user
     * explaining them.  Most conversion issues (e.g. unconvertable items)
     * are warnings not errors.  Errors, which stop the process, mainly arise
     * from unparseable Java source code.
     */
    public void promptAndConvertJavaToStride(Window window)
    {
        
    }

    /**
     * Process a double click on this target. That is: open its editor.
     *
     * @param  openInNewWindow if this is true, the editor opens in a new window
     */
    @Override
    public void doubleClick(boolean openInNewWindow)
    {
        Editor editor = getEditor();
        if(editor == null)
        {
            getPackage().showError("error-open-source");
        }
        editor.setEditorVisible(true, openInNewWindow);
    }
    /**
     * Set the size of this target.
     * 
     * @param width The new size value
     * @param height The new size value
     */
    @Override
    public void setSize(int width, int height)
    {
        int w = Math.max(width, MIN_WIDTH);
        int h = Math.max(height, MIN_HEIGHT);
        super.setSize(w, h);
        if(assoc != null)
            assoc.setSize(w, h);
    }
    
    public void setVisible(boolean vis)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    @Override
    protected void redraw()
    {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        g.clearRect(0, 0, width, height);

        // Draw either grey or red stripes:
        if (getState() != State.COMPILED)
        {
            // We could draw the stripes manually each time, but that
            // could get quite time-consuming when we have lots of classes.
            // Instead, we create an image of the given stripes which
            // we can draw tiled to save time.
            g.setFill(hasKnownError() ? getRedStripeFill() : getGreyStripeFill());
            g.fillRect(0, 0, width, height);
        }

        if (this.selected && isResizable())
        {
            g.setStroke(javafx.scene.paint.Color.BLACK);
            g.setLineDashes();
            g.setLineWidth(1.0);
            // Draw the marks in the corner to indicate resizing is possible:
            g.strokeLine(width - RESIZE_CORNER_SIZE, height, width, height - RESIZE_CORNER_SIZE);
            g.strokeLine(width - RESIZE_CORNER_SIZE + RESIZE_CORNER_GAP, height, width, height - RESIZE_CORNER_SIZE + RESIZE_CORNER_GAP);
        }
    }

    /**
     * Gets the grey diagonal stripe pattern used for modified classes.
     */
    @OnThread(Tag.FX)
    public static ImagePattern getGreyStripeFill()
    {
        int size = GREY_STRIPE_SEPARATION * 10;
        // Grey stripes
        if (greyStripeImage == null)
        {
            greyStripeImage = JavaFXUtil.createImage(size, size, gImage -> {
                JavaFXUtil.stripeRect(gImage, 0, 0, size, size, GREY_STRIPE_SEPARATION - STRIPE_THICKNESS, STRIPE_THICKNESS, false, GREY_STRIPE);
            });
        }
        return new ImagePattern(greyStripeImage, 0, 0, size, size, false);
    }

    /**
     * Gets the red diagonal stripe pattern used for classes with an error.
     */
    @OnThread(Tag.FX)
    public static ImagePattern getRedStripeFill()
    {
        // Red stripes
        int size = RED_STRIPE_SEPARATION * 10;
        if (redStripeImage == null)
        {
            redStripeImage = JavaFXUtil.createImage((int)size, (int)size, gImage -> {
                JavaFXUtil.stripeRect(gImage, 0, 0, size, size, RED_STRIPE_SEPARATION - STRIPE_THICKNESS, STRIPE_THICKNESS, false, RED_STRIPE);
                JavaFXUtil.stripeRect(gImage, 0, 0, size, size, RED_STRIPE_SEPARATION - STRIPE_THICKNESS, STRIPE_THICKNESS, true, RED_STRIPE);
            });
        }
        return new ImagePattern(redStripeImage, 0, 0, size, size, false);
    }

    /**
     * Prepares this ClassTarget for removal from a Package. It removes
     * dependency arrows and calls prepareFilesForRemoval() to remove applicable
     * files.
     */
    private void prepareForRemoval()
    {
        if (editor != null) {
            editor.close();
        }

        // if this target is the assocation for another Target, remove
        // the association
        for (Target o : getPackage().getVertices())
        {
            if (o instanceof DependentTarget) {
                DependentTarget d = (DependentTarget) o;
                if (this.equals(d.getAssociation())) {
                    d.setAssociation(null);
                }
            }
        }

        // flag dependent Targets as invalid
        invalidate();
        removeAllInDependencies();
        removeAllOutDependencies();
        // remove associated files (.frame, .class, .java and .ctxt)
        prepareFilesForRemoval();
    }

    /**
     * Removes applicable files (.class, .java and .ctxt) prior to this
     * ClassTarget being removed from a Package.
     */
    public void prepareFilesForRemoval()
    {
        List<File> allFiles = getRole().getAllFiles(this);
        for(Iterator<File> i = allFiles.iterator(); i.hasNext(); ) {
            i.next().delete();
        }
    }

    @Override
    public void generateDoc()
    {
        getPackage().generateDocumentation(this);
    }

    @Override
    public void remove()
    {
        
    }

    /**
     * Converts this ClassTarget from Stride to Java, by simply
     * deleting the Stride file and keeping the Java file which we've
     * always been generating from Stride for compilation purposes.
     *
     * This method shows no confirmation dialog/prompt; the caller is expected
     * to have taken care of that.
     *
     * Throws an exception if this is not a Stride ClassTarget.
     */
    public void convertStrideToJava()
    {
        
    }

    /**
     * Duplicates the class which is represented by this class target
     */
    private void duplicate()
    {
        
    }

    /**
     * Adds the given stride code element as a Stride file for this
     * class target, as part of a conversion from Java into Stride, or part
     * of generating a new class from a template.
     *
     * @param element The source code content to put in the new .stride file.
     */
    private void addStride(Object element)
    {
        
    }

    public boolean isMoveable()
    {
        return isMoveable;
    }

    /**
     * Set whether this ClassTarget can be moved by the user (dragged around).
     * This is set false for unit tests which are associated with another class.
     * 
     * @see bluej.graph.Moveable#setIsMoveable(boolean)
     */
    public void setIsMoveable(boolean isMoveable)
    {
        this.isMoveable = isMoveable;
    }
    
    /**
     * perform interactive method call
     */
    @Override
    public void executeMethod(MethodView mv)
    {
        getPackage().callStaticMethodOrConstructor(mv);
    }
    
    /**
     * interactive constructor call
     */
    @Override
    public void callConstructor(ConstructorView cv)
    {
        getPackage().callStaticMethodOrConstructor(cv);
    }
    
    /**
     * Method to check state of debug VM (currently running may cause problems)
     * and then give options accordingly. 
     * Returns a value from user about how to continue i.e should the original requested be executed.
     * 
     * @return Whether the original request should be executed (dependent on how the user wants to proceed)
     */
    private boolean checkDebuggerState()
    {
        return ProjectUtils.checkDebuggerState(getPackage().getProject(), getPackage().getUI().getStage());
    }

    /**
     * Returns the naviview expanded value from the properties file
     * @return 
     */
    public boolean isNaviviewExpanded()
    {
        return isNaviviewExpanded.orElse(false);
    }

    /**
     * Sets the naviview expanded value from the properties file to this local variable
     * @param isNaviviewExpanded
     */
    public void setNaviviewExpanded(boolean isNaviviewExpanded)
    {
        this.isNaviviewExpanded = Optional.of(isNaviviewExpanded);
    }

    /**
     * Retrieves a property from the editor
     */
    @Override
    public String getProperty(String key)
    {
        return properties.get(key);
    }

    /**
     * Sets a property for the editor
     */
    @Override
    public void setProperty(String key, String value)
    {
        properties.put(key, value);
    }
    
    @Override
    public void recordJavaEdit(String latest, boolean includeOneLineEdits)
    {
        
    }

    @Override
    public void recordStrideEdit(String latestJava, String latestStride, Object reason)
    {
        
    }

    @Override
    public void recordClose()
    {
        
    }

    @Override
    public void recordOpen()
    {
        
    }

    @Override
    public void recordSelected()
    {
        
    }

    public CompileInputFile getCompileInputFile()
    {
        return new CompileInputFile(getJavaSourceFile(), getSourceFile());
    }

    /**
     * Display a compilation diagnostic (error message), if possible and appropriate. The editor
     * decides if it is appropriate to display the error and may have a policy where eg it only
     * shows a limited number of errors.
     * 
     * @param diagnostic   the compiler-generated diagnostic
     * @param errorIndex   the index of the error in this batch (first error is 0)
     * @param compileType  the type of compilation leading to the error
     * @return    true if the diagnostic was displayed to the user
     */
    public boolean showDiagnostic(Diagnostic diagnostic, int errorIndex, CompileType compileType)
    {
        // If an edit has been made since the compilation started, we don't want to display the
        // error since it may no longer be present, and if it is it will be shown by a later
        // compilation anyway:
        if (compilationInvalid)
        {
            return false;
        }
        
        Editor ed = getEditor();
        if (ed == null)
        {
            return false;
        }
        
        setState(State.HAS_ERROR);
        return ed.displayDiagnostic(diagnostic, errorIndex, compileType);
    }
    
    /**
     * Check whether there was a compilation error for this target, last time
     * compilation was attempted.
     */
    @OnThread(Tag.Any)
    public boolean hasKnownError()
    {
        return getState() == State.HAS_ERROR;
    }

    @Override
    public void recordShowErrorMessage(int identifier, List<String> quickFixes)
    {
        
    }

    @Override
    public void recordShowErrorIndicators(Collection<Integer> identifiers)
    {
        
    }

    @Override
    public void recordEarlyErrors(List<?> diagnostics, int compilationIdentifier)
    {
        
    }

    @Override
    public void recordLateErrors(List<?> diagnostics, int compilationIdentifier)
    {
        
    }

    @Override
    public void recordFix(int errorIdentifier, int fixIndex)
    {
        
    }

    // See comment for DataCollector.codeCompletionStart
    @Override
    public void recordCodeCompletionStarted(Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem, int codeCompletionId)
    {
        
    }

    // See comment for DataCollector.codeCompletionEnded
    @Override
    public void recordCodeCompletionEnded(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, String replacement, int codeCompletionId)
    {
        
    }

    @Override
    public void recordUnknownCommandKey(String enclosingFrameXpath, int cursorIndex, char key)
    {
        
    }

    @Override
    public void recordShowHideFrameCatalogue(String enclosingFrameXpath, int cursorIndex, boolean show, Object reason)
    {
        
    }

    @Override
    public void recordViewModeChange(String enclosingFrameXpath, int cursorIndex, Object oldView, Object newView, Object reason)
    {
        
    }

    @Override
    public boolean isFront()
    {
        return isFront;
    }

    @Override
    public void showingInterface(boolean showing)
    {
        this.showingInterface = showing;
    }

    @Override
    public void setCreatingExtends(boolean drawingExtends)
    {
        // Don't call super; we don't want to darken ourselves
        this.drawingExtends = drawingExtends;
    }

    @Override
    public boolean cursorAtResizeCorner(MouseEvent e)
    {
        // Don't allow resize if we are picking an extends arrow:
        return super.cursorAtResizeCorner(e) && !drawingExtends;
    }

    @Override
    public int compareTo(Target o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
