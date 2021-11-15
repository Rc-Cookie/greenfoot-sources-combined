/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2016,2017,2018  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.inspector;

import bluej.Config;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.ExpressionInformation;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * A window that displays a method return value.
 * 
 * @author Poul Henriksen
 */
@SuppressWarnings("unused")
@OnThread(Tag.FXPlatform)
public class ResultInspector extends Inspector
{

    // === static variables ===

    protected final static String resultTitle = Config.getString("debugger.inspector.result.title");
    protected final static String returnedString = Config.getString("debugger.inspector.result.returned");

    // === instance variables ===

    protected DebuggerObject obj;
    protected String objName; // name on the object bench

    private ExpressionInformation expressionInformation;
    private JavaType resultType; // static result type
    private VBox contentPane;


    /**
     * Note: 'pkg' may be null if 'ir' is null.
     * 
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we created this result/object
     *            if null, the "get" button is permanently disabled
     * @param info
     *            The expression used to create the object (ie. the method call
     *            information)
     * @param parent
     *            The parent frame of this frame
     */
    public ResultInspector(DebuggerObject obj, InspectorManager inspectorManager, String name,
            Package pkg, InvokerRecord ir, ExpressionInformation info)
    {
        super(inspectorManager, pkg, ir, StageStyle.DECORATED);

        expressionInformation = info;
        this.obj = obj;
        this.objName = name;

        calcResultType();

        makeFrame();
        update();
    }

    /**
     * Determine the expected static type of the result.
     */
    private void calcResultType()
    {
        
    }

    @Override
    protected boolean shouldAutoUpdate()
    {
        return false;
    }

    /**
     * Returns a single string representing the return value.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    protected List<FieldInfo> getListData()
    {
        String fieldString;
        DebuggerField resultField = obj.getField(0);
        if (!resultType.isPrimitive()) {
            DebuggerObject resultObject = resultField.getValueObject(resultType);
            if (!resultObject.isNullObject()) {
                fieldString = resultObject.getGenType().toString(true);
            }
            else {
                fieldString = resultType.toString(true);
            }
        }
        else {
            fieldString = resultField.getType().toString(true);
        }
        
        List<FieldInfo> rlist = new ArrayList<FieldInfo>(1);
        rlist.add(new FieldInfo(fieldString, resultField.getValueString()));
        return rlist;
    }

    /**
     * Build the GUI
     * 
     * @param showAssert
     *            Indicates if assertions should be shown.
     */
    protected void makeFrame()
    {
        
    }

    @Override
    public Region getContent()
    {
        return contentPane;
    }

    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
        DebuggerField field = obj.getInstanceField(0);
        if (field.isReferenceType() && ! field.isNull()) {
            // Don't use the name, since it is meaningless anyway (it is always "result")
            setCurrentObj(field.getValueObject(resultType), null, resultType.toString(false));
            setButtonsEnabled(true, true);
        }
        else {
            setCurrentObj(null, null, null);
            setButtonsEnabled(false, false);
        }
    }

    @Override
    protected void doInspect()
    {
        if (selectedField != null) {
            boolean isPublic = !getButton.isDisable();
            inspectorManager.getInspectorInstance(selectedField, selectedFieldName, pkg, isPublic ? ir : null, this, null);
        }
    }
    
    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        if(inspectorManager != null) {
            inspectorManager.removeInspector(obj);
        }
    }

    /**
     * return a String with the result.
     * 
     * @return The Result value
     */
    public String getResult()
    {
        DebuggerField resultField = obj.getField(0);
        
        String result = resultField.getType() + " " + resultField.getName() + " = " + resultField.getValueString();
        return result;
    }

    protected int getPreferredRows()
    {
        return 2;
    }
    
    protected void doGet()
    {
        
    }
}
