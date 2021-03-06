package org.jboss.forge.arquillian;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.archive.ForgeRemoteAddon;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Node;

public class ForgeDeploymentPackager implements DeploymentPackager
{
   @Override
   public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors)
   {
      if (testDeployment.getApplicationArchive() instanceof ForgeArchive)
      {
         ForgeArchive deployment = ForgeArchive.class.cast(testDeployment.getApplicationArchive());

         Collection<Archive<?>> auxiliaryArchives = testDeployment.getAuxiliaryArchives();
         for (Archive<?> archive : auxiliaryArchives)
         {
            Map<ArchivePath, Node> content = archive.getContent(new Filter<ArchivePath>()
            {
               @Override
               public boolean include(ArchivePath path)
               {
                  boolean matched = path.get().matches("/org/jboss/shrinkwrap/descriptor/api/(?!spec).*/.*");
                  if (matched)
                     System.out.println(path.toString());
                  return matched;
               }
            });

            Set<ArchivePath> toRemove = new HashSet<ArchivePath>();
            for (Entry<ArchivePath, Node> entry : content.entrySet())
            {
               ArchivePath path = entry.getKey();
               toRemove.add(path);
            }

            for (ArchivePath path : toRemove)
            {
               archive.delete(path);
            }

            deployment.addAsLibrary(archive);
         }
         deployment.addClasses(ForgeArchive.class);

         return deployment;
      }
      else if (testDeployment.getApplicationArchive() instanceof ForgeRemoteAddon)
      {
         return testDeployment.getApplicationArchive();
      }
      else
      {
         throw new IllegalArgumentException(
                  "Invalid Archive type. Ensure that your @Deployment method returns type 'ForgeArchive'.");
      }
   }
}
