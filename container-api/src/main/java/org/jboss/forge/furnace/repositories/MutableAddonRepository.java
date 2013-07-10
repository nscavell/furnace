/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.repositories;

import java.io.File;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;

/**
 * Used to perform {@link Addon} installation/registration operations. May be obtained using CDI injection:
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface MutableAddonRepository extends AddonRepository
{
   public boolean deploy(AddonId addon, Iterable<AddonDependencyEntry> dependencies, Iterable<File> resourceJars);

   public boolean disable(final AddonId addon);

   public boolean enable(AddonId addon);

   public boolean undeploy(AddonId addonEntry);
}
