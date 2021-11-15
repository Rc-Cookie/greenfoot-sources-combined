/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2016  Michael Kolling and John Rosenberg 
 
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import bluej.utility.JavaNames;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * A representation of a Java class in BlueJ.
 * 
 * <p>The methods in this class are generally thread-safe.
 *
 * @author  Michael Cahill
 */
@OnThread(Tag.FXPlatform)
public class View
{
    /** The class that this view is for **/
    protected Class<?> cl;

    protected Object[] fields;
    protected Object[] allFields;
    protected ConstructorView[] constructors;
    protected MethodView[] methods;
    protected MethodView[] allMethods;
    protected Object[] typeParams;

    protected Object comment;

    private static Map<Class<?>,View> views = new HashMap<Class<?>,View>();

    /**
     * Return a view of a class.
     * This is the only way to obtain a View object.
     * This method is thread-safe.
     */
    public static View getView(Class<?> cl)
    {
        if(cl == null)
            return null;

        // Debug.message("Started getView for class " + cl);

        synchronized (views) {
            View v = views.get(cl);
            if(v == null) {
                v = new View(cl);
                views.put(cl, v);
            }

            // Debug.message("Ended getView for class " + cl);

            return v;
        }
    }

    /**
     * Remove from the view cache, all views of classes
     * which were loaded by the given class loader.
     * This method is thread-safe.
     */
    public static void removeAll(ClassLoader loader)
    {
        synchronized (views) {
            Iterator<View> it = views.values().iterator();

            while(it.hasNext()) {
                View v = it.next();

                if (v.getClassLoader() == loader) {
                    it.remove();
                }
            }
        }
    }

    private View(Class<?> cl)
    {
        this.cl = cl;
    }

    private ClassLoader getClassLoader()
    {
        return cl.getClassLoader();
    }

    public String getQualifiedName()
    {
        return cl.getName();
    }
    
    public String getPackageName()
    {
        String clName = cl.getName();
        int i = clName.lastIndexOf('.');
        if (i == -1)
            return "";
        else
            return clName.substring(0, i);
    }

    /**
     * Gets the Class this view is looking into.
     * This is used to know the exact return type of a method and is consistent 
     * with the Java Reflection API. Damiano
     */
    public Class<?> getViewClass ()
    {
        return cl;
    }

    public String getBaseName()
    {
        return JavaNames.getBase(cl.getName());
    }

    public View getSuper()
    {
        return getView(cl.getSuperclass());
    }

    public View[] getInterfaces()
    {
        Class<?>[] interfaces = cl.getInterfaces();

        View[] interfaceViews = new View[interfaces.length];
        for(int i = 0; i < interfaces.length; i++)
            interfaceViews[i] =  getView(interfaces[i]);

        return interfaceViews;
    }

    public final boolean isInterface()
    {
        return cl.isInterface();
    }
    
    public final boolean isGeneric()
    {
        return false;
    }
    
    /**
     * Returns all the formal type parameters.
     * 
     * @return Type parameters. Empty array if none exist.
     */
    public  Object[] getTypeParams() {
        return null;
    }
    

    /**
     * Return views of all methods of this class (including inherited ones).
     * Walk superclasses + interfaces for methods. Method definitions higher
     * up in the inheritance hierarchy are first in the array, with the latest 
     * redefinition last.
     */
    public synchronized MethodView[] getAllMethods()
    {
        if(allMethods == null) {
            HashMap<String,MemberElement> map = new HashMap<String,MemberElement>();
            getAllMethods(map, 0);
            
            List<MemberElement> methods = new ArrayList<MemberElement>(map.values());
            Collections.sort(methods, new ElementComparer());

            int numMethods = methods.size();
            allMethods = new MethodView[numMethods];
            for(int i = 0; i < numMethods; i++) {
                MemberElement elem = (MemberElement)methods.get(i);
                allMethods[i] = (MethodView)elem.member;
            }
        }

        return allMethods;
    }

    /**
     ** Walk superclasses + interfaces for fields.
     ** All fields are inherited (+ overridden) from everywhere.
     **/
    public Object[] getAllFields()
    {
        return null;
    }

    /**
     ** (Attempt at an) efficient implementation of getAllMethods + getAllFields
     ** The old version had shocking performance - this one uses a HashMap
     ** to notice the conflicts
     **/

    class MemberElement
    {
        int index;
        MemberView member;

        MemberElement(int index, MemberView member)
        {
            this.index = index;
            this.member = member;
        }
    }

    class ElementComparer implements Comparator<MemberElement>
    {
        /** Return { -1, 0, 1 } to represent <a> { <, ==, > } <b> **/
        public final int compare(MemberElement a, MemberElement b)
        {
            int cmp = a.index - b.index;

            return (cmp < 0) ? -1 : ((cmp > 0) ? 1 : 0);
        }
    }

    /**
     * Helper method to get all methods from the class represented by this
     * view and all its superclasses. If all methods have already been cached
     * (in "allMethods"), simply returns the cached list.
     * 
     * @param h        The hashmap into which to put all the methods
     * @param methnum  The number of methods presently in the map
     * @return         The number of methods in the map at completion
     */
    protected int getAllMethods(HashMap<String,MemberElement> h, int methnum)
    {
        if(allMethods != null) {
            // carefully copy from allMethods into h
            methnum = addMembers(h, allMethods, methnum);
            return methnum;
        }

        // otherwise, do the real work
        // carefully copy local methods into v

        View sView = getSuper();
        if(sView != null)
            methnum = sView.getAllMethods(h, methnum);

        if(isInterface()) {
            View[] ifaces = getInterfaces();
            for(int i = 0; i < ifaces.length; i++)
                methnum = ifaces[i].getAllMethods(h, methnum);
        }

        methnum = addMembers(h, getDeclaredMethods(), methnum);
        return methnum;
    }

    protected int getAllFields(HashMap<String,MemberElement> h, int fieldnum)
    {
        return 0;
    }

    private int addMembers(HashMap<String,MemberElement> h, MemberView[] members, int num)
    {
        for(int i = members.length - 1; i >= 0; i--) {
            h.put(members[i].toString(), new MemberElement(num++, members[i]));
        }

        return num;
    }

    public MethodView[] getDeclaredMethods()
    {
        if(methods == null) {
            int count = 0;
            try {
                Method[] cl_methods = cl.getDeclaredMethods();
                
                for(int i = 0; i < cl_methods.length; i++) {
                    if (!cl_methods[i].isSynthetic()) {
                        count++;
                    }
                }
                methods = new MethodView[count];
                
                count = 0;
                for(int i = 0; i < cl_methods.length; i++) {
                    if (!cl_methods[i].isSynthetic()) {
                        try {
                            methods[count] = new MethodView(this, cl_methods[i]);
                        }
                        catch (Throwable t) {
                            t.printStackTrace();
                            if (t instanceof ClassNotFoundException) {
                                throw (ClassNotFoundException) t;
                            }
                        }
                        count++;
                    }
                }
            }
            catch (LinkageError le) {
                // getDeclaredMethods can cause attempts for other classes to be loaded.
                // This in turn can cause a LinkageError variant to be thrown. (For
                // instance, NoClassDefFoundError).
                methods = new MethodView[0];
            }
            catch (ClassNotFoundException cnfe) {
                methods = new MethodView[0];
            }
        }

        return methods;
    }

    public Object[] getDeclaredFields()
    {
        return null;
    }

    public ConstructorView[] getConstructors()
    {
        if(constructors == null)
        {
            try {
                Constructor<?>[] cl_constrs = cl.getDeclaredConstructors();
                constructors = new ConstructorView[cl_constrs.length];
                
                for(int i = 0; i < constructors.length; i++)
                    constructors[i] = new ConstructorView(this, cl_constrs[i]);
            }
            catch (LinkageError le) {
                // Class.getDeclaredConstructors() can throw various linkage errors
                return new ConstructorView[0];
            }
        }

        return constructors;
    }

    public Object getComment()
    {
        return null;
    }

    public void setComment(Object comment)
    {
        
    }

    boolean comments_loaded = false;
    protected void loadComments()
    {
        
    }

    protected void loadClassComments(View curview, Map<String,MemberView> table)
    {
        
    }

    public String getTypeName()
    {
        return getTypeName(cl);
    }

    static String getTypeName(Class<?> type)
    {
        if(type.isArray())
            {
                try {
                    Class<?> primtype = type;
                    int dimensions = 0;
                    while(primtype.isArray())
                        {
                            dimensions++;
                            primtype = primtype.getComponentType();
                        }
                    StringBuffer sb = new StringBuffer();
                    sb.append(JavaNames.stripPrefix(primtype.getName()));
                    for (int i = 0; i < dimensions; i++)
                        sb.append("[]");
                    return sb.toString();
                } catch (Throwable e) {
                    // ignore it
                }
            }
        return JavaNames.stripPrefix(type.getName());
    }
}
