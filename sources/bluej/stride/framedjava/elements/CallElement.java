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

import java.util.stream.Stream;

@SuppressWarnings("unused")
public class CallElement extends CodeElement
{
    public static final String ELEMENT = "call";
    private final Object call;
    private Object frame;
    
    public CallElement(Object frame, Object call, boolean enabled)
    {
        this.frame = frame;
        this.call = call;
        this.enable = enabled;
    }
    
    public CallElement(Object el)
    {
        call = null;
    }
    
    public CallElement(String call, String javaCode)
    {
        this.call = null;
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
}
