/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2016,2017  Michael Kolling and John Rosenberg
 
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

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import bluej.pkgmgr.Package;
import javafx.geometry.Point2D;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A target that has relationships to other targets
 *
 * @author   Michael Cahill
 * @author   Michael Kolling
 */
public abstract class DependentTarget extends EditableTarget
{
    /**
     * States.  A compile target has two pieces of information.
     *   It can be up-to-date (i.e. class file matches latest source state)
     *   or it can need a compile (i.e. class file lags source state) with unknown error state,
     *   or it can need a compile and be known to have an error.
     */
    @OnThread(Tag.Any)
    public static enum State
    {
        COMPILED, NEEDS_COMPILE, HAS_ERROR;
    }

    @OnThread(Tag.Any)
    private final AtomicReference<State> state = new AtomicReference<>(State.NEEDS_COMPILE);
    @OnThread(Tag.FXPlatform)
    protected final List<TargetListener> stateListeners = new ArrayList<>();

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<?> inUses;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<?> outUses;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<?> parents;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<?> children;

    @OnThread(value = Tag.Any,requireSynchronized = true)
    protected DependentTarget assoc;

    /**
     * Create a new target belonging to the specified package.
     */
    public DependentTarget(Package pkg, String identifierName)
    {
        super(pkg, identifierName);

        inUses = new ArrayList<>();
        outUses = new ArrayList<>();
        parents = new ArrayList<>();
        children = new ArrayList<>();

        assoc = null;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void setPos(int x, int y)
    {
        super.setPos(x,y);
        recalcDependentPositions();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setSize(int width, int height)
    {
        super.setSize(width, height);
        recalcDependentPositions();
    }

    /**
     * Save association information about this class target
     * @param props the properties object to save to
     * @param prefix an internal name used for this target to identify
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        if (getAssociation() != null) {
            String assocName = getAssociation().getIdentifierName(); 
            props.put(prefix + ".association", assocName);
        }
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void setAssociation(DependentTarget t)
    {
        assoc = t;
        //assoiated classes are not allowed to move on their own
        if (assoc != null && assoc.isMoveable()){
            assoc.setIsMoveable(false);
        }
        if (assoc != null && assoc.isResizable())
        {
            assoc.setResizable(false);
        }
    }

    @OnThread(value = Tag.Any, requireSynchronized = true)
    public synchronized DependentTarget getAssociation()
    {
        return assoc;
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void addDependencyOut(Object d, boolean recalc)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void addDependencyIn(Object d, boolean recalc)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void removeDependencyOut(Object d, boolean recalc)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public synchronized void removeDependencyIn(Object d, boolean recalc)
    {
        
    }

    @OnThread(Tag.Any)
    public synchronized Collection<?> dependencies()
    {
        return null;
    }

    @OnThread(Tag.Any)
    public synchronized Collection<?> dependents()
    {
        return null;
    }
    
    /**
     * Get the dependencies between this target and its parent(s).
     * The returned list should not be modified and may be a view or a copy.
     */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    public synchronized List<?> getParents()
    {
        return null;
    }
    
    /**
     * Get the dependencies between this target and its children.
     * 
     * @return
     */
    @OnThread(value = Tag.Any)
    public synchronized List<?> getChildrenDependencies()
    {
        return null;
    }

    @OnThread(Tag.Any)
    public synchronized List<?> dependentsAsList()
    {
        return null;
    }

    @OnThread(Tag.Any)
    public synchronized List<?> usesDependencies()
    {
        return null;
    }
    
    /**
     *  Remove all outgoing dependencies. Also updates the package. (Don't
     *  call from package remove method - this will cause infinite recursion.)
     */
    @OnThread(Tag.FXPlatform)
    protected synchronized void removeAllOutDependencies()
    {
        
    }

    /**
     *  Remove inheritance dependencies.
     */
    @OnThread(Tag.FXPlatform)
    protected synchronized void removeInheritDependencies()
    {
        
    }

    /**
     *  Remove all incoming dependencies. Also updates the package. (Don't
     *  call from package remove method - this will cause infinite recursion.)
     */
    @OnThread(Tag.FXPlatform)
    protected synchronized void removeAllInDependencies()
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public void recalcOutUses()
    {
        
    }

    /**
     * Re-layout arrows into this target
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void recalcInUses()
    {
        
    }

    /**
     * Returns from the specified {@link List} all uses dependencies which are
     * currently visible.
     * 
     * @param usesDependencies
     *            A {@link List} of uses dependencies.
     * @return A {@link List} containing all visible uses dependencies from the
     *         input list.
     */
    @OnThread(Tag.FXPlatform)
    private static List<?> getVisibleUsesDependencies(List<?> usesDependencies)
    {
        return null;
    }

    @OnThread(Tag.FXPlatform)
    public Point2D getAttachment(double angle)
    {
        double radius;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double tan = sin / cos;
        double m = (double) getHeight() / getWidth();

        if(Math.abs(tan) < m)   // side
            radius = 0.5 * getWidth() / Math.abs(cos);
        else    // top
            radius = 0.5 * getHeight() / Math.abs(sin);

        javafx.geometry.Point2D p = new Point2D(getX() + getWidth() / 2 + (int)(radius * cos),
                            getY() + getHeight() / 2 - (int)(radius * sin));

        // Correct for shadow
        /*
        if((-m < tan) && (tan < m) && (cos > 0))    // right side
            p.x += SHAD_SIZE;
        if((Math.abs(tan) > m) && (sin < 0) && (p.x > getX() + SHAD_SIZE))  // bottom
            p.y += SHAD_SIZE;
        */
        return p;
    }
    
    
    /**
     * The user may have moved or resized the target. If so, recalculate the
     * dependency arrows associated with this target.
     * @param editor
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void recalcDependentPositions() 
    {
        
    }

    @OnThread(Tag.FXPlatform)
    protected void updateAssociatePosition()
    {
        DependentTarget t = getAssociation();

        if (t != null) {
            //TODO magic numbers. Should also take grid size in to account.
            t.setPos(getX() + 30, getY() - 30);
            if (isResizable())
                t.setSize(getWidth(), getHeight());
            t.recalcDependentPositions();
        }
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
    
    /**
     * Return the current state of the target (one of S_NORMAL, S_INVALID,
     * S_COMPILING)
     */
    @OnThread(Tag.Any)
    public State getState()
    {
        return state.get();
    }

    /**
     * Mark the class as needing a compile (if it is not marked thus already).
     *
     * Do not call this method on classes which lack source code.
     */
    public void markModified()
    {
        // If it's already NEEDS_COMPILE or HAS_ERROR, no need to change:
        if (getState() == State.COMPILED)
            setState(State.NEEDS_COMPILE);
    }
    
    /**
     * Change the state of this target. The target will be repainted to show the
     * new state.
     * 
     * @param newState The new state value
     */
    @OnThread(Tag.FXPlatform)
    public void setState(State newState)
    {
        state.set(newState);
        repaint();
        redraw();
        for (TargetListener stateListener : stateListeners)
        {
            stateListener.stateChanged(newState);
        }
    }

    /**
     * Adds a TargetListener to changes in this target.
     */
    @OnThread(Tag.FXPlatform)
    public void addListener(TargetListener listener)
    {
        stateListeners.add(listener);
    }

    /**
     * Removes a listener added by addListener
     */
    @OnThread(Tag.FXPlatform)
    public void removeListener(TargetListener listener)
    {
        stateListeners.remove(listener);
    }

    /**
     * A listener to changes in a DependentTarget
     */
    public static interface TargetListener
    {
        /**
         * Called when the editor has been opened.  If the same Editor instance is opened and closed
         * multiple times, this method is called on every open.
         */
        public void editorOpened();
        
        /**
         * Called when state has changed
         */
        public void stateChanged(State newState);

        /**
         * Called when the target is renamed.
         */
        public void renamed(String newName);
    }
}
