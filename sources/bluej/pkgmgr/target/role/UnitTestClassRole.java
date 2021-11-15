/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2014,2016,2017  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target.role;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.utility.javafx.JavaFXUtil;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.MenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Method;
import java.util.List;

import static bluej.pkgmgr.target.ClassTarget.MENU_STYLE_INBUILT;

/**
 * A role object for Junit unit tests.
 *
 * @author  Andrew Patterson
 */
public class UnitTestClassRole extends ClassRole
{
    public static final String UNITTEST_ROLE_NAME = "UnitTestTarget";
    public static final String UNITTEST_ROLE_NAME_JUNIT4 = "UnitTestTargetJunit4";
    
    /** Whether this is a Junit 4 test class. If false, it's a Junit 3 test class. */
    private final boolean isJunit4;
    
    /**
     * Create the unit test class role.
     */
    public UnitTestClassRole(boolean isJunit4)
    {
        this.isJunit4 = isJunit4;
    }

    @Override
    @OnThread(Tag.Any)
    public String getRoleName()
    {
        if (isJunit4) {
            return UNITTEST_ROLE_NAME_JUNIT4;
        }
        else {
            return UNITTEST_ROLE_NAME;
        }
    }

    @Override
    @OnThread(Tag.Any)
    public String getStereotypeLabel()
    {
        return "unit test";
    }


    @OnThread(Tag.Any)
    private boolean isJUnitTestMethod(Method m)
    {
        return false;
    }
    
    /**
     * Generate a popup menu for this TestClassRole.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public boolean createRoleMenu(ObservableList<MenuItem> menu, ClassTarget ct, Class<?> cl, State state)
    {
        return false;
    }

    @OnThread(Tag.FXPlatform)
    private static void addMenuItem(ObservableList<MenuItem> menu, TargetAbstractAction testAction, boolean enableTestAll)
    {
        menu.add(testAction);
        testAction.setDisable(!enableTestAll);
        JavaFXUtil.addStyleClass(testAction, MENU_STYLE_INBUILT);
    }

    /**
     * creates a class menu containing any constructors and static methods etc.
     *
     * @param menu the popup menu to add the class menu items to
     * @param cl Class object associated with this class target
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public boolean createClassConstructorMenu(ObservableList<MenuItem> menu, ClassTarget ct, Class<?> cl)
    {
        return true;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public boolean createClassStaticMenu(ObservableList<MenuItem> menu, ClassTarget ct,  Class<?> cl)
    {
        return true;
    }

    @Override
    @OnThread(Tag.Any)
    public boolean canConvertToStride()
    {
        return false; // annotations needed for JUnit are not supported
    }

    @Override
    public void run(final Object pmf, final ClassTarget ct, final String param)
    {
        
    }
    
    /**
     * Set up a test run. This just involves going through the methods in the class
     * and creating a list of those which are test methods.
     * 
     * @param pmf   The package manager frame
     * @param ct    The class target
     * @param trt   The test runner thread
     * @return The list of test methods in the class, or null if we could not find out
     */
    public List<String> startRunTest(Object pmf, ClassTarget ct, Object trt)
    {
        return null;
    }
    
    /**
     * Get the count of tests in the test class.
     * @param ct  The ClassTarget of the unit test class
     * @return    the number of tests in the unit test class
     */
    public int getTestCount(ClassTarget ct)
    {
        if (! ct.isCompiled()) {
            return 0;
        }
        
        Class<?> cl = ct.getPackage().loadClass(ct.getQualifiedName());
        if (cl == null) {
            return 0;
        }
        
        Method[] allMethods = cl.getMethods();

        int testCount = 0;

        for (int i=0; i < allMethods.length; i++) {
            if (isJUnitTestMethod(allMethods[i])) {
                testCount++;
            }
        }
        
        return testCount;
    }
    
    /**
     * Start the construction of a test method.
     * 
     * This method prompts the user for a test method name and then sets up
     * all the variables for constructing a new test method.
     * 
     * @param pmf  the PkgMgrFrame this is all occurring in
     * @param ct   the ClassTarget of the unit test class
     */
    @OnThread(Tag.FXPlatform)
    public void doMakeTestCase(final Object pmf, final ClassTarget ct)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    private void finishTestCase(Object pmf, ClassTarget ct, String newTestName)
    {
        
    }
    
    /**
     * End the construction of a test method.
     * <p>
     * This method is responsible for actually created the source code for a
     * just-recorded test method.
     * 
     * @param pmf   the PkgMgrFrame this is all occurring in
     * @param ct    the ClassTarget of the unit test class
     * @param name  the name of the test method we are writing out
     */
    public void doEndMakeTestCase(Object pmf, ClassTarget ct, String name)
    {
        
    }
    
    /**
     * Turn the fixture declared in a unit test class into a set of
     * objects on the object bench.
     * 
     * @param pmf  the PkgMgrFrame that will hold the object bench
     * @param ct   the ClassTarget of the unit test class
     */
    public void doFixtureToBench(Object pmf, ClassTarget ct)
    {
        
    }   
    
    /**
     * Convert the objects on the object bench into a test fixture.
     */
    public void doBenchToFixture(Object pmf, ClassTarget ct)
    {
        
    }
    
    /**
     * A base class for all our actions that run on targets.
     */
    @OnThread(Tag.FXPlatform)
    private abstract class TargetAbstractAction extends MenuItem
    {
        public TargetAbstractAction(String name, Object ped, ClassTarget t)
        {
            super(name);
            setOnAction(e -> actionPerformed(e));
        }

        @OnThread(Tag.FXPlatform)
        public abstract void actionPerformed(javafx.event.ActionEvent actionEvent);
    }

    /**
     * A TestAction is an action that causes a JUnit test to be run on a class.
     * If testName is not provided, it is set to null which means that the whole
     * test class is run; otherwise it refers to a test method that should be run
     * individually.
     */
    @OnThread(Tag.FXPlatform)
    private class TestAction extends TargetAbstractAction
    {

        public TestAction(String actionName, Object ped, ClassTarget t)
        {
            super(actionName, ped, t);
        }
                    
        public TestAction(String actionName, Object ped, ClassTarget t, String testName)
        {
            super(actionName, ped, t);
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public void actionPerformed(ActionEvent e)
        {
            
        }
    }

    @OnThread(Tag.FXPlatform)
    private class MakeTestCaseAction extends TargetAbstractAction
    {
        public MakeTestCaseAction(String name, Object ped, ClassTarget t)
        {
            super(name, ped, t);
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public void actionPerformed(ActionEvent e)
        {
            
        }
    }

    @OnThread(Tag.FXPlatform)
    private class BenchToFixtureAction extends TargetAbstractAction
    {
        public BenchToFixtureAction(String name, Object ped, ClassTarget t)
        {
            super(name, ped, t);
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public void actionPerformed(ActionEvent e)
        {
            
        }
    }

    @OnThread(Tag.FXPlatform)
    private class FixtureToBenchAction extends TargetAbstractAction
    {
        public FixtureToBenchAction(String name, Object ped, ClassTarget t)
        {
            super(name, ped, t);
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public void actionPerformed(ActionEvent e)
        {
            
        }
    }
}
