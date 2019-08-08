/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.docker.environment.dockerimage;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.Warning;
import org.eclipse.che.api.core.model.workspace.config.Environment;
import org.eclipse.che.api.installer.server.InstallerRegistry;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.*;
import org.eclipse.che.commons.annotation.Nullable;

/** @author Sergii Leshchenko */
@Singleton
public class DockerImageEnvironmentFactory
    extends InternalEnvironmentFactory<DockerImageEnvironment> {

  private final MemoryAttributeProvisioner memoryProvisioner;

  @Inject
  public DockerImageEnvironmentFactory(
      InstallerRegistry installerRegistry,
      RecipeRetriever recipeRetriever,
      MachineConfigsValidator machinesValidator,
      MemoryAttributeProvisioner memoryProvisioner) {
    super(installerRegistry, recipeRetriever, machinesValidator);
    this.memoryProvisioner = memoryProvisioner;
  }

  @Override
  public DockerImageEnvironment create(Environment sourceEnv)
      throws InfrastructureException, ValidationException {
    checkNotNull(
        sourceEnv, "Null environment is not supported by docker image environment factory");
    if (sourceEnv.getRecipe().getLocation() != null) {
      // move image from location to content
      EnvironmentImpl envCopy = new EnvironmentImpl(sourceEnv);
      envCopy.getRecipe().setContent(sourceEnv.getRecipe().getLocation());
      envCopy.getRecipe().setLocation(null);
      return super.create(envCopy);
    }
    return super.create(sourceEnv);
  }

  @Override
  protected DockerImageEnvironment doCreate(
      @Nullable InternalRecipe recipe,
      Map<String, InternalMachineConfig> machines,
      List<Warning> warnings)
      throws ValidationException {
    checkNotNull(recipe, "Null recipe is not supported by docker image environment factory");
    if (!DockerImageEnvironment.TYPE.equals(recipe.getType())) {
      throw new ValidationException(
          format(
              "Docker image environment parser doesn't support recipe type '%s'",
              recipe.getType()));
    }

    String dockerImage = recipe.getContent();

    checkArgument(dockerImage != null, "Docker image should not be null.");

    ensureSingleMachine(machines);

    addRamAttributes(machines);

    return new DockerImageEnvironment(dockerImage, recipe, machines, warnings);
  }

  private void ensureSingleMachine(Map<String, InternalMachineConfig> machines)
      throws ValidationException {
    int nofMachines = machines.size();
    if (nofMachines == 0) {
      // we create a "fake" machine definition where the rest of the code can put additional
      // definitions, if needed.
      InternalMachineConfig emptyConfig = new InternalMachineConfig();
      // let's just call the machine after the type. The name doesn't matter that much anyway.
      machines.put(DockerImageEnvironment.TYPE, emptyConfig);
    } else if (nofMachines > 1) {
      throw new ValidationException(
          format(
              "Docker image environment only supports a single machine definition but found %d.",
              nofMachines));
    }
  }

  private void addRamAttributes(Map<String, InternalMachineConfig> machines) {
    for (InternalMachineConfig machineConfig : machines.values()) {
      memoryProvisioner.provision(machineConfig, 0L, 0L);
    }
  }

  private static void checkNotNull(
      Object object, String errorMessageTemplate, Object... errorMessageParams)
      throws ValidationException {
    if (object == null) {
      throw new ValidationException(format(errorMessageTemplate, errorMessageParams));
    }
  }
}
