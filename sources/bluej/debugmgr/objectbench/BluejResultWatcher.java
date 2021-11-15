/*
 This file is part of the BlueJ program. 
 Copyright (C) 2018,2019  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.objectbench;

import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.views.CallableView;
import bluej.views.MethodView;

/**
 * A standard watcher for invocation results in BlueJ.
 * <p>
 * Over ObjectResultWatcher, this mainly just adds setting of the wait cursor on the PkgMgrFrame
 * associated with the invocation.
 * 
 * @author Davin McCall
 */
@SuppressWarnings("unused")
public abstract class BluejResultWatcher extends ResultWatcherBase
{
    private Object pmf;

    /**
     * Constructor for BluejResultWatcher, for a constructor or static method call.
     * 
     * @param className      the name of the class whose member is called
     * @param pkg    the package in which the invocation occurs
     * @param pmf    the PkgMgrFrame for the package
     * @param method  the method/constructor being invoked
     */
    public BluejResultWatcher(Package pkg, Object pmf, CallableView method)
    {
        super(pkg, null, method);
    }
    
    /**
     * Constructor for BluejResultWatcher, for an instance method call.
     * 
     * @param obj    the target object of the invocation
     * @param objInstanceName   the name of the target instance
     * @param pkg    the package in which the invocation occurs
     * @param pmf    the PkgMgrFrame for the package
     * @param method  the method being invoked
     */
    public BluejResultWatcher(DebuggerObject obj, String objInstanceName, Package pkg, Object pmf, MethodView method)
    {
        super(obj, objInstanceName, pkg, null, method);
        this.pmf = pmf;
    }
    
    @Override
    public void beginCompile()
    {
        
    }

    @Override
    public void beginExecution(InvokerRecord ir)
    {
        
    }

    @Override
    public void putError(String msg, InvokerRecord ir)
    {
        
    }
}
