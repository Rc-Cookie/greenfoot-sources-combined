/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
import java.util.Map.Entry;
import java.util.stream.Stream;

import threadchecker.OnThread;
import threadchecker.Tag;

@SuppressWarnings("unused")
public class NormalMethodElement
{
    public static final String ELEMENT = "method";
    private boolean staticModifier = false;
    private boolean finalModifier = false;
    private final Object returnType;
    private final Object name;
    
    public NormalMethodElement(Object frame, Object access, boolean staticModifier, 
            boolean finalModifier, Object returnType, Object name, List<?> params,
            List<?> throwsTypes, List<CodeElement> contents, Object documentation, boolean enabled)
    {
        this.staticModifier = staticModifier;
        this.finalModifier = finalModifier;
        this.returnType = returnType;
        this.name = name;
    }
    
    public NormalMethodElement(Object el)
    {
        returnType = null;
        name = null;
    }
    
    public NormalMethodElement(String access, String returnType, String name, List<Entry<String,String>> params, 
            List<CodeElement> contents, String documentation)
    {
        this.returnType = null;
        this.name = null;
    }

    public Object toJavaSource()
    {
        return null;
    }

    public Object toXML()
    {
        return null;
    }
    
    public Object createFrame(Object editor)
    {
        return null;
    }

    public String getType()
    {
        return null;
    }

    public String getName()
    {
        return null;
    }
    
    public void show(Object reason)
    {
              
    }

    @OnThread(Tag.FXPlatform)
    public boolean equalDeclaration(String name, List<?> params, Object el)
    {
        return true;
    }

    protected Stream<?> getDirectSlotFragments()
    {
        return null;
    }

    public Object getFrame()
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
}
