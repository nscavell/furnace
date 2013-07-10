/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.lifecycle;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.ServiceRegistry;

/**
 * SPI for controlling {@link Addon} life-cycles such as start-up and shut-down.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonLifecycleProvider
{

   /**
    * Initialize the provider with artifacts required for {@link Addon} startup.
    */
   void initialize(Furnace furnace, AddonRegistry registry, Addon self);

   /**
    * Start the given {@link Addon}.
    */
   void start(Addon addon);

   /**
    * Stop the given {@link Addon}.
    */
   void stop(Addon addon);

   /**
    * Get the service registry for the given {@link Addon}.
    */
   ServiceRegistry getServiceRegistry(Addon addon);

   /**
    * Handle any post-startup tasks.
    */
   void postStartup(Addon addon);

}
