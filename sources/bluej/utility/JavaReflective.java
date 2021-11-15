/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2014,2015,2016,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.utility;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.debugger.gentype.ConstructorReflective;
import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;

/**
 * A reflective for GenTypeClass which uses the standard java reflection API.  
 * 
 * @author Davin McCall
 */
public class JavaReflective extends Reflective
{
    private Class<?> c;
    
    @Override
    public int hashCode()
    {
        return c.hashCode();
    }
    
    @Override
    public boolean equals(Object other)
    {
        if (other instanceof JavaReflective) {
            JavaReflective jrOther = (JavaReflective) other;
            return jrOther.c == c;
        }
        return false;
    }
    
    public JavaReflective(Class<?> c)
    {
        if (c == null)
            throw new NullPointerException();
        this.c = c;
    }
    
    @Override
    public String getName()
    {
        return c.getName();
    }
    
    @Override
    public String getSimpleName()
    {
        if (c.isArray()) {
            return c.getComponentType().getName().replace('$', '.') + "[]";
        }
        else {
            return c.getName().replace('$', '.');
        }
    }

    @Override
    public boolean isInterface()
    {
        return c.isInterface();
    }
    
    @Override
    public boolean isStatic()
    {
        return Modifier.isStatic(c.getModifiers());
    }
    
    @Override
    public boolean isPublic()
    {
        return Modifier.isPublic(c.getModifiers());
    }
    
    @Override
    public boolean isFinal()
    {
        return Modifier.isFinal(c.getModifiers());
    }
    
    @Override
    public List<GenTypeDeclTpar> getTypeParams()
    {
        return null;
    }
    
    @Override
    public Reflective getArrayOf()
    {
        String rname;
        if (c.isArray())
            rname = "[" + c.getName();
        else
            rname = "[L" + c.getName() + ";";
        
        try {
            ClassLoader cloader = c.getClassLoader();
            Class<?> arrClass = Class.forName(rname, false, cloader);
            return new JavaReflective(arrClass);
        }
        catch (ClassNotFoundException cnfe) {}
        
        return null;
    }
    
    @Override
    public Reflective getRelativeClass(String name)
    {
        try {
            ClassLoader cloader = c.getClassLoader();
            if (cloader == null)
                cloader = ClassLoader.getSystemClassLoader();
            Class<?> cr = cloader.loadClass(name);
            return new JavaReflective(cr);
        }
        catch (ClassNotFoundException cnfe) {
            return null;
        }
        catch (LinkageError le) {
            return null;
        }
    }

    @Override
    public List<Reflective> getSuperTypesR()
    {
        List<Reflective> l = new ArrayList<Reflective>();
        
        // Arrays must be specially handled
        if (c.isArray()) {
            Class<?> ct = c.getComponentType();  // could be primitive, but won't matter
            JavaReflective ctR = new JavaReflective(ct);
            List<Reflective> componentSuperTypes = ctR.getSuperTypesR();
            Iterator<Reflective> i = componentSuperTypes.iterator();
            while (i.hasNext()) {
                JavaReflective componentSuperType = (JavaReflective) i.next();
                l.add(componentSuperType.getArrayOf());
            }
        }
        
        Class<?> superclass = c.getSuperclass();
        if( superclass != null )
            l.add(new JavaReflective(superclass));

        Class<?> [] interfaces = c.getInterfaces();
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(new JavaReflective(interfaces[i]));
        }
        
        // Interfaces with no direct superinterfaces have a supertype of Object
        if (superclass == null && interfaces.length == 0 && c.isInterface())
            l.add(new JavaReflective(Object.class));
        
        return l;
    }

    @Override
    public List<GenTypeClass> getSuperTypes()
    {
        return null;
    }
    
    /**
     * Get the underlying class (as a java.lang.Class object) that this
     * reflective represents.
     */
    public Class<?> getUnderlyingClass()
    {
        return c;
    }

    @Override
    public boolean isAssignableFrom(Reflective r)
    {
        if (r instanceof JavaReflective) {
            return c.isAssignableFrom(((JavaReflective)r).getUnderlyingClass());
        }
        else {
            return false;
        }
    }
    
    @Override
    public Map<String,FieldReflective> getDeclaredFields()
    {
        return null;
    }
    
    @Override
    public Map<String,Set<MethodReflective>> getDeclaredMethods()
    {
        return null;
    }

    @Override
    public List<ConstructorReflective> getDeclaredConstructors()
    {
        return null;
    }
    
    @Override
    public Reflective getOuterClass()
    {
        Class<?> declaring = c.getDeclaringClass();
        if (declaring != null) {
            return new JavaReflective(declaring);
        }
        return null;
    }
    
    @Override
    public Reflective getInnerClass(String name)
    {
        try {
            Class<?> [] declared = c.getDeclaredClasses();
            for (Class<?> inner : declared) {
                String innerName = inner.getName();
                int lastDollar = innerName.lastIndexOf('$');
                if (lastDollar != -1) {
                    String baseName = innerName.substring(lastDollar + 1);
                    if (baseName.equals(name)) {
                        return new JavaReflective(inner);
                    }
                }
            }
        }
        catch (LinkageError le) {}
        return null;
    }

    @Override
    public String getModuleName()
    {
        return c.getModule() != null ? c.getModule().getName() : null;
    }
}
