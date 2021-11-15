/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014,2015,2016,2017,2018  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target;

import bluej.editor.Editor;
import bluej.pkgmgr.Package;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Properties;

/**
 * A parent package
 *
 * @author  Andrew Patterson
 */
public class ReadmeTarget
{
    public static final String README_ID = "@README";

    // Images
    @OnThread(Tag.FXPlatform)
    private static Image readmeImage;
    @OnThread(Tag.FXPlatform)
    private static Image selectedReadmeImage;
    @OnThread(Tag.FXPlatform)
    private ImageView imageView;

    public ReadmeTarget(Package pkg)
    {
        
    }

    public void load(Properties props, String prefix) throws NumberFormatException
    {
    }

    /*
     * @return the name of the (text) file this target corresponds to.
     */
    public File getSourceFile()
    {
        return null;
    }

    @OnThread(Tag.FX)
    public boolean isResizable()
    {
        return false;
    }
    
    /*
     * Although we do save some information (the editor position) about a Readme
     * this is not done via the usual target save mechanism. If the normal save
     * mechanism was used, the readme target would appear as a normal target.
     * This would result in not being able to open a project saved in a newer
     * BlueJ version with an older BlueJ version.
     */
    @OnThread(Tag.Any)
    public boolean isSaveable()
    {
        return false;
    }

    public Editor getEditor()
    {
        return null;
    }


    private void openEditor(boolean openInNewWindow)
    {
        
    }

    /*
     * Called when a package icon in a GraphEditor is double clicked.
     * Creates a new PkgFrame when a package is drilled down on.
     */
    @OnThread(Tag.FXPlatform)
    public void doubleClick(boolean openInNewWindow)
    {
        openEditor(openInNewWindow);
    }

    /*
     * Post the context menu for this target.
     */
    @OnThread(Tag.FXPlatform)
    public void popupMenu(int x, int y, Object editor)
    {
        
    }
    
    @OnThread(Tag.FXPlatform)
    private ContextMenu createMenu()
    {
        return null;
    }

    public void remove()
    {
        // The user is not permitted to remove the readmefile
    }

    public @OnThread(Tag.FXPlatform) void setSelected(boolean selected)
    {
        
    }
}
