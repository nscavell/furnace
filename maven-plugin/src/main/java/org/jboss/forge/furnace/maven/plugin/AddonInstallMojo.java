/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.maven.plugin;

import java.io.File;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.manager.AddonManager;
import org.jboss.forge.furnace.manager.impl.AddonManagerImpl;
import org.jboss.forge.furnace.manager.request.InstallRequest;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;

/**
 * Goal which installs addons to a specified directory
 */
@Mojo(name = "addon-install", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class AddonInstallMojo extends AbstractMojo
{

   /**
    * Addon repository file location
    */
   @Parameter(property = "forge.repository", required = true)
   private File addonRepository;

   /**
    * Addon IDs to install
    */
   @Parameter(property = "forge.addonIds", required = true)
   private String[] addonIds;
   
   @Override
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      Furnace forge = new FurnaceImpl();
      if (!addonRepository.exists())
      {
         addonRepository.mkdirs();
      }
      AddonRepository repository = forge.addRepository(AddonRepositoryMode.MUTABLE, addonRepository);
      Iterator<AddonDependencyResolver> it = ServiceLoader.load(AddonDependencyResolver.class).iterator();
      if (!it.hasNext())
      {
         throw new MojoExecutionException(
                  "No AddonDependencyResolver implementation found. Please add one in the <dependencies> section of the forge-maven-plugin.");
      }
      AddonDependencyResolver addonResolver = it.next();
      AddonManager addonManager = new AddonManagerImpl(forge, addonResolver, false);

      for (String addonId : addonIds)
      {
         AddonId id = AddonId.fromCoordinates(addonId);
         InstallRequest install = addonManager.install(id, repository);
         if (!install.getActions().isEmpty())
         {
            getLog().info("" + install);
            install.perform();
         }
      }
   }
}
