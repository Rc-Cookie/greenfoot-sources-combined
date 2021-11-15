/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2016  Michael Kolling and John Rosenberg 
 
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

import java.util.List;

import javafx.application.Platform;

import bluej.pkgmgr.target.ClassTarget;

/**
 * A wrapper for a class target (vertex) in the class diagram of BlueJ.
 * 
 * @author Simon Gerlach
 */
public class BClassTarget
{
    /**
     * Constructor. Creates a new {@link BClassTarget}.
     * 
     * @param targetId
     *            The {@link Identifier} which represents the corresponding
     *            class target. It is duty of the caller to guarantee that this
     *            <code>targetId</code> is reasonable.
     */
    BClassTarget(Object targetId)
    {
        
    }

    /**
     * Returns the wrapped {@link ClassTarget} or <code>null</code> if no such
     * class target exist.
     * 
     * @return The wrapped {@link ClassTarget}.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    ClassTarget getClassTarget() throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }

    /**
     * Notification that the name of the underlying class has changed.
     * 
     * @param newName
     *            The new class name, fully qualified.
     */
    void nameChanged(String newName)
    {
        
    }

    /**
     * Recalculates the dependency arrows associated with this class target.
     * This may be necessary if the user has moved or resized the class target.
     * 
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public void recalcDependentPositions() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget classTarget = getClassTarget();
        
        if (classTarget != null) {
            Platform.runLater(() -> {classTarget.recalcDependentPositions();});
        }
    }

    /**
     * Revalidates the editor the wrapped class target is part of.
     * 
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public void revalidate() throws ProjectNotOpenException, PackageNotFoundException
    {
        
    }

    /**
     * Returns the class of this class target. Similar to Reflection API. Note
     * the naming inconsistency, which avoids a clash with
     * {@link java.lang.Object#getClass()}. May return <code>null</code> if this
     * class target is no longer valid.
     * 
     * @return The class of this class target or <code>null</code> if there is
     *         no such class.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public BClass getBClass() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget classTarget = getClassTarget();
        return (classTarget != null) ? classTarget.getBClass() : null;
    }

    /**
     * Indicates whether this class target represents an interface.
     * 
     * @return <code>true</code> if this target represents an interface,
     *         <code>false</code> otherwise.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public boolean isInterface() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget classTarget = getClassTarget();
        return (classTarget != null) ? classTarget.isInterface() : false;
    }

    /**
     * Indicates whether this class target represents a JUnit test.
     * 
     * @return <code>true</code> if this target represents a JUnit test,
     *         <code>false</code> otherwise.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public boolean isUnitTest() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget classTarget = getClassTarget();
        return (classTarget != null) ? classTarget.isUnitTest() : false;
    }

    /**
     * Indicates whether this class target shall be visible in the graph.
     * 
     * @return <code>true</code> if this class target is visible,
     *         <code>false</code> otherwise.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public boolean isVisible() throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget classTarget = getClassTarget();
        return (classTarget != null) ? classTarget.isVisible() : false;
    }

    /**
     * Sets the visible setting of this class target.
     * 
     * @param visible
     *            The new visible setting.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public void setVisible(boolean visible) throws ProjectNotOpenException, PackageNotFoundException
    {
        ClassTarget classTarget = getClassTarget();

        if (classTarget != null) {
            Platform.runLater(() -> {classTarget.setVisible(visible);});
        }
    }

    /**
     * Returns the associated target of this class target or <code>null</code>
     * if there is no associated target. For example, this can be the the class
     * target of the corresponding test class of the class represented by this
     * class target.
     * 
     * @return The associated target of this class target or <code>null</code>
     *         if there is no associated target.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public BClassTarget getAssociation() throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }

    /**
     * Returns a {@link List} containing all dependencies that have this class
     * target as their origin.
     * 
     * @return A {@link List} containing all outgoing dependencies.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public List<?> getOutgoingDependencies() throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }

    /**
     * Returns a {@link List} containing all dependencies that have this class
     * target as their destination.
     * 
     * @return A {@link List} containing all incoming dependencies.
     * @throws ProjectNotOpenException
     *             if the project to which this class target belongs has been
     *             closed by the user.
     * @throws PackageNotFoundException
     *             if the package to which this class target belongs has been
     *             deleted by the user.
     */
    public List<?> getIncomingDependencies() throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }

    /**
     * Returns a {@link String} representation of this object.
     */
    @Override
    public String toString()
    {
        try {
            ClassTarget classTarget = getClassTarget();
            return "BClassTarget: " + classTarget.getIdentifierName();
        } catch (ExtensionException e) {
            return "BClassTarget: INVALID";
        }
    }
}
