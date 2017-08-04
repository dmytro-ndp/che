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

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.ssh.shared.dto.GenerateSshPairRequest;
import org.eclipse.che.plugin.ssh.key.SshServiceClient;
import org.eclipse.che.selenium.core.client.TestGitHubServiceClient;
import org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants;
import org.eclipse.che.selenium.core.user.DefaultTestUser;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.ImportProjectFromLocation;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.Menu;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.Wizard;
import org.eclipse.che.selenium.pageobject.git.Git;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * @author Aleksandr Shmaraev
 */
public class CheckoutToRemoteBranchWhichAlreadyHasLinkedLocalBranchTest {
    private static final String PROJECT_NAME  = "testRepo";
    private static final String MASTER_BRANCH = "master";
    private static final String ORIGIN_MASTER = "origin/master";
    private static final String GIT_MSG       = "Ref master already exists";

    @Inject
    private TestWorkspace             ws;
    @Inject
    private Ide                       ide;
    @Inject
    private DefaultTestUser           productUser;
    @Inject
    @Named("github.username")
    private String                    gitHubUsername;
    @Inject
    @Named("github.password")
    private String                    gitHubPassword;
    @Inject
    private ProjectExplorer           projectExplorer;
    @Inject
    private Menu                      menu;
    @Inject
    private Git                       git;
    @Inject
    private Loader                    loader;
    @Inject
    private ImportProjectFromLocation importFromLocation;
    @Inject
    private Wizard                    projectWizard;
    @Inject
    private TestGitHubServiceClient   gitHubClientService;
    @Inject
    private SshServiceClient          sshServiceClient;

    @BeforeClass
    public void prepare() throws Exception {
        try {
            String publicKey = sshServiceClient.generatePair(newDto(GenerateSshPairRequest.class)
                                                                     .withName("github.com")
                                                                     .withService("vcs"))
                                               .getPublicKey();

            gitHubClientService.uploadPublicKey(gitHubUsername,
                                                gitHubPassword,
                                                publicKey);
        } catch (ServerException e) {
            // if already generated, ignore.
            if (!(e.getCause() instanceof ConflictException)) {
                throw e;
            }
        }

        ide.open(ws);
    }

    @Test
    public void checkoutRemoteBranchToExistingLocalBranchTest() throws Exception {
        // Clone test repository with specific remote name.
        projectExplorer.waitProjectExplorer();
        String repoUrl = "https://github.com/" + gitHubUsername + "/gitPullTest.git";
        git.importJavaApp(repoUrl, PROJECT_NAME, Wizard.TypeProject.MAVEN);
        projectExplorer.selectItem(PROJECT_NAME);

        // Open branches form
        menu.runCommand(TestMenuCommandsConstants.Git.GIT, TestMenuCommandsConstants.Git.BRANCHES);
        git.waitBranchInTheList(MASTER_BRANCH);
        git.waitBranchInTheList(ORIGIN_MASTER);
        git.waitBranchInTheListWithCoState(MASTER_BRANCH);

        // Checkout to the master remote branch.
        git.selectBranchAndClickCheckoutBtn(ORIGIN_MASTER);
        git.closeBranchesForm();
        git.waitGitStatusBarWithMess(GIT_MSG);
    }

}
