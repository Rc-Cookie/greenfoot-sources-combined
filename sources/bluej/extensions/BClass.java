/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2013,2014,2015,2016  Michael Kolling and John Rosenberg
 
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
package bluej.extensions;

import java.io.File;

/**
 * A wrapper for a class. This is used to represent both classes which have a representation
 * within the BlueJ project, and those that don't.
 * 
 * <p>From an instance of this class you can create BlueJ objects and call their methods.
 * Behaviour is similar to the Java reflection API.
 *
 * @author Damiano Bolla, University of Kent at Canterbury, 2002,2003,2004
 */
public class BClass
{

    /**
     * Constructor for the BClass.
     * It is duty of the caller to guarantee that it is a reasonable classId
     */
    BClass(Object thisClassId)
    {
        
    }
    
    /**
     * Get a BClass for some class identifier. To be used for classes which don't have a
     * representation (ClassTarget) in BlueJ.
     */
    synchronized static BClass getBClass(Object classId)
    {
        return null;
    }
    

    /**
     * Notification that the name of the class has changed.
     * @param newName  The new class name, fully qualified.
     */
    void nameChanged(String newName)
    {
        
    }

    /**
     * Returns the name of this BClass.
     * 
     * @return the fully qualified name of the wrapped BlueJ class.
     */
    public final String getName()
    {
        return null;
    }

    /**
     * Removes this class from BlueJ, including the underlying files.
     *
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @throws  ClassNotFoundException    if the class has been deleted by the user, or if the class does
     *                                    not otherwise have a representation within the project.
     */
    public void remove()
             throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        
    }

    /**
     * Converts a Stride class to Java, by removing the Stride file and retaining the generated Java.
     *
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public void convertStrideToJava()
            throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        
    }

    public void convertJavaToStride()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        
    }


    /**
     * Returns the Java class being wrapped by this BClass.
     * Use this method when you need more information about the class than
     * is provided by the BClass interface. E.g.:
     * 
     * <ul>
     * <li>What is the real class being hidden?
     * <li>Is it an array?
     * <li>What is the type of the array element?
     * </ul>
     *
     * <p>Note that this is for information only. If you want to interact with BlueJ you must
     * use the methods provided in BClass.
     *
     * @return                           The javaClass value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Class<?> getJavaClass()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }


    /**
     * Returns the package this class belongs to.
     * Similar to reflection API.
     *
     * @return                            The package value
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public Object getPackage() throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }


    /**
     * Returns a proxy object that provide an interface to the editor for this BClass.
     * If an editor already exists, a proxy for it is returned. Otherwise, an editor is created but not made visible.
     *
     * @return                            The proxy editor object or null if it cannot be created
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public Object getEditor() throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }
    
    /**
     * Finds out whether this class has source code available, and whether it's Java or Stride
     */
    public SourceType getSourceType() throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }


    /**
     * Checks to see if this class has been compiled.
     *
     * @return                            true if it is compiled false othervise.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public boolean isCompiled()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        return false;
    }


    /**
     * Compile this class, and any dependents.
     * 
     * <p>After the compilation has finished the method isCompiled() can be used to determined the class
     * status.
     * 
     * <p>A single CompileEvent will be generated with all dependent files listed.
     * 
     * <p>A call to this method is equivalent to: <code>compile(waitCompileEnd, false)</code>.
     *
     * @param  waitCompileEnd                   <code>true</code> waits for the compilation to be finished.
     * @throws  ProjectNotOpenException         if the project to which this class belongs has been closed.
     * @throws  PackageNotFoundException        if the package to which this class belongs has been deleted.
     * @throws  CompilationNotStartedException  if BlueJ is currently executing Java code.
     */
    public void compile(boolean waitCompileEnd)
             throws ProjectNotOpenException, PackageNotFoundException
    {
        compile(waitCompileEnd, false);
    }

    /**
     * Compile this class, and any dependents, optionally without showing compilation errors to the user.
     * 
     * <p>After the compilation has finished the method isCompiled() can be used to determined the class
     * status.
     * 
     * <p>A single CompileEvent with all dependent files listed will be generated.
     *
     * @param  waitCompileEnd                   <code>true</code> waits for the compilation to be finished.
     * @param  forceQuiet                       if true, compilation errors will not be shown/highlighted to the user.
     * @throws  ProjectNotOpenException         if the project to which this class belongs has been closed.
     * @throws  PackageNotFoundException        if the package to which this class belongs has been deleted.
     * @throws  CompilationNotStartedException  if BlueJ is currently executing Java code.
     */
    public void compile(boolean waitCompileEnd, boolean forceQuiet)
             throws ProjectNotOpenException, PackageNotFoundException
    {
        
    }


    /**
     * Returns the superclass of this class.
     * 
     * <p>Similar to reflection API.
     * 
     * <p>If this class represents either the Object class, an interface,
     * a primitive type, or void, then null is returned.
     *
     * @return                            The superclass value
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @throws  ClassNotFoundException    if the class has been deleted by the user.
     */
    public Object getSuperclass()
             throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException
    {
        return null;
    }


    /**
     * Returns all the constructors of this class.
     * Similar to reflection API.
     *
     * @return                           The constructors value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Object[] getConstructors()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }


    /**
     * Returns the constructor for this class which has the given signature.
     * Similar to reflection API.
     *
     * @param  signature                 the signature of the required constructor.
     * @return                           the requested constructor of this class, or null if
     * the class has not been compiled or the constructor cannot be found.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Object getConstructor(Class<?>[] signature)
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }


    /**
     * Returns the declared methods of this class.
     * Similar to reflection API.
     *
     * @return                           The declaredMethods value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Object[] getDeclaredMethods()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }


    /**
     * Returns the declared method of this class which has the given signature.
     * Similar to reflection API.
     *
     * @param  methodName                The name of the method.
     * @param  params                    The parameters of the method. Pass a zero length array if the method takes no arguments. 
     * @return                           The declaredMethod value or <code>null</code> if the method is not found.
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Object getDeclaredMethod(String methodName, Class<?>[] params)
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }

    
    /**
     * Returns all methods of this class, those declared and those inherited from all ancestors. 
     * Similar to reflection API, except that all methods, declared and inherited, are returned, and not only the public ones.
     * That is, it returns all public, private, protected, and package-access methods, inherited or declared.
     * The elements in the array returned are not sorted and are not in any particular order.
     *
     * @return                           The Methods value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Object[] getMethods()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }

    
    /**
     * Returns the method of this class with the given signature.
     * Similar to reflection API, except that all methods, declared and inherited, are searched, and not only the public ones.
     * That is, it searches all public, private, protected, and package-access methods, declared or inherited from all ancestors.
     * If the searched method has been redefined, the returned method is chosen arbitrarily from the list of inherited and declared methods.
     *
     * @param  methodName                The name of the method
     * @param  params                    The parameters of the method. Pass a zero length array if the method takes no arguments. 
     * @return                           The Method value or <code>null</code> if the method is not found.
     * @throws  ProjectNotOpenException  If the project to which this class belongs has been closed by the user
     * @throws  ClassNotFoundException   If the class has been deleted by the user
     */
    public Object getMethod(String methodName, Class<?>[] params)
    {
        return null;
    }


    /**
     * Returns all the fields of this class.
     * Similar to reflection API.
     *
     * @return                           The fields value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Object[] getFields()
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }


    /**
     * Returns the field of this class which has the given name.
     * Similar to Reflection API.
     *
     * @param  fieldName                 Description of the Parameter
     * @return                           The field value
     * @throws  ProjectNotOpenException  if the project to which this class belongs has been closed by the user.
     * @throws  ClassNotFoundException   if the class has been deleted by the user.
     */
    public Object getField(String fieldName)
             throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }


    /**
     * Returns the class target of this class. May return <code>null</code> if
     * the class target is no longer valid.
     * 
     * @return The class target of this class or <code>null</code> if there is
     *         no such class target.
     * @throws ProjectNotOpenException
     *             if the project to which this class belongs has been closed by
     *             the user.
     * @throws PackageNotFoundException
     *             if the package to which this class belongs has been deleted
     *             by the user.
     */
    public BClassTarget getClassTarget() throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }

    /**
     * Returns this class's .class file (or null, if the class no longer exists in the project).
     *
     * @return                            the class .class file.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public File getClassFile()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }


    /**
     * Returns this class's .java file.
     * If the file is currently being edited, calling this method will cause it to be saved.
     *
     * @return                            the class .java file.
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     */
    public File getJavaFile()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        return null;
    }


    /**
     * Signal to BlueJ that an extension is about to begin changing the source file of this class.
     * The file containing the source for this class can be found using getJavaFile();
     * If the file is currently being edited it will be saved and the editor will be set read-only.
     *
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @deprecated As of BlueJ 2.0, replaced by {@link Editor#setReadOnly(boolean readOnly)}
     */
    @Deprecated
    public void beginChangeSource()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        
    }


    /**
     * Signal to BlueJ that an extension has finished changing the source file of this class.
     * If the file is currently being edited, this will cause it to be re-loaded and the editor to be set read/write.
     *
     * @throws  ProjectNotOpenException   if the project to which this class belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this class belongs has been deleted by the user.
     * @deprecated As of BlueJ 2.0, replaced by {@link Editor#setReadOnly(boolean readOnly)}
     */
    @Deprecated
    public void endChangeSource()
             throws ProjectNotOpenException, PackageNotFoundException
    {
        
    }
}
