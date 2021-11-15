/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.views;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A representation of a Java method in BlueJ
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
public class MethodView extends CallableView implements Comparable<MethodView>
{
    @OnThread(Tag.Any)
    protected final Method method;
    protected View returnType;
    private JavaType jtReturnType;

    /**
     * Constructor.
     */
    public MethodView(View view, Method method) throws ClassNotFoundException
    {
        super(view);
        this.method = method;
        jtReturnType = null;
    }

    public Method getMethod()
    {
        return method;
    }
    
    /**
     * Returns a string describing this Method.
     */
    public String toString()
    {
        return method.toString();
    }

    @OnThread(Tag.Any)
    public int getModifiers()
    {
        return method.getModifiers();
    }

    public boolean hasParameters()
    {
        return (method.getParameterTypes().length > 0);
    }
    
    public boolean isConstructor()
    {
        return false;
    }

    /**
     * Returns a signature string in the format
     * "type name(type,type,type)".
     */
    @Override
    public String getSignature()
    {
        return null;
    }
    
    /**
     * Get the "call signature", ie. the signature without the return type.
     * This should not be made user visible, it is for internal purposes only.
     * It is useful for locating methods which override a method in a super
     * class, without having to worry about covariant returns and generic
     * methods etc.
     */
    public String getCallSignature()
    {
        StringBuffer name = new StringBuffer();
        name.append(method.getName());
        name.append('(');
        Class<?>[] params = method.getParameterTypes();
        for(int i = 0; i < params.length; i++) {
            name.append(params[i].getName());
            if (i != params.length - 1) {
                name.append(',');
            }
        }
        name.append(')');
        return name.toString();
    }
    
    /**
     * Get a short String describing this member. A description is similar
     * to the signature, but it has parameter names in it instead of types.
     */
    @Override
    public String getShortDesc()
    {
        return null;
    }

    /**
     * Get a long String describing this member. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    @Override
    public String getLongDesc()
    {
        return null;
    }
    
    /**
     * Get a long String describing this member, with type parameters from the
     * class mapped to the corresponding instantiation type. Type parameters
     * not contained in the map are mapped to their erasure type; type
     * parameters from a generic method are left unmapped.
     * 
     * @param genericParams  The map of String -> GenType
     * @return  the signature string with type parameters mapped
     */
    public String getLongDesc(Map<String,GenTypeParameter> genericParams)
    {
        return null;
    }

    /**
     * Get an array of Class objects representing method's parameters
     * @returns array of Class objects
     */
    public Class<?>[] getParameters()
    {
        return method.getParameterTypes();
    }
    
    @Override
    public JavaType[] getParamTypes(boolean raw)
    {
        return null;
    }
    
    @Override
    public GenTypeDeclTpar[] getTypeParams() throws ClassNotFoundException
    {
        return null;
    }
    
    @Override
    public String[] getParamTypeStrings()
    {
        return null;
    }

    /**
     * Returns the name of this method as a String
     */
    public String getName()
    {
        return method.getName();
    }

    /**
     * Check whether this is method returns void
     */
    public boolean isVoid()
    {
        return method.getReturnType() == void.class;
    }

    /**
     * @returns if this method is the main method (a static void returning
     * function called main with a string array as an argument)
     */
    public boolean isMain()
    {
        if (!isVoid()) {
            return false;
        }
        if ("main".equals(getName())) {
            Class<?>[] c = getParameters();
            if (c.length != 1) {
                return false;
            }
            if (c[0].isArray() && String.class.equals(c[0].getComponentType())) {
                if (Modifier.isStatic(getModifiers()) && Modifier.isPublic(getModifiers())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Whether this method has a var arg.
     */
    @Override
    public boolean isVarArgs()
    {
        return false;
    }
    
    /**
     * Test whether the method is generic.
     */
    @Override
    public boolean isGeneric()
    {
        return false;
    }

    /**
     * Returns a Class object that represents the formal return type
     * of the method represented by this Method object.
     */
    public View getReturnType()
    {
        if (returnType == null) {
            returnType = View.getView(method.getReturnType());
        }
        return returnType;
    }
    
    /**
     * Get the return type of this method.
     */
    public JavaType getGenericReturnType()
    {
        return jtReturnType;
    }

    @OnThread(Tag.FXPlatform)
    public void print(Object out, Map<String,GenTypeParameter> typeParams, int indents)
    {
        
    }

    // ==== Comparable interface ====
    
    /**
     * Compare operation to provide alphabetical sorting by method name.
     */
    public int compareTo(MethodView other)
    {
        return method.getName().compareTo(other.method.getName());
    }
}
