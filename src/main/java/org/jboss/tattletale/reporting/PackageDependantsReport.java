/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.tattletale.reporting;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jboss.tattletale.core.Archive;
import org.jboss.tattletale.core.NestableArchive;
import org.jboss.tattletale.profiles.Profile;

/**
 * Reporting class that will generate package level
 * reports like how {@link ClassDependantsReport} does
 * on class level reports.
 * @author Navin Surtani
 */
public class PackageDependantsReport extends CLSReport
{
   /** NAME */
   private static final String NAME = "Package Dependants";

   /** DIRECTORY */
   private static final String DIRECTORY = "packagedependants";

   /** Constructor */
   public PackageDependantsReport()
   {
      super(DIRECTORY, ReportSeverity.INFO, NAME, DIRECTORY);
   }

   /**
    * write out the report's content
    * @param bw the writer to use
    * @throws IOException if an error occurs
    */
   public void writeHtmlBodyContent(BufferedWriter bw) throws IOException
   {
      bw.write("<table>" + Dump.newLine());

      bw.write("  <tr>" + Dump.newLine());
      bw.write("    <th>Package</th>" + Dump.newLine());
      bw.write("    <th>Dependants</th>" + Dump.newLine());
      bw.write("  </tr>" + Dump.newLine());

      final SortedMap<String, SortedSet<String>> result = recursivelyBuildResultFromArchive(archives);
      boolean odd = true;

      for (Map.Entry<String, SortedSet<String>> entry : result.entrySet())
      {
         SortedSet<String> packDeps = entry.getValue();

         if (null != packDeps && packDeps.size() > 0)
         {
            if (odd)
            {
               bw.write("  <tr package =\"rowodd\">" + Dump.newLine());
            }
            else
            {
               bw.write("  <tr package =\"roweven\">" + Dump.newLine());
            }
            bw.write("    <td>" + entry.getKey() + "</td>" + Dump.newLine());

            bw.write("    <td>");
            bw.write(join(packDeps, ", "));
            bw.write("</td>" + Dump.newLine());

            bw.write("  </tr>" + Dump.newLine());

            odd = !odd;
         }
      }

      bw.write("</table>" + Dump.newLine());
   }

   /**
    * Method recursivelyBuildResultFromArchive.
    * @param archives Collection<Archive>
    * @return SortedMap&lt;String,SortedSet&lt;String&gt;&gt;
    */
   private SortedMap<String, SortedSet<String>> recursivelyBuildResultFromArchive(Collection<Archive> archives)
   {
      final SortedMap<String, SortedSet<String>> result = new TreeMap<String, SortedSet<String>>();

      for (Archive archive : archives)
      {
         if (archive instanceof NestableArchive)
         {
            NestableArchive nestableArchive = (NestableArchive) archive;
            SortedMap<String, SortedSet<String>> subResult =
                recursivelyBuildResultFromArchive(nestableArchive.getSubArchives());
            result.putAll(subResult);
         }
         else
         {
            SortedMap<String, SortedSet<String>> packageDependencies = archive.getPackageDependencies();
            for (Map.Entry<String, SortedSet<String>> entry : packageDependencies.entrySet())
            {
               String pack = entry.getKey();

               for (String dep : entry.getValue())
               {
                  if (!dep.equals(pack))
                  {
                     boolean include = true;

                     for (Profile profile : getKnown())
                     {
                        if (profile.doesProvide(dep))
                        {
                           include = false;
                           break;
                        }
                     }

                     if (include)
                     {
                        SortedSet<String> deps = result.get(dep);

                        if (null == deps)
                        {
                           deps = new TreeSet<String>();
                        }

                        deps.add(pack);

                        result.put(dep, deps);
                     }
                  }
               }
            }
         }
      }
      return result;
   }
}
