/*
 * FilesList.java
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
package org.rstudio.studio.client.workbench.views.files.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ColumnSortInfo;
import org.rstudio.core.client.cellview.LinkColumn;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.views.files.Files;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.ColumnSortEvent.Handler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;

public class FilesList extends Composite
{
   public FilesList(final Files.Display.Observer observer,
                    final FileTypeRegistry fileTypeRegistry)
   {
      observer_ = observer;
      
      // create data provider and sort handler
      dataProvider_ = new ListDataProvider<FileSystemItem>();
      sortHandler_ = new ColumnSortEvent.ListHandler<FileSystemItem>(
                                                      dataProvider_.getList());
      
      // create cell table
      filesCellTable_ = new CellTable<FileSystemItem>(
                                          15,
                                          FilesListCellTableResources.INSTANCE,
                                          KEY_PROVIDER);
      selectionModel_ = new MultiSelectionModel<FileSystemItem>(KEY_PROVIDER);
      filesCellTable_.setSelectionModel(
         selectionModel_, 
         DefaultSelectionEventManager.<FileSystemItem> createCheckboxManager());
      filesCellTable_.setWidth("100%", false);
      
      // hook-up data provider 
      dataProvider_.addDataDisplay(filesCellTable_);
      
      // add columns
      addSelectionColumn();
      addIconColumn(fileTypeRegistry);
      nameColumn_ = addNameColumn();
      sizeColumn_ = addSizeColumn();
      modifiedColumn_ = addModifiedColumn();
      
      // initialize sorting
      addColumnSortHandler();
      
      // enclose in scroll panel
      scrollPanel_ = new ScrollPanel();
      initWidget(scrollPanel_);
      scrollPanel_.setWidget(filesCellTable_);   
   }
   
   private Column<FileSystemItem, Boolean> addSelectionColumn()
   {
      Column<FileSystemItem, Boolean> checkColumn = 
         new Column<FileSystemItem, Boolean>(new CheckboxCell(true, false) {
            @Override
            public void render(Context context, Boolean value, SafeHtmlBuilder sb) 
            {
               // don't render the check box if its for the parent path
               if (parentPath_ == null || context.getIndex() > 0)
                  super.render(context, value, sb);
            }
         }) 
         {
            @Override
            public Boolean getValue(FileSystemItem item)
            {
               return selectionModel_.isSelected(item);
            }
            
            
         };
      checkColumn.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      filesCellTable_.addColumn(checkColumn); 
      filesCellTable_.setColumnWidth(checkColumn, 20, Unit.PX);
      
      return checkColumn;
   }
  
   
   private Column<FileSystemItem, ImageResource> addIconColumn(
                              final FileTypeRegistry fileTypeRegistry)
   {
      Column<FileSystemItem, ImageResource> iconColumn = 
         new Column<FileSystemItem, ImageResource>(new ImageResourceCell()) {

            @Override
            public ImageResource getValue(FileSystemItem object)
            {
               if (object == parentPath_)
                  return FileIconResources.INSTANCE.iconUpFolder();
               else
                  return fileTypeRegistry.getIconForFile(object);
            }
         };
      iconColumn.setSortable(true);
      filesCellTable_.addColumn(iconColumn, 
                                SafeHtmlUtils.fromSafeConstant("<br/>"));
      filesCellTable_.setColumnWidth(iconColumn, 20, Unit.PX);
    
      sortHandler_.setComparator(iconColumn, new FilesListComparator() {
         @Override
         public int doCompare(FileSystemItem arg0, FileSystemItem arg1)
         {
            if (arg0.isDirectory() && !arg1.isDirectory())
               return 1;
            else if (arg1.isDirectory() && !arg0.isDirectory())
               return -1;
            else
               return arg0.getExtension().compareTo(arg1.getExtension());
         }
      });
      
      return iconColumn;
   }

   private LinkColumn<FileSystemItem> addNameColumn()
   {
      LinkColumn<FileSystemItem> nameColumn = new LinkColumn<FileSystemItem>(
         dataProvider_, 
         new OperationWithInput<FileSystemItem>() 
         {
            public void execute(FileSystemItem input)
            {
               observer_.onFileNavigation(input);  
            }   
         }) 
         {
            @Override
            public String getValue(FileSystemItem item)
            {
               if (item == parentPath_)
                  return "..";
               else
                  return item.getName();
            }
         };
      nameColumn.setSortable(true);
      filesCellTable_.addColumn(nameColumn, "Name");
      
      sortHandler_.setComparator(nameColumn, new FilesListComparator() {
         @Override
         public int doCompare(FileSystemItem arg0, FileSystemItem arg1)
         {
            return arg0.getName().compareToIgnoreCase(arg1.getName());
         }
      });
      
      return nameColumn;
   }
   
   
   private TextColumn<FileSystemItem>  addSizeColumn()
   {
      TextColumn<FileSystemItem> sizeColumn = new TextColumn<FileSystemItem>() {
         public String getValue(FileSystemItem file)
         {
            if (!file.isDirectory())
               return StringUtil.formatFileSize(file.getLength());
            else
               return new String();
         } 
      };  
      sizeColumn.setSortable(true);
      filesCellTable_.addColumn(sizeColumn, "Size");
      filesCellTable_.setColumnWidth(sizeColumn, 80, Unit.PX);
      
      sortHandler_.setComparator(sizeColumn, new FoldersOnBottomComparator() {
         @Override
         public int doItemCompare(FileSystemItem arg0, FileSystemItem arg1)
         {
            return new Long(arg0.getLength()).compareTo(
                                             new Long(arg1.getLength()));
         }
      });
      
      return sizeColumn;
   }

   
   private TextColumn<FileSystemItem> addModifiedColumn()
   {
      TextColumn<FileSystemItem> modColumn = new TextColumn<FileSystemItem>() {
         public String getValue(FileSystemItem file)
         {
            if (!file.isDirectory())
               return StringUtil.formatDate(file.getLastModified());
            else
               return new String();
         } 
      };  
      modColumn.setSortable(true);
      filesCellTable_.addColumn(modColumn, "Modified");
      filesCellTable_.setColumnWidth(modColumn, 160, Unit.PX); 
      
      sortHandler_.setComparator(modColumn, new FoldersOnBottomComparator() {
         @Override
         public int doItemCompare(FileSystemItem arg0, FileSystemItem arg1)
         {
            return arg0.getLastModified().compareTo(arg1.getLastModified());
         }
      });
      
      return modColumn;
   }
   
   private void addColumnSortHandler()
   {
      filesCellTable_.addColumnSortHandler(new Handler() {
         @Override
         public void onColumnSort(ColumnSortEvent event)
         {     
            ColumnSortList sortList = event.getColumnSortList();

            // insert the default initial sort order for size and modified
            if (!applyingProgrammaticSort_)
            {
               if (event.getColumn().equals(sizeColumn_) && 
                   forceSizeSortDescending)
               {
                  forceSizeSortDescending = false;
                  forceModifiedSortDescending = true;
                  sortList.insert(0, 
                                  new com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo(event.getColumn(), false));
               }
               else if (event.getColumn().equals(modifiedColumn_) && 
                        forceModifiedSortDescending)
               {
                  forceModifiedSortDescending = false;
                  forceSizeSortDescending = true;
                  sortList.insert(0, 
                                  new com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo(event.getColumn(), false));
               }
               else
               {
                  forceModifiedSortDescending = true;
                  forceSizeSortDescending = true;
               } 
            }
            
            // record sort order and fire event to observer
            JsArray<ColumnSortInfo> sortOrder = newSortOrderArray();
            for (int i=0; i<sortList.size(); i++)
            {
               // match the column index
               com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo sortInfo = sortList.get(i);
               Object column = sortInfo.getColumn();
               
               for (int c=0; c<filesCellTable_.getColumnCount(); c++)
               {
                  if (filesCellTable_.getColumn(c).equals(column))
                  { 
                     boolean ascending = sortInfo.isAscending();
                     sortOrder.push(ColumnSortInfo.create(c, ascending));
                     break;
                  }
               }
            }        
            observer_.onColumnSortOrderChanaged(sortOrder);
    
            // record active sort column ascending state
            activeSortColumnAscending_ = event.isSortAscending();
            
            // delegate the sort
            sortHandler_.onColumnSort(event);
         }
         
         private native final JsArray<ColumnSortInfo> newSortOrderArray()
         /*-{
            return [];
         }-*/;       
         private boolean forceSizeSortDescending = true;
         private boolean forceModifiedSortDescending = true;
      });
   }
   
  
  
   public void setColumnSortOrder(JsArray<ColumnSortInfo> sortOrder)
   {
      if (sortOrder != null)
      {
         ColumnSortInfo.setSortList(filesCellTable_, sortOrder);
      }
      else
      {
         ColumnSortList columnSortList = filesCellTable_.getColumnSortList();
         columnSortList.clear();
         columnSortList.push(nameColumn_);
      }
   }
   
   
   public void displayFiles(FileSystemItem containingPath, 
                            JsArray<FileSystemItem> files)
   {
      // clear the selection
      selectNone();
      
      // set containing path
      containingPath_ = containingPath;
      parentPath_ = containingPath_.getParentPath();
      
      // set page size (+1 for parent path)
      filesCellTable_.setPageSize(files.length() + 1);
      
      // get underlying list
      List<FileSystemItem> fileList = dataProvider_.getList();
      fileList.clear();
            
      // add entry for parent path if we have one
      if (parentPath_ != null)
         fileList.add(parentPath_);
      
      // add files to table
      for (int i=0; i<files.length(); i++)
         fileList.add(files.get(i));
           
      // apply sort list
      applyColumnSortList();
      
      // fire selection changed
      observer_.onFileSelectionChanged();
   }
   
   public void selectAll()
   {
      for (FileSystemItem item : dataProvider_.getList())
      {
         if (item != parentPath_)
            selectionModel_.setSelected(item, true);
      }
   }
   
   public void selectNone()
   {
      selectionModel_.clear();
   }
   
   
   public ArrayList<FileSystemItem> getSelectedFiles()
   {    
      // first make sure there are no leftover items in the selected set
      Set<FileSystemItem> selectedSet = selectionModel_.getSelectedSet();
      selectedSet.retainAll(dataProvider_.getList());
   
      return new ArrayList<FileSystemItem>(selectedSet);
   }
   
   public void updateWithAction(FileChange viewAction)
   {        
      final FileSystemItem file = viewAction.getFile();
      final List<FileSystemItem> files = getFiles();
      switch(viewAction.getType())
      {
      case FileChange.ADD:
         if (file.getParentPath().equalTo(containingPath_))
         {
            int row = rowForFile(file);
            if (row == -1)
            {
               files.add(file);
               filesCellTable_.setPageSize(files.size() + 1);
            }
            else
            {
               // since we eagerly perform renames at the client UI
               // layer then sometimes an "added" file is really just
               // a rename. in this case the file already exists due
               // to the eager rename in the client but still needs its
               // metadata updated
               files.set(row, file);
            }
         }
         break;
         
      case FileChange.MODIFIED:
         {
            int row = rowForFile(file);
            if (row != -1)
               files.set(row, file);
         }
         break;
 
      case FileChange.DELETE:
         {
            int row = rowForFile(file);
            if (row != -1)
            {
               files.remove(row);
               
               // if a file is deleted and then re-added within the same
               // event loop (as occurs when gedit saves a text file) the
               // table doesn't always update correctly (it has a duplicate
               // of the item deleted / re-added). the call to flush overcomes
               // this issue
               dataProvider_.flush();
            }
         }
         break;
      
      default:
         Debug.log("Unexpected file change type: " + viewAction.getType());
         
         break;
      }
   }
   
   public void renameFile(FileSystemItem from, FileSystemItem to)
   {
      int index = getFiles().indexOf(from);
      if (index != -1)
      {
         selectNone();
         getFiles().set(index, to);
      }
   }
   
   private List<FileSystemItem> getFiles()
   {
      return dataProvider_.getList();
   }
   
   private int rowForFile(FileSystemItem file)
   {
      List<FileSystemItem> files = getFiles();
      for (int i=0; i<files.size(); i++)
         if (files.get(i).equalTo(file))
            return i ;
      
      return -1;
   }
   
   private void applyColumnSortList()
   {
      applyingProgrammaticSort_ = true;
      ColumnSortEvent.fire(filesCellTable_, 
                           filesCellTable_.getColumnSortList());
      applyingProgrammaticSort_ = false;
   }
   
   private static final ProvidesKey<FileSystemItem> KEY_PROVIDER = 
      new ProvidesKey<FileSystemItem>() {
         @Override
         public Object getKey(FileSystemItem item)
         {
            return item.getPath();
         }
    };
    
    // comparator which ensures that the parent path is always on top
    private abstract class FilesListComparator implements Comparator<FileSystemItem>
    {     
       @Override
       public int compare(FileSystemItem arg0, FileSystemItem arg1)
       {
          int ascendingFactor = activeSortColumnAscending_ ? -1 : 1;
          
          if (arg0 == parentPath_)
             return 1 * ascendingFactor;
          else if (arg1 == parentPath_)
             return -1 * ascendingFactor;
          else
             return doCompare(arg0, arg1);
       }
       
       protected abstract int doCompare(FileSystemItem arg0, FileSystemItem arg1);    
    }
    
    private abstract class SeparateFoldersComparator extends FilesListComparator
    {
       public SeparateFoldersComparator(boolean foldersOnBottom)
       {
          if (foldersOnBottom)
             sortFactor_ = 1;
          else
             sortFactor_ = -1;
       }
       
       protected int doCompare(FileSystemItem arg0, FileSystemItem arg1)
       {
          int ascendingResult = activeSortColumnAscending_ ? 1 : -1;
          
          if (arg0.isDirectory() && !arg1.isDirectory())
             return ascendingResult * sortFactor_;
          else if (arg1.isDirectory() && !arg0.isDirectory())
             return -ascendingResult * sortFactor_;
          else
             return doItemCompare(arg0, arg1);
       }
       
       protected abstract int doItemCompare(FileSystemItem arg0, FileSystemItem arg1);    
       
       private final int sortFactor_ ;   
    }
    
    private abstract class FoldersOnBottomComparator extends SeparateFoldersComparator
    {
       public FoldersOnBottomComparator() 
       { 
          super(true); 
       }
    }
    
    @SuppressWarnings("unused")
    private abstract class FoldersOnTopComparator extends SeparateFoldersComparator
    {
       public FoldersOnTopComparator() 
       { 
          super(false); 
       }
    }
    
   
   private FileSystemItem containingPath_ = null;
   private FileSystemItem parentPath_ = null;
  
   private final CellTable<FileSystemItem> filesCellTable_; 
   private final LinkColumn<FileSystemItem> nameColumn_;
   private final TextColumn<FileSystemItem> sizeColumn_;
   private final TextColumn<FileSystemItem> modifiedColumn_;
   private boolean activeSortColumnAscending_ = true;
   private boolean applyingProgrammaticSort_ = false;
   
   
   private final MultiSelectionModel<FileSystemItem> selectionModel_;
   private final ListDataProvider<FileSystemItem> dataProvider_;
   private final ColumnSortEvent.ListHandler<FileSystemItem> sortHandler_;

   private final Files.Display.Observer observer_ ;
   private final ScrollPanel scrollPanel_ ;  
   
 
   
}
