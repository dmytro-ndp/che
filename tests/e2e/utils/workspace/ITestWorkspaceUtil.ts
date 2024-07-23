/** *******************************************************************
 * copyright (c) 2019-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import { WorkspaceStatus } from './WorkspaceStatus';

export interface ITestWorkspaceUtil {
	waitWorkspaceStatus(workspaceName: string, expectedWorkspaceStatus: WorkspaceStatus): Promise<void>;

	stopWorkspaceByName(workspaceName: string): Promise<void>;

	/**
	 * delete a workspace without stopping phase (similar with force deleting)
	 */
	deleteWorkspaceByName(workspaceName: string): Promise<void>;

	/**
	 * stop workspace before deleting with checking stopping phase
	 */
	stopAndDeleteWorkspaceByName(workspaceName: string): Promise<void>;

	/**
	 * stop all run workspaces in the namespace
	 */
	stopAllRunningWorkspaces(): Promise<void>;

	/**
	 * stop all run workspaces, check statused and remove the workspaces
	 */
	stopAndDeleteAllRunningWorkspaces(): Promise<void>;

	/**
	 * stop all run workspaces without stopping and waiting for of 'Stopped' phase
	 * Similar with 'force' deleting
	 */
	deleteAllWorkspaces(): Promise<void>;
}
