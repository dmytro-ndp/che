/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.selenium.git;

import org.eclipse.che.selenium.core.project.ProjectTemplates;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.client.TestProjectServiceClient;
import org.eclipse.che.selenium.core.client.TestUserPreferencesServiceClient;
import org.eclipse.che.selenium.core.constant.TestGitConstants;
import org.eclipse.che.selenium.core.user.DefaultTestUser;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.AskDialog;
import org.eclipse.che.selenium.pageobject.Events;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.Menu;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.WarningDialog;
import org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.file.Paths;

/**
 * Created by aleksandr shmaraev
 * on 29.09.14.
 */
public class InitializeAndDeleteLocalRepositoryTest {
    private static final String PROJECT_NAME     = NameGenerator.generate("InitAndDelLocalRepo-", 4);
    private static final String PATH_FOR_EXPAND  = PROJECT_NAME + "/src/main/java/org/eclipse/qa/examples/";
    private static final String ASK_DIALOG_TEXT  = "Do you want to initialize the local repository " + PROJECT_NAME + "?";
    private static final String DELETE_REPO_TEXT = "Are you sure you want to delete " + PROJECT_NAME + "?";
    private static final String WARNING_TEXT     = "Initial commit is required to perform this operation.";
    private static final String COMMIT_MESSAGE   = "init";

    @Inject
    private TestWorkspace                               ws;
    @Inject
    private Ide                                         ide;
    @Inject
    private DefaultTestUser                             productUser;
    @Inject
    @Named("github.username")
    private String                                      gitHubUsername;
    @Inject
    private ProjectExplorer                             projectExplorer;
    @Inject
    private Menu                                        menu;
    @Inject
    private org.eclipse.che.selenium.pageobject.git.Git git;
    @Inject
    private Events                                      events;
    @Inject
    private Loader                                      loader;
    @Inject
    private AskDialog                                   askDialog;
    @Inject
    private WarningDialog                               warningDialog;
    @Inject
    private TestUserPreferencesServiceClient            testUserPreferencesServiceClient;
    @Inject
    private TestProjectServiceClient                    testProjectServiceClient;

    @BeforeClass
    public void prepare() throws Exception {
        testUserPreferencesServiceClient.addGitCommitter(gitHubUsername, productUser.getEmail());

        URL resource = getClass().getResource("/projects/default-spring-project");
        testProjectServiceClient.importProject(ws.getId(), Paths.get(resource.toURI()), PROJECT_NAME,
                                               ProjectTemplates.MAVEN_SPRING
                                              );
        ide.open(ws);
    }

    @Test
    public void initializeLocalRepositoryTest() {
        // Initialize git repository
        projectExplorer.waitProjectExplorer();
        projectExplorer.waitItem(PROJECT_NAME);
        projectExplorer.selectItem(PROJECT_NAME);
        menu.runCommand(TestMenuCommandsConstants.Git.GIT, TestMenuCommandsConstants.Git.INITIALIZE_REPOSITORY);
        askDialog.acceptDialogWithText(ASK_DIALOG_TEXT);
        loader.waitOnClosed();
        git.waitGitStatusBarWithMess(TestGitConstants.GIT_INITIALIZED_SUCCESS);
        events.clickProjectEventsTab();
        events.waitExpectedMessage(TestGitConstants.GIT_INITIALIZED_SUCCESS);
        projectExplorer.quickExpandWithJavaScript();
        projectExplorer.openItemByPath(PATH_FOR_EXPAND + "AppController.java");
        loader.waitOnClosed();

        // Check git log
        projectExplorer.selectItem(PROJECT_NAME);
        menu.runCommand(TestMenuCommandsConstants.Git.GIT, TestMenuCommandsConstants.Git.SHOW_HISTORY);
        warningDialog.waitWaitWarnDialogWindowWithSpecifiedTextMess(WARNING_TEXT);
        warningDialog.clickOkBtn();
        projectExplorer.selectItem(PROJECT_NAME);
        menu.runCommand(TestMenuCommandsConstants.Git.GIT, TestMenuCommandsConstants.Git.COMMIT);
        git.waitAndRunCommit("init");
        loader.waitOnClosed();
        menu.runCommand(TestMenuCommandsConstants.Git.GIT, TestMenuCommandsConstants.Git.SHOW_HISTORY);
        git.waitTextInHistoryForm(COMMIT_MESSAGE);
        git.closeGitHistoryForm();
        git.waitHistoryFormToClose();

        // Delete git repo
        menu.runCommand(TestMenuCommandsConstants.Git.GIT, TestMenuCommandsConstants.Git.DELETE_REPOSITORY);
        askDialog.acceptDialogWithText(DELETE_REPO_TEXT);
        loader.waitOnClosed();
        git.waitGitStatusBarWithMess(TestGitConstants.GIT_REPO_DELETE);
        events.clickProjectEventsTab();
        events.waitExpectedMessage(TestGitConstants.GIT_REPO_DELETE);
    }

}
