/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013,2014,2016,2018  Michael Kolling and John Rosenberg
 
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
package bluej.extensions;

import bluej.collect.DataCollector;
import bluej.extensions.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 * A proxy object which provides services to BlueJ extensions.
 * From this class
 * an extension can obtain the projects and packages which BlueJ is currently displayng
 * and the classes and objects they contain. Fields and methods of these objects
 * can be inspected and invoked using an API based on Java's reflection API.
 *
 * Every effort has been made to retain the logic of the Reflection API and to provide
 * methods that behave in a very similar way.
 *
 * <PRE>
 * BlueJ
 *   |
 *   +---- BProject
 *             |
 *             +---- BPackage
 *                      |
 *                      +--------- BClass
 *                      |            |
 *                      +- BObject   + BConstructor
 *                                   |      |
 *                                   |      +- BObject
 *                                   |
 *                                   +---- BMethod
 *                                   |      |
 *                                   |      +- BObject
 *                                   |
 *                                   +---- BField
 *
 * </PRE>
 * Attempts to invoke methods on a BlueJ object made by an extension
 * after its <code>terminate()</code> method has been called will result
 * in an (unchecked) <code>ExtensionUnloadedException</code> being thrown.
 *
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003, 2004, 2005
 */
public final class BlueJ
{
    public static final int SE_PROJECT = 0;
    /**
     * This is left here for compatibility with old extensions, but
     * will never be used.
     */
    public static final int ME_PROJECT = 1;
    // This is the queue for the whole of them
    private ArrayList<ApplicationListener> applicationListeners;
    private ArrayList<CompileListener> compileListeners;


    /**
     * Constructor for a BlueJ proxy object.
     * See the ExtensionBridge class.
     *
     * @param  aWrapper      Description of the Parameter
     * @param  aPrefManager  Description of the Parameter
     */
    BlueJ(Object aWrapper, Object aPrefManager)
    {

    }

    /**
     * Opens a project.
     *
     * @param  directory  Where the project is stored.
     * @return            the BProject that describes the newly opened project,
     *                    or null if it cannot be opened.
     */
    public final Object openProject(File directory)
    {
        return null;
    }


    /**
     * Creates a new BlueJ project.
     *
     * @param  directory    where you want the project be placed, it must be writable.
     * @param  projectType  the type of project, currently only SE_PROJECT is available.
     * @return              the newly created BProject if successful, null otherwise.
     */
    public Object newProject(File directory, int projectType )
    {
        return null;
    }


    /**
     * Creates a new BlueJ project.
     *
     * @param  directory  where you want the project be placed, it must be writable.
     * @return            the newly created BProject if successful, null otherwise.
     */
    public Object newProject(File directory)
    {
        return null;
    }

    
    /**
     * Returns all currently open projects.
     * Returns an empty array if no projects are open.
     *
     * @return    The openProjects value
     */
    public Object[] getOpenProjects()
    {
        return null;
    }


    /**
     * Returns the currently selected package.
     * The current package is the one that is currently selected by the
     * user interface.
     * It can return null if there is no currently open package.
     *
     * @return    The currentPackage value
     */
    public Object getCurrentPackage()
    {
        return null;
    }


    /**
     * Returns the current frame being displayed.
     * Can be used (e.g.) as a "parent" frame for positioning modal dialogs.
     * If there is a package currently open, it's probably better to use its <code>getFrame()</code>
     * method to provide better placement.
     *
     * @return    The currentFrame value
     */
    public Frame getCurrentFrame()
    {
        return null;
    }


    /**
     * Install a new menu generator for this extension.
     * If you want to delete a previously installed menu, then set it to null
     *
     *
     * @param  menuGen        The new menuGenerator value
     */
    public void setMenuGenerator(Object menuGen)
    {
        
    }


    /**
     * Returns the currently registered menu generator
     *
     * @return    The menuGenerator value
     */
    public Object getMenuGenerator()
    {
        return null;
    }


    /**
     * Install a new preference panel for this extension.
     * If you want to delete a previously installed preference panel, then set it to null
     *
     * @param  prefGen  a class instance that implements the PreferenceGenerator interface.
     */
    public void setPreferenceGenerator(Object prefGen)
    {
        
    }


    /**
     * Returns the currently registered preference generator.
     *
     * @return    The preferenceGenerator value
     */
    public Object getPreferenceGenerator()
    {
        return null;
    }


    /**
     * Installs a new custom class target painter for this extension. If you
     * want to delete a previously installed custom class target painter, then
     * set it to <code>null</code>.
     * 
     * @param classTargetPainter
     *            The {@link ExtensionClassTargetPainter} to set.
     */
    public void setClassTargetPainter(Object classTargetPainter)
    {
        
    }

    /**
     * Returns the currently registered custom class target painter.
     * 
     * @return The currently registered custom class target painter.
     */
    public Object getClassTargetPainter()
    {
        return null;
    }

    /**
     * Returns the path of the <code>&lt;BLUEJ_HOME&gt;/lib</code> system directory.
     * This can be used to locate systemwide configuration files.
     * Having the directory you can then locate a file within it.
     *
     * @return    The systemLibDir value
     */
    public File getSystemLibDir()
    {
        return null;
    }


    /**
     * Returns the path of the user configuration directory.
     * This can be used to locate user dependent information.
     * Having the directory you can then locate a file within it.
     *
     * @return    The userConfigDir value
     */
    public File getUserConfigDir()
    {
        return null;
    }
    
    /**
     * Returns the data-collection user ID, for use with extensions that
     * aim to augment the BlueJ data collection project.
     * 
     * Since extension version 2.10
     * 
     * @return the user ID, as read from the properties file.
     */
    public String getDataCollectionUniqueID()
    {
        return DataCollector.getUserID();
    }


    /**
     * Returns a property from BlueJ's properties,
     * or the given default value if the property is not currently set.
     *
     * @param  property  The name of the required global property
     * @param  def       The default value to use if the property cannot be found.
     * @return           the value of the property.
     */
    public String getBlueJPropertyString(String property, String def)
    {
        return null;
    }


    /**
     * Return a property associated with this extension from the standard BlueJ property repository.
     * You must use the setExtensionPropertyString to write any property that you want stored.
     * You can then come back and retrieve it using this function.
     *
     * @param  property  The name of the required global property.
     * @param  def       The default value to use if the property cannot be found.
     * @return           the value of that property.
     */
    public String getExtensionPropertyString(String property, String def)
    {
        return null;
    }


    /**
     * Sets a property associated with this extension into the standard BlueJ property repository.
     * The property name does not need to be fully qualified since a prefix will be prepended to it.
     *
     *
     * @param  property  The name of the required global property
     * @param  value     the required value of that property (or null to remove the property)
     */
    public void setExtensionPropertyString(String property, String value)
    {
        
    }


    /**
     * Returns the language-dependent label with the given key.
     * The search order is to look first in the extension's <code>label</code> files and
     * if the requested label is not found in the BlueJ system <code>label</code> files.
     * Extensions' labels are stored in a Property format and must be jarred together
     * with the extension. The path searched is equivalent to the bluej/lib/[language]
     * style used for the BlueJ system labels. E.g. to create a set of labels which can be used
     * by English, Italian and German users of an extension, the following files would need to
     * be present in the extension's Jar file:
     * <pre>
     * lib/english/label
     * lib/italian/label
     * lib/german/label
     * </pre>
     * The files named <code>label</code> would contain the actual label key/value pairs.
     *
     * @param  key  Description of the Parameter
     * @return      The label value
     */
    public String getLabel(String key)
    {
        return null;
    }



    /**
     * Registers a listener for all the events generated by BlueJ.
     */
    public void addExtensionEventListener(Object listener)
    {
        
    }


    /**
     * Removes the specified listener so that it no longer receives events.
     */
    public void removeExtensionEventListener(Object listener)
    {
        
    }


    /**
     * Registers a listener for application events.
     */
    public void addApplicationListener(ApplicationListener listener)
    {
        if (listener != null) {
            synchronized (applicationListeners) {
                applicationListeners.add(listener);
            }

            // Relay a previous given up message:
            if (DataCollector.hasGivenUp())
                listener.dataSubmissionFailed(new ApplicationEvent(ApplicationEvent.DATA_SUBMISSION_FAILED_EVENT));
        }
    }


    /**
     * Removes the specified listener so that it no longer receives events.
     */
    public void removeApplicationListener(ApplicationListener listener)
    {
        if (listener != null) {
            synchronized (applicationListeners) {
                applicationListeners.remove(listener);
            }
        }
    }


    /**
     * Registers a listener for package events.
     */
    public void addPackageListener(Object listener)
    {
        
    }


    /**
     * Removes the specified listener so that it no longer receives events.
     */
    public void removePackageListener(Object listener)
    {
        
    }


    /**
     * Registers a listener for compile events.
     */
    public void addCompileListener(CompileListener listener)
    {
        if (listener != null) {
            synchronized (compileListeners) {
                compileListeners.add(listener);
            }
        }
    }


    /**
     * Removes the specified listener so that it no longer receives events.
     */
    public void removeCompileListener(CompileListener listener)
    {
        if (listener != null) {
            synchronized (compileListeners) {
                compileListeners.remove(listener);
            }
        }
    }


    /**
     * Registers a listener for invocation events.
     */
    public void addInvocationListener(Object listener)
    {
        
    }


    /**
     * Removes the specified listener so no that it no longer receives events.
     */
    public void removeInvocationListener(Object listener)
    {
        
    }


    /**
     * Register a listener for class events.
     * 
     * @param listener
     */
    public void addClassListener(Object listener)
    {
        
    }
    
    /**
     * Removes the specified class listener so no that it no longer receives
     * class events.
     */
    public void removeClassListener(Object listener)
    {
        
    }
    
    /**
     * Register a listener for dependency events.
     * 
     * @param listener
     *            The listener to register.
     */
    public void addDependencyListener(Object listener)
    {
        
    }

    /**
     * Removes the specified dependency listener so it no longer receives
     * dependency events.
     */
    public void removeDependencyListener(Object listener)
    {
        
    }
    
    /**
     * Register a listener for class target events.
     * 
     * @param listener
     *            The listener to register.
     */
    public void addClassTargetListener(Object listener)
    {
        
    }

    /**
     * Removes the specified class target listener so it no longer receives
     * class target events.
     */
    public void removeClassTargetListener(Object listener)
    {
        
    }

    /**
     * Informs any registered listeners that an event has occurred.
     * This will call the various dispatcher as needed.
     * Errors will be trapped by the caller.
     */
    void delegateEvent(Object event)
    {
        
    }



    /**
     * Calls the extension to get the right menu item.
     * This is already wrapped for errors in the caller.
     * It is right for it to create a new wrapped object each time.
     * We do not want extensions to share objects.
     * It is here since it can access all constructors directly.
     *
     * @param  attachedObject  Description of the Parameter
     * @return                 The menuItem value
     */
    JMenuItem getMenuItem(Object attachedObject)
    {
        return null;
    }


    /**
     * Post a notification of a menu going to be displayed
     */
    void postMenuItem(Object attachedObject, JMenuItem onThisItem)
    {
        
    }

    /**
     * Calls the extension to draw its representation of a class target.
     * 
     * @param layer
     *            The layer of the drawing which causes the different methods of
     *            the {@link ExtensionClassTargetPainter} instance to be called.
     * @param bClassTarget
     *            The {@link BClassTarget} which represents the class target
     *            that will be painted.
     * @param graphics
     *            The {@link Graphics2D} instance to draw on.
     * @param width
     *            The width of the area to paint.
     * @param height
     *            The height of the area to paint.
     */
    void drawExtensionClassTarget(Object layer, BClassTarget bClassTarget, Graphics2D graphics,
            int width, int height)
    {
        
    }
}
