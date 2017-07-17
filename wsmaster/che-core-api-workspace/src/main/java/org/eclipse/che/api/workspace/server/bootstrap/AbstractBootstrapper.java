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
package org.eclipse.che.api.workspace.server.bootstrap;

import org.eclipse.che.api.core.model.workspace.runtime.BootstrapperStatus;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.server.OutputEndpoint;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.shared.dto.RuntimeIdentityDto;
import org.eclipse.che.api.workspace.shared.dto.event.BootstrapperStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bootstraps installers.
 *
 * @author Sergii Leshchenko
 */
public abstract class AbstractBootstrapper {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBootstrapper.class);

    private static final AtomicInteger ENDPOINT_IDS = new AtomicInteger();

    private final String                                     machineName;
    private final int                                        bootstrappingTimeoutMinutes;
    private final EventService                               eventService;
    private final EventSubscriber<BootstrapperStatusEvent>   bootstrapperStatusListener;
    private final String                                     installerEndpoint;
    private final String                                     outputEndpoint;
    private       CompletableFuture<BootstrapperStatusEvent> finishEventFuture;

    public AbstractBootstrapper(String machineName,
                                RuntimeIdentity runtimeIdentity,
                                int bootstrappingTimeoutMinutes,
                                String websocketBaseEndpoint,
                                EventService eventService) {
        this.machineName = machineName;
        this.bootstrappingTimeoutMinutes = bootstrappingTimeoutMinutes;
        this.eventService = eventService;
        this.installerEndpoint = websocketBaseEndpoint + InstallerEndpoint.INSTALLER_WEBSOCKET_ENDPOINT_BASE;
        this.outputEndpoint = websocketBaseEndpoint + OutputEndpoint.OUTPUT_WEBSOCKET_ENDPOINT_BASE;
        this.bootstrapperStatusListener = event -> {
            BootstrapperStatus status = event.getStatus();
            //skip starting status event
            if (status.equals(BootstrapperStatus.DONE) || status.equals(BootstrapperStatus.FAILED)) {
                //check boostrapper belongs to current runtime and machine
                RuntimeIdentityDto runtimeId = event.getRuntimeId();
                if (event.getMachineName().equals(machineName)
                    && runtimeIdentity.getEnvName().equals(runtimeId.getEnvName())
                    && runtimeIdentity.getOwner().equals(runtimeId.getOwner())
                    && runtimeIdentity.getWorkspaceId().equals(runtimeId.getWorkspaceId())) {

                    finishEventFuture.complete(event);
                }
            }
        };
    }

    /**
     * Bootstraps installers and wait while they finished.
     *
     * @throws InfrastructureException
     *         when bootstrapping timeout reached
     * @throws InfrastructureException
     *         when bootstrapping failed
     * @throws InfrastructureException
     *         when any other error occurs while bootstrapping
     */
    public void bootstrap() throws InfrastructureException {
        if (finishEventFuture != null) {
            throw new IllegalStateException("Bootstrap method must be called only once.");
        }
        finishEventFuture = new CompletableFuture<>();

        eventService.subscribe(bootstrapperStatusListener, BootstrapperStatusEvent.class);
        try {
            LOG.info("Launching bootstrapper");

            doBootstrapAsync(installerEndpoint + ENDPOINT_IDS.getAndIncrement(),
                             outputEndpoint + ENDPOINT_IDS.getAndIncrement());

            LOG.info("Launched bootstrapper. Waiting for Done event");

            //waiting for DONE or FAILED bootstrapper status event
            BootstrapperStatusEvent resultEvent = finishEventFuture.get(bootstrappingTimeoutMinutes, TimeUnit.MINUTES);
            if (resultEvent.getStatus().equals(BootstrapperStatus.FAILED)) {
                throw new InfrastructureException(resultEvent.getError());
            }
        } catch (ExecutionException e) {
            throw new InfrastructureException(e.getCause().getMessage(), e);
        } catch (TimeoutException e) {
            throw new InfrastructureException("Bootstrapping of machine " + machineName + " reached timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InfrastructureException("Bootstrapping of machine " + machineName + " was interrupted");
        } finally {
            eventService.unsubscribe(bootstrapperStatusListener, BootstrapperStatusEvent.class);
        }
    }

    /**
     * Launches bootstrapping.
     *
     * @param installerWebsocketEndpoint
     *         endpoint for pushing installers and bootstrapper statuses
     * @param outputWebsocketEndpoint
     *         endpoint for pushing installer logs
     * @throws InfrastructureException
     *         when any other error occurs while bootstrapping launching
     */
    protected abstract void doBootstrapAsync(String installerWebsocketEndpoint,
                                             String outputWebsocketEndpoint) throws InfrastructureException;
}
