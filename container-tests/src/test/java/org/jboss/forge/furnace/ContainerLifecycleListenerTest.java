package org.jboss.forge.furnace;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.FurnaceImpl;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class ContainerLifecycleListenerTest
{
   @Deployment
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.furnace:container-cdi", version = "2.0.0-SNAPSHOT")
   })
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addClasses(ContainerLifecycleListenerTest.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace:container-cdi", "2.0.0-SNAPSHOT")
               );

      return archive;
   }

   @Inject
   private Furnace furnace;

   @Test
   public void testContainerStartup()
   {
      FurnaceImpl impl = (FurnaceImpl) furnace;
      List<ContainerLifecycleListener> listeners = impl.getRegisteredListeners();
      Assert.assertEquals(1, listeners.size());
      Assert.assertEquals(1, ((TestLifecycleListener) listeners.get(0)).beforeStartTimesCalled);
   }
}