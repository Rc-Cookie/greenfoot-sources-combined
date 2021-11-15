/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2019  Michael Kolling and John Rosenberg

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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import bluej.Config;

import bluej.Boot;
import bluej.pkgmgr.Project;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class which manages scanning the classpath for available imports.
 */
@SuppressWarnings("unused")
public class ImportScanner
{
    // A lock item :
    private final Object monitor = new Object();
    // Root package with "" as ident.
    private CompletableFuture<RootPackageInfo> root;
    // The Project which we are scanning for:
    private final Project project;

    public ImportScanner(Project project)
    {
        this.project = project;
    }

    /**
     * For each package that we scan, we hold one PackgeInfo with details on the items
     * in that package, and links to any subpackages.  Thus the root of this tree
     * is a single PackageInfo representing the unnamed package (held in this.root).
     */
    private class PackageInfo
    {
        // Value can be null if details not loaded yet
        public final HashMap<String, ?> types = new HashMap<>();
        public final HashMap<String, PackageInfo> subPackages = new HashMap<>();
    
        // Records a class with the given name (scoped relative to this package).
        // So first we call addClass({"java","lang"},"String") on the root package, then
        // addClass({"lang"}, "String"} on the java package, then
        // addClass({}, "String)" on the java.lang package.
        protected void addClass(Iterator<String> packageIdents, String name)
        {
            
        }

        /**
         * Gets the type for the given name from this package, either using cached copy
         * or by calculating it on demand.
         * 
         * @param prefix The package name, ending in ".", e.g. "java.lang."
         * @param name The unqualified type name, e.g. "String".
         */
        @OnThread(Tag.Worker)
        private Object getType(String prefix, String name, Object javadocResolver)
        {
            return null;
        }

        /**
         * Gets types arising from a given import directive in the source code.
         * 
         * @param prefix The prefix of this package, ending in ".".  E.g. for the java
         *               package, we would be passed "java."
         * @param idents The next in the sequence of identifiers.  E.g. if we are the java package
         *               we might be passed {"lang", "String"}.  The final item may be an asterisk,
         *               e.g. {"lang", "*"}, in which case we return all types.  Otherwise we will
         *               return an empty list (if the type is not found), or a singleton list.
         * @return The 
         */
        @OnThread(Tag.Worker)
        public List<?> getImportedTypes(String prefix, Iterator<String> idents, Object javadocResolver)
        {
            return null;
        }

        public void addTypes(PackageInfo from)
        {
            
        }
    }
    
    // PackageInfo, but for the root type.
    private class RootPackageInfo extends PackageInfo
    {

    }
    
    @OnThread(Tag.Any)
    private CompletableFuture<? extends PackageInfo> getRoot()
    {
        synchronized (monitor)
        {
            // Already started calculating:
            if (root != null)
            {
                return root;
            }
            else
            {
                // Start calculating:
                root = new CompletableFuture<>();
                // We don't use runBackground because we don't want to end up
                // behind other callers of getRoot in the queue (this can
                // cause a deadlock because there are no background threads
                // available, as they are all blocked waiting for this
                // future to complete):
                new Thread() { public void run()
                {
                    RootPackageInfo rootPkg = findAllTypes();
                    try
                    {
                        loadCachedImports(rootPkg);
                    }
                    finally
                    {
                        root.complete(rootPkg);
                    }
                }}.start();
                return root;
            }
        }
    }

    /**
     * Given an import source (e.g. "java.lang.String", "java.util.*"), finds all the
     * types that will be imported.
     * 
     * If the one-time on-load import scanning has not finished yet, this method will
     * wait until it has.  Hence you should call it from a worker thread, not from a 
     * GUI thread where it could block the GUI for a long time.
     */
    @OnThread(Tag.Worker)
    public List<?> getImportedTypes(String importSrc)
    {
        return null;
    }

    /**
     * Gets a list of ClassGraph items which can be used to find available classes.
     * 
     * Because of the way ClassGraph works, one item is not enough for all classes;
     * we use one for system classes and one for user classes.
     */
    @OnThread(Tag.Worker)
    private List<?> getClassloaderConfig()
    {
        return null;
    }

    /**
     * Gets a package-tree structure which includes all packages and class-names
     * on the current class-path (by scanning all JARs and class-files on the path).
     *
     * @return A package-tree structure with all class names present, but not any further
     * details about the classes.
     */
    @OnThread(Tag.Worker)
    private RootPackageInfo findAllTypes()
    {
        return null;
    }

    /**
     * Starts scanning for available importable types from the classpath.
     * Will operate in a background thread.
     */
    public void startScanning()
    {
        // This will make sure the future has started:
        getRoot();
    }

    /**
     * Saves all java.** type information to a cache
     */
    public void saveCachedImports()
    {
        
    }

    /** Version of the currently running software */
    private static String getVersion()
    {
        return Config.isGreenfoot() ? Boot.GREENFOOT_VERSION : Boot.BLUEJ_VERSION;
    }

    /** Java home directory */
    private static String getJavaHome()
    {
        return Boot.getInstance().getJavaHome().getAbsolutePath();
    }

    /** Import cache path to save to/load from */
    private static File getImportCachePath()
    {
        return new File(Config.getUserConfigDir(), "import-cache.xml");
    }

    /**
     * Loads cached (java.**) imports into the given root package, if possible.
     */
    public void loadCachedImports(PackageInfo rootPkg)
    {
        
    }

    /**
     * Save the given PackageInfo item (with package name) to XML
     */
    private static Object toXML(PackageInfo pkg, String name)
    {
        return null;
    }
}
