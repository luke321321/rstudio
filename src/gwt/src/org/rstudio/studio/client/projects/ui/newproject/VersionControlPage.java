/*
 * VersionControlPage.java
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.VcsCloneOptions;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class VersionControlPage extends NewProjectWizardPage
{
   public VersionControlPage()
   {
      super("Version Control", 
            "Checkout a project from a version control repository",
            "Create Project from Version Control",
            NewProjectResources.INSTANCE.projectFromRepositoryIcon(),
            NewProjectResources.INSTANCE.projectFromRepositoryIconLarge());
   }


   @Override
   protected void onAddWidgets()
   { 
      NewProjectResources.Styles styles = NewProjectResources.INSTANCE.styles();
      
      DockPanel sourcePanel = new DockPanel();
      sourcePanel.addStyleName(styles.wizardTextEntry());
      
      VerticalPanel vcsPanel = new VerticalPanel();
      Label vcsLabel = new Label("VCS:");
      vcsLabel.addStyleName(styles.wizardTextEntryLabel());
      vcsPanel.add(vcsLabel);
      String[] availableVcs = getSessionInfo().getAvailableVCS();
      vcsSelector_ = new ListBox();
      vcsSelector_.addStyleName(styles.wizardVcsSelector());
      for (int i=0; i<availableVcs.length; i++)
         vcsSelector_.addItem(availableVcs[i]);
      vcsSelector_.setSelectedIndex(0);
      vcsPanel.add(vcsSelector_);
      sourcePanel.add(vcsPanel, DockPanel.WEST);
      
      VerticalPanel urlPanel = new VerticalPanel();
      urlPanel.setWidth("100%");
      Label urlLabel = new Label("Repository URL:");
      urlLabel.addStyleName(styles.wizardTextEntryLabel());
      urlPanel.add(urlLabel);
      txtRepoUrl_ = new TextBox();
      txtRepoUrl_.setWidth("100%");
      urlPanel.add(txtRepoUrl_);
      sourcePanel.add(urlPanel, DockPanel.CENTER);
      
      
      addWidget(sourcePanel);
      
      addSpacer();
     
      existingRepoDestDir_ = new DirectoryChooserTextBox(
            "Clone project into subdirectory of:", txtRepoUrl_);
      addWidget(existingRepoDestDir_);
   }
   
   @Override 
   protected void initialize(FileSystemItem defaultNewProjectLocation)
   {
      super.initialize(defaultNewProjectLocation);
      existingRepoDestDir_.setText(defaultNewProjectLocation.getPath());
   }

   @Override
   protected NewProjectResult collectInput()
   {
      String url = txtRepoUrl_.getText().trim();
      String dir = existingRepoDestDir_.getText().trim();
      if (url.length() > 0 && dir.length() > 0)
      {
         String repo = guessGitRepoDir(url);
         if (repo.length() == 0)
            return null;

         String repoDir = FileSystemItem.createDir(dir).completePath(repo);
         String projFile = projFileFromDir(repoDir);
         
         VcsCloneOptions vcsOptions = VcsCloneOptions.create("git", 
                                                             url, 
                                                             repo, 
                                                             dir);
         
         return new NewProjectResult(projFile, dir, vcsOptions);
      }
      else
      {
         return null;
      }
   }

   @Override
   protected boolean validate(NewProjectResult input)
   {
      if (input == null)
      {
         if (txtRepoUrl_.getText().trim().length() == 0
               || existingRepoDestDir_.getText().trim().length() == 0)
         {
            globalDisplay_.showMessage(
                  MessageDialog.WARNING,
                  "Error",
                  "You must specify a git repository URL and existing " +
                  "directory to create the new project within.",
                  txtRepoUrl_);
         }
         else if (guessGitRepoDir(txtRepoUrl_.getText().trim()).length() == 0)
         {
            globalDisplay_.showMessage(
                  MessageDialog.WARNING,
                  "Error",
                  "Could not guess the git repository directory name from " +
                  "the git repository URL. Please clone the repository " +
                  "manually, and then create a new RStudio project within " +
                  "it.");
         }
         
         return false;
      }
      else
      {
         return true;
      }

   }


   @Override
   public void focus()
   {
      txtRepoUrl_.setFocus(true);
      
   }
   
   private static String guessGitRepoDir(String url)
   {
      /*
       * Strip trailing spaces, slashes and /.git
       */
      while (url.endsWith("/") || url.endsWith(" ") || url.endsWith("\t"))
         url = url.substring(0, url.length() - 1);
      if (url.endsWith("/.git"))
      {
         url = url.substring(0, url.length() - 5);
         while (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);
      }

      /*
       * Find last component, but be prepared that repo could have
       * the form  "remote.example.com:foo.git", i.e. no slash
       * in the directory part.
       */
      url = url.replaceFirst(".*[:/]", ""); // greedy

      /*
       * Strip .{bundle,git}.
       */
      url = url.replaceAll(".(bundle|git)$", "");
      url = url.replaceAll("[\u0000-\u0020]+", " ");
      url = url.trim();
      return url;
   }
  
   private ListBox vcsSelector_;
   private TextBox txtRepoUrl_;
   private TextBox txtDirName_;
   private DirectoryChooserTextBox existingRepoDestDir_;

  

}
