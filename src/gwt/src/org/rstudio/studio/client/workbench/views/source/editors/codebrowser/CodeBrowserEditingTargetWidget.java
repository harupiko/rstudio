/*
 * CodeBrowserEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

public class CodeBrowserEditingTargetWidget extends Composite
   implements CodeBrowserEditingTarget.Display
{
   public CodeBrowserEditingTargetWidget(Commands commands,
                                         DocDisplay docDisplay)
   {
      commands_ = commands;
      
      docDisplay_ = docDisplay;
      
      panel_ = new PanelWithToolbar(createToolbar(),
                                    docDisplay_.asWidget());
      panel_.setSize("100%", "100%");
      
      docDisplay_.setReadOnly(true);
      docDisplay_.setFileType(FileTypeRegistry.R, true); 
      
      initWidget(panel_);

   }
   
   @Override
   public Widget asWidget()
   {
      return this;
   }
   
   
   @Override
   public void adaptToFileType(TextFileType fileType)
   {
      docDisplay_.setFileType(fileType, true); 
   }


   @Override
   public void setFontSize(double size)
   {
      docDisplay_.setFontSize(size);
   }
   
   @Override
   public void onActivate()
   {
      docDisplay_.onActivate();
   }
   
   public void showFunction(SearchPathFunctionDefinition functionDef)
   {
      docDisplay_.setCode(StringUtil.notNull(functionDef.getCode()), false);   
   }
   
   private Toolbar createToolbar()
   {
      Toolbar toolbar = new EditingTargetToolbar(commands_);
      toolbar.addLeftWidget(commands_.printSourceDoc().createToolbarButton());
  
      return toolbar;
   }



   private final PanelWithToolbar panel_;
   private final Commands commands_;
   private final DocDisplay docDisplay_;
   
  
  
}