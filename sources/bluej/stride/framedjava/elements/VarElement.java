/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.elements;

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class VarElement extends CodeElement
{
    public static final String ELEMENT = "variable";
    private final Object varAccess;
    private boolean staticModifier = false;
    private boolean finalModifier = false;
    private final Object varType;
    private final Object varName;
    private final Object varValue;
    private Object frame;
    
    // varValue is optional and can be null
    public VarElement(Object frame, Object varAccess, boolean staticModifier, 
            boolean finalModifier, Object varType, Object varName, 
            Object varValue, boolean enabled)
    {
        this.frame = frame;
        this.varAccess = varAccess;
        this.staticModifier = staticModifier;
        this.finalModifier = finalModifier;
        this.varType = varType;
        this.varName = varName;
        this.varValue = varValue;
        this.enable = enabled;
    }
    
    public VarElement(Object el)
    {
        varAccess = null;
        varType = null;
        varName = null;
        varValue = null;
    }

    public VarElement(String access, String type, String name, String value)
    {
        varAccess = null;
        varType = null;
        varName = null;
        varValue = null;
    }

    @Override
    public Object toJavaSource()
    {
        return null;
    }

    @Override
    public Object toXML()
    {
        return null;
    }
    
    @Override
    public Object createFrame(Object editor)
    {
        return null;
    }

    @Override
    public List<LocalParamInfo> getDeclaredVariablesAfter()
    {
        return null;
    }

    public Object showDebugBefore(Object debug)
    {
        return null;
    }
    
    @Override
    public void show(Object reason)
    {
            
    }

    @Override
    protected Stream<?> getDirectSlotFragments()
    {
        return null;
    }

    public boolean isStatic()
    {
        return staticModifier;
    }

    public boolean isFinal()
    {
        return finalModifier;
    }

    public String getType()
    {
        return null;
    }

    public String getName()
    {
        return null;
    }

    public String getValue()
    {
        return null;
    }
}
