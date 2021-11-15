/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2016  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr;

import java.util.Map;

import javafx.stage.Window;

import bluej.Config;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.views.CallableView;
import bluej.views.MethodView;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * This dialog is used for an interactive method call. The call
 * can be an object creation or an invocation of an object method.
 * A new instance of this dialog is created for each method.
 *
 * @author  Michael Kolling
 * @author  Bruce Quig
 * @author  Poul Henriksen <polle@mip.sdu.dk>
 */
@OnThread(Tag.FXPlatform)
@SuppressWarnings("unused")
public class MethodDialog extends CallDialog
{
    private final boolean rawObject;

    // Window Titles
    private static final String appName = Config.getApplicationName(); 
    static final String wCallRoutineTitle = appName + ":  " + Config.getString("pkgmgr.methodCall.titleCall");

    private final String methodName;
    private final MethodView method;
    private final Map<String,GenTypeParameter> typeParameterMap;
    private final Invoker invoker;

    /**
     * MethodDialog constructor.
     * 
     * @param parentFrame  The parent window for the dialog
     * @param ob           The object bench to listen for object selection on
     * @param callHistory  The call history tracker
     * @param instanceName The initial instance name (for a constructor dialog)
     *                     or the object instance on which the method is being called
     * @param method       The constructor or method being used
     * @param typeMap      The mapping of type parameter names to runtime types
     *                     (a Map of String -> GenType).
     */
    public MethodDialog(Window parentFrame, ObjectBenchInterface ob, CallHistory callHistory,
                        String instanceName, MethodView method, Map<String,GenTypeParameter> typeMap, Invoker invoker)
    {
        super(parentFrame, ob, "");
        this.invoker = invoker;
        
        history = callHistory;

        // Find out the type of dialog
        methodName = method.getName();

        this.method = method;
        rawObject = false;
        typeParameterMap = null;
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     * Collects arguments and calls watcher objects (Invoker).
     */
    @Override
    public void handleOK()
    {
        if (!parameterFieldsOk()) {
            setErrorMessage(emptyFieldMsg);            
        }
        else if(!typeParameterFieldsOk()) {
            setErrorMessage(emptyTypeFieldMsg);
        } 
        else {
            setWaitCursor(true);
            invoker.callDialogOK();
        }
    }

    /*
     * @see bluej.debugmgr.CallDialog#getCallableView()
     */
    @Override
    protected CallableView getCallableView()
    {
        return method;
    }
    
    /*
     * @see bluej.debugmgr.CallDialog#targetIsRaw()
     */
    @Override
    protected boolean targetIsRaw()
    {
        return rawObject;
    }

    /*
     * @see bluej.debugmgr.CallDialog#getTargetTypeArgs()
     */
    @Override
    protected Map<String, GenTypeParameter> getTargetTypeArgs()
    {
        return typeParameterMap;
    }
}
