/*
 * PackageInstallContext.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class PackageInstallContext extends JavaScriptObject
{
   protected PackageInstallContext()
   {
   }
   
   public final native boolean isCRANMirrorConfigured() /*-{
      return this.cranMirrorConfigured[0];
   }-*/;

   public final native String getDefaultLibraryPath() /*-{
      return this.defaultLibraryPath[0];
   }-*/;

   public final native boolean isDefaultLibraryWriteable() /*-{
      return this.defaultLibraryWriteable[0];
   }-*/;
   
   public final native JsArrayString getWriteableLibraryPaths() /*-{
      return this.writeableLibraryPaths;
   }-*/;
   
   public final native String getDefaultUserLibraryPath() /*-{
      return this.defaultUserLibraryPath[0];
   }-*/;
}