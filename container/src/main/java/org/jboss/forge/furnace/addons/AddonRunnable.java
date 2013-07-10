/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.lifecycle.AddonLifecycleProvider;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.util.ClassLoaders;
import org.jboss.forge.furnace.util.Iterators;
import org.jboss.forge.proxy.ClassLoaderAdapterCallback;

/**
 * Loads an {@link Addon}
 */
public final class AddonRunnable implements Runnable
{
   private static final Logger logger = Logger.getLogger(AddonRunnable.class.getName());

   boolean shutdownRequested = false;
   private Furnace furnace;
   private Addon addon;

   private AddonLifecycleManager lifecycleManager;
   private AddonStateManager stateManager;

   private Entry<Addon, AddonLifecycleProvider> lifecycleProvider;

   public AddonRunnable(Furnace furnace,
            AddonLifecycleManager lifecycleManager,
            AddonStateManager stateManager,
            Addon addon)
   {
      this.lifecycleManager = lifecycleManager;
      this.stateManager = stateManager;
      this.furnace = furnace;
      this.addon = addon;
   }

   @Override
   public void run()
   {
      Thread currentThread = Thread.currentThread();
      String name = currentThread.getName();
      currentThread.setName(addon.getId().toCoordinates());
      try
      {
         logger.info("> Starting container [" + addon.getId() + "] [" + addon.getRepository().getRootDirectory() + "]");
         long start = System.currentTimeMillis();

         lifecycleProvider = detectLifecycleProvider();
         if (lifecycleProvider != null)
         {
            ClassLoaders.executeIn(lifecycleProvider.getKey().getClassLoader(), new Callable<Void>()
            {
               @Override
               public Void call() throws Exception
               {
                  AddonLifecycleProvider provider = lifecycleProvider.getValue();
                  provider.initialize(furnace, furnace.getAddonRegistry(getRepositories()), lifecycleProvider.getKey());
                  provider.start(addon);
                  stateManager.setServiceRegistry(addon, provider.getServiceRegistry(addon));

                  for (AddonDependency dependency : addon.getDependencies())
                  {
                     if (dependency.getDependency().getStatus().isLoaded())
                        Addons.waitUntilStarted(dependency.getDependency());
                  }

                  provider.postStartup(addon);
                  return null;
               }
            });
         }

         logger.info(">> Started container [" + addon.getId() + "] - " + (System.currentTimeMillis() - start) + "ms");

      }
      catch (Throwable e)
      {
         addon.getFuture().cancel(false);

         logger.log(Level.WARNING, "Failed to start addon [" + addon.getId() + "] with classloader ["
                  + stateManager.getClassLoaderOf(addon)
                  + "]", e);

         if (!shutdownRequested)
            throw new RuntimeException(e);
      }
      finally
      {
         lifecycleManager.finishedStarting(addon);
         currentThread.setName(name);
      }
   }

   protected AddonRepository[] getRepositories()
   {
      Set<AddonRepository> repositories = stateManager.getViewsOf(addon).iterator().next().getRepositories();
      return repositories.toArray(new AddonRepository[] {});
   }

   public void shutdown()
   {
      shutdownRequested = true;
      try
      {
         logger.info("< Stopping container [" + addon.getId() + "] [" + addon.getRepository().getRootDirectory() + "]");
         long start = System.currentTimeMillis();

         if (lifecycleProvider != null)
         {
            ClassLoaders.executeIn(lifecycleProvider.getKey().getClassLoader(), new Callable<Void>()
            {
               @Override
               public Void call() throws Exception
               {
                  AddonLifecycleProvider provider = lifecycleProvider.getValue();
                  provider.stop(addon);
                  return null;
               }
            });
         }

         logger.info("<< Stopped container [" + addon.getId() + "] - " + (System.currentTimeMillis() - start) + "ms");
      }
      catch (RuntimeException e)
      {
         logger.log(Level.SEVERE, "Failed to shut down addon " + addon.getId(), e);
         throw e;
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Failed to shut down addon " + addon.getId(), e);
         throw new ContainerException("Failed to shut down addon " + addon.getId(), e);
      }
   }

   private Entry<Addon, AddonLifecycleProvider> detectLifecycleProvider()
   {
      Map<Addon, AddonLifecycleProvider> lifecycleProviderMap = new HashMap<Addon, AddonLifecycleProvider>();
      for (AddonDependency d : addon.getDependencies())
      {
         Addon dependency = d.getDependency();
         if (dependency.getStatus().isLoaded())
         {
            ClassLoader classLoader = dependency.getClassLoader();
            ServiceLoader<?> serviceLoader = ServiceLoader.load(
                     ClassLoaders.loadClass(classLoader, AddonLifecycleProvider.class), classLoader);

            List<?> providers = Iterators.asList(serviceLoader);

            if (!providers.isEmpty())
            {
               if (providers.size() == 1)
               {
                  AddonLifecycleProvider provider = ClassLoaderAdapterCallback.enhance(getClass().getClassLoader(),
                           classLoader, providers.get(0), AddonLifecycleProvider.class);
                  lifecycleProviderMap.put(dependency, provider);
               }
               else
               {
                  throw new ContainerException("Expected only one [" + AddonLifecycleProvider.class.getName()
                           + "] but found [" + providers.size() + "]. Redundant container implementations must be "
                           + "removed before this addon can be started.");
               }
            }
         }
      }

      Entry<Addon, AddonLifecycleProvider> result = null;
      if (!lifecycleProviderMap.isEmpty())
      {
         if (lifecycleProviderMap.size() == 1)
         {
            result = lifecycleProviderMap.entrySet().iterator().next();
         }
         else
         {
            throw new ContainerException("Multiple [" + AddonLifecycleProvider.class.getName()
                     + "] found in Addon [" + addon.getId() + "], but only one is allowed. Redundant container "
                     + "implementations must be removed before this addon can be started."
                     + "\n" + "YOU MUST REMOVE ALL BUT ONE OF THE FOLLOWING DEPENDENCIES:"
                     + "\n" + lifecycleProviderMap.keySet());
         }
      }
      return result;
   }

   @Override
   public String toString()
   {
      return addon.toString();
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((addon == null) ? 0 : addon.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AddonRunnable other = (AddonRunnable) obj;
      if (addon == null)
      {
         if (other.addon != null)
            return false;
      }
      else if (!addon.equals(other.addon))
         return false;
      return true;
   }
}
