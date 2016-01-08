/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol, 2012-2015 Ali Tokmen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ========================================================================
 */
package org.codehaus.cargo.container.websphere;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.configuration.entry.DataSource;
import org.codehaus.cargo.container.configuration.entry.Resource;
import org.codehaus.cargo.container.configuration.script.ScriptCommand;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfiguration;
import org.codehaus.cargo.container.websphere.internal.WebSphere85xStandaloneLocalConfigurationCapability;
import org.codehaus.cargo.container.websphere.internal.configuration.WebSphereJythonConfigurationFactory;
import org.codehaus.cargo.container.websphere.internal.configuration.rules.WebSphereResourceRules;
import org.codehaus.cargo.container.websphere.util.ByteUnit;
import org.codehaus.cargo.container.websphere.util.JvmArguments;
import org.codehaus.cargo.container.websphere.util.WebSphereResourceComparator;
import org.codehaus.cargo.util.CargoException;

/**
 * IBM WebSphere 8.5 standalone
 * {@link org.codehaus.cargo.container.spi.configuration.ContainerConfiguration} implementation.
 * 
 */
public class WebSphere85xStandaloneLocalConfiguration extends AbstractStandaloneLocalConfiguration
    implements WebSphereConfiguration
{
    /**
     * Capability of the WebSphere standalone configuration.
     */
    private static ConfigurationCapability capability =
        new WebSphere85xStandaloneLocalConfigurationCapability();

    /**
     * WebSphere container.
     */
    private WebSphere85xInstalledLocalContainer wsContainer;

    /**
     * Configuration factory for creating WebSphere jython configuration scripts.
     */
    private WebSphereJythonConfigurationFactory factory;

    /**
     * {@inheritDoc}
     * @see AbstractStandaloneLocalConfiguration#AbstractStandaloneLocalConfiguration(String)
     */
    public WebSphere85xStandaloneLocalConfiguration(String dir)
    {
        super(dir);
        factory = new WebSphereJythonConfigurationFactory(this,
                RESOURCE_PATH + "websphere85x/commands/");

        setProperty(ServletPropertySet.PORT, "9080");

        setProperty(WebSpherePropertySet.ADMIN_USERNAME, "websphere");
        setProperty(WebSpherePropertySet.ADMIN_PASSWORD, "websphere");

        setProperty(WebSpherePropertySet.PROFILE, "cargoProfile");
        setProperty(WebSpherePropertySet.CELL, "cargoNodeCell");
        setProperty(WebSpherePropertySet.NODE, "cargoNode");
        setProperty(WebSpherePropertySet.SERVER, "cargoServer");

        setProperty(WebSpherePropertySet.CLASSLOADER_MODE, "PARENT_FIRST");
        setProperty(WebSpherePropertySet.WAR_CLASSLOADER_POLICY, "MULTIPLE");
        setProperty(WebSpherePropertySet.APPLICATION_SECURITY, "true");

        setProperty(WebSpherePropertySet.JMS_SIBUS, "jmsBus");

        setProperty(WebSpherePropertySet.EJB_TO_ACT_SPEC_BINDING, "");
        setProperty(WebSpherePropertySet.EJB_TO_RES_REF_BINDING, "");
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationCapability getCapability()
    {
        return capability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doConfigure(LocalContainer container) throws Exception
    {
        this.wsContainer = (WebSphere85xInstalledLocalContainer) container;

        // delete old profile and create new profile
        deleteOldProfile();
        createNewProfile(container);

        getLogger().info("Configuring profile.", this.getClass().getName());

        List<ScriptCommand> commands = new ArrayList<ScriptCommand>();

        // add miscellaneous configuration
        commands.add(factory.miscConfigurationScript());

        // add JVM configuration
        commands.addAll(createJvmPropertiesScripts(wsContainer));

        // add system properties
        for (Map.Entry<String, String> systemProperty
                : wsContainer.getSystemProperties().entrySet())
        {
            if (systemProperty.getValue() != null)
            {
                commands.add(factory.setSystemPropertyScript(systemProperty.getKey(),
                        systemProperty.getValue()));
            }
        }

        // add shared libraries
        List<String> extraLibraries = Arrays.asList(wsContainer.getExtraClasspath());
        for (String extraLibrary : extraLibraries)
        {
            commands.add(factory.deploySharedLibraryScript(extraLibrary));
        }

        // create datasources
        for (DataSource dataSource : getDataSources())
        {
            commands.addAll(factory.createDataSourceScript(dataSource, extraLibraries));
        }

        // add missing resources to list of resources
        WebSphereResourceRules.addMissingJmsResources(this);

        // sort resources
        WebSphereResourceComparator resourceComparator = new WebSphereResourceComparator();
        List<Resource> resources = getResources();
        Collections.sort(resources, resourceComparator);

        // create resources
        for (Resource resource : getResources())
        {
            commands.add(factory.createResourceScript(resource));
        }

        // deploy cargo ping
        commands.add(getDeployCargoPingScript());

        // deploy deployables
        for (Deployable deployable : getDeployables())
        {
            commands.addAll(factory.deployDeployableScript(deployable, extraLibraries));
        }

        //save and activate
        commands.add(factory.saveSyncScript());

        wsContainer.executeScript(commands);
    }

    /**
     * Delete old profile.
     * @throws Exception if any error is raised during deleting of profile
     */
    private void deleteOldProfile() throws Exception
    {
        getLogger().info("Deleting old profile.", this.getClass().getName());

        // Delete profile in WebSphere
        wsContainer.runManageProfileCommand(
            "-delete",
            "-profileName",
            getPropertyValue(WebSpherePropertySet.PROFILE));

        // Profile directory has to be deleted too.
        getLogger().debug("Deleting profile folder " + getHome(), this.getClass().getName());
        getFileHandler().delete(getHome());

        if (getFileHandler().isDirectory(getHome()))
        {
            throw new CargoException("Directory " + getHome() + " cannot be deleted");
        }

        // Update profile informations in WebSphere
        wsContainer.runManageProfileCommand("-validateAndUpdateRegistry");
    }

    /**
     * Create new profile.
     * @param container Container.
     * @throws Exception if any error is raised during deleting of profile
     */
    private void createNewProfile(Container container) throws Exception
    {
        File portsFile = File.createTempFile("cargo-websphere-portdef-", ".properties");
        getResourceUtils().copyResource(RESOURCE_PATH + container.getId() + "/portdef.props",
            portsFile, createFilterChain(), "ISO-8859-1");

        try
        {
            getLogger().info("Creating new profile.", this.getClass().getName());
            wsContainer.runManageProfileCommand(
                "-create",
                "-profileName",
                getPropertyValue(WebSpherePropertySet.PROFILE),
                "-profilePath",
                getHome(),
                "-nodeName",
                getPropertyValue(WebSpherePropertySet.NODE),
                "-cellName",
                getPropertyValue(WebSpherePropertySet.CELL),
                "-serverName",
                getPropertyValue(WebSpherePropertySet.SERVER),
                "-portsFile",
                portsFile.getAbsolutePath(),
                "-winserviceCheck",
                "false",
                "-enableService",
                "false",
                "-enableAdminSecurity",
                "true",
                "-adminUserName",
                getPropertyValue(WebSpherePropertySet.ADMIN_USERNAME),
                "-adminPassword",
                getPropertyValue(WebSpherePropertySet.ADMIN_PASSWORD));
        }
        finally
        {
            portsFile.delete();
        }
    }

    /**
     * Create JVM properties.
     * @param container Container.
     * @return Scripts for creating JVM properties.
     * @throws Exception if any error is raised during deleting of profile.
     */
    private Collection<ScriptCommand> createJvmPropertiesScripts(
            WebSphere85xInstalledLocalContainer container) throws Exception
    {
        Collection<ScriptCommand> jvmCommands = new ArrayList<ScriptCommand>();

        String jvmArgs = getPropertyValue(GeneralPropertySet.JVMARGS);
        JvmArguments parsedArguments = JvmArguments.parseArguments(jvmArgs);

        jvmCommands.add(factory.setJvmPropertyScript("initialHeapSize",
                Long.toString(parsedArguments.getInitialHeap(ByteUnit.MEGABYTES))));
        jvmCommands.add(factory.setJvmPropertyScript("maximumHeapSize",
                Long.toString(parsedArguments.getMaxHeap(ByteUnit.MEGABYTES))));
        jvmCommands.add(factory.setJvmPropertyScript("genericJvmArguments",
                parsedArguments.getGenericArgs()));

        return jvmCommands;
    }

    /**
     * Deploy the Cargo Ping utility to the container.
     * @return Script command deploying cargo ping war file.
     * @throws Exception if the cargo ping deployment fails
     */
    private ScriptCommand getDeployCargoPingScript() throws Exception
    {
        File cargoCpc = File.createTempFile("cargo-cpc-", ".war");
        getResourceUtils().copyResource(RESOURCE_PATH + "cargocpc.war", cargoCpc);
        WAR cargoCpcWar = new WAR(cargoCpc.getAbsolutePath());
        cargoCpcWar.setContext("cargocpc");
        return factory.deployDeployableScript(cargoCpcWar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "WebSphere 8.5 Standalone Configuration";
    }

    /**
     * {@inheritDoc}
     * @see WebSphereConfiguration#getFactory()
     */
    public WebSphereJythonConfigurationFactory getFactory()
    {
        return factory;
    }
}
