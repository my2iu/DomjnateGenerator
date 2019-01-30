package com.user00.domjnate.generator;

import java.util.Map;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;

/**
 * Goes through all interfaces and tries to merge static methods and properties
 * with the instance version of the interfaces.
 */
public class AssignPackages
{
   ApiDefinition topLevel;
   String pkg;
   
   public AssignPackages(ApiDefinition topLevel, String pkg)
   {
      this.topLevel = topLevel;
      this.pkg = pkg;
   }
   
   public String getFullPackageForInterface(ApiDefinition namespace, InterfaceDefinition intf)
   {
      if (intf.remapPackage != null)
         return pkg + "." + intf.remapPackage;
      String subpkg = null;
      while (namespace.parent != null)
      {
         ApiDefinition parent = namespace.parent;
         if (namespace.remapName != null)
         {
            if (subpkg == null)
               subpkg = namespace.remapName;
            else
               subpkg = namespace.remapName + "." + subpkg;
            break;
         }
         String levelName = null;
         for (Map.Entry<String, ApiDefinition> entry: parent.namespaces.entrySet())
         {
            if (entry.getValue() == namespace)
               levelName = entry.getKey();
         }
         if (levelName == null) throw new IllegalArgumentException("Cannot find namespace");
         if (subpkg == null)
            subpkg = levelName;
         else
            subpkg = levelName + "." + subpkg;
         namespace = parent;
      }
      if (subpkg != null)
         return pkg + "." + subpkg;
      else
         return pkg;
   }

   void handleFunctionInterface(InterfaceDefinition intf, ApiDefinition api) 
   {
      String fullPkg = getFullPackageForInterface(api, intf);
      intf.finalPkg = fullPkg;

   }
   
   void handleInterface(ApiDefinition api, InterfaceDefinition intf) 
   {
      if (intf.doNotGenerateJava) return;
      if (intf.isFunction())
      {
         handleFunctionInterface(intf, api);
         return;
      }
      String fullPkg = getFullPackageForInterface(api, intf);
      intf.finalPkg = fullPkg;
   }

   public void go(ApiDefinition api)
   {
      for (InterfaceDefinition intf: api.interfaces.values())
      {
         handleInterface(api, intf);
      }
      for (Map.Entry<String, ApiDefinition> namespace: api.namespaces.entrySet())
      {
         go(namespace.getValue());
      }
   }
}
