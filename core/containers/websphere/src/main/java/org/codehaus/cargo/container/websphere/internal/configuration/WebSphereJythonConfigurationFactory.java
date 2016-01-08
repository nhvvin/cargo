/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol, 2011-2015 Ali Tokmen.
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
package org.codehaus.cargo.container.websphere.internal.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.cargo.container.configuration.Configuration;
import org.codehaus.cargo.container.configuration.entry.DataSource;
import org.codehaus.cargo.container.configuration.entry.Resource;
import org.codehaus.cargo.container.configuration.script.ScriptCommand;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.User;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.ImportWsadminlibScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.deployment.AddSharedLibraryToDeployableScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.deployment.DeployDeployableScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.deployment.DeploySharedLibraryScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.deployment.MapApplicationSecurityRoleScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.deployment.UndeployDeployableScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.domain.MiscConfigurationScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.domain.SaveSyncScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.domain.SetJvmPropertyScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.domain.SetSystemPropertyScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.resource.DataSourceConnectionPropertyScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.resource.DataSourceScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.resource.JmsConnectionFactoryScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.resource.JmsQueueScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.resource.JmsSiBusMemberScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.resource.JmsSiBusScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.user.AddUserToGroupScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.user.CreateGroupScriptCommand;
import org.codehaus.cargo.container.websphere.internal.configuration.commands.user.CreateUserScriptCommand;
import org.codehaus.cargo.module.webapp.WarArchive;
import org.codehaus.cargo.module.webapp.WarArchiveIo;
import org.codehaus.cargo.module.webapp.WebXml;
import org.codehaus.cargo.module.webapp.WebXmlUtils;
import org.codehaus.cargo.util.CargoException;

/**
 * WebSphere configuration factory returning specific jython configuration scripts.
 */
public class WebSphereJythonConfigurationFactory
{
    /**
     * Type to resource command script class map.
     */
    private static Map<String, Class<? extends ScriptCommand>> resourceMap =
            new HashMap<String, Class<? extends ScriptCommand>>();

    /**
     * Path to configuration script resources.
     */
    private final String resourcePath;

    /**
     * Container configuration.
     */
    private Configuration configuration;

    static
    {
        resourceMap.put(WebSphereConfigurationEntryType.JMS_SIBUS, JmsSiBusScriptCommand.class);
        resourceMap.put(WebSphereConfigurationEntryType.JMS_SIBUS_MEMBER,
                JmsSiBusMemberScriptCommand.class);
        resourceMap.put(WebSphereConfigurationEntryType.JMS_CONNECTION_FACTORY,
                JmsConnectionFactoryScriptCommand.class);
        resourceMap.put(WebSphereConfigurationEntryType.JMS_QUEUE,
                JmsQueueScriptCommand.class);
    }

    /**
     * Sets configuration containing all needed information for building configuration scripts.
     *
     * @param configuration Container configuration.
     * @param resourcePath Path to configuration script resources.
     */
    public WebSphereJythonConfigurationFactory(Configuration configuration, String resourcePath)
    {
        this.resourcePath = resourcePath;
        this.configuration = configuration;
    }

    /**
     * @param wsadminlibPath Path to wsadminlib script.
     * @return Import wsadminlib jython script.
     */
    public ScriptCommand importWsadminlibScript(String wsadminlibPath)
    {
        return new ImportWsadminlibScriptCommand(configuration, resourcePath, wsadminlibPath);
    }

    /* Domain configuration*/

    /**
     * @return Save and sync wsadminlib jython script.
     */
    public ScriptCommand saveSyncScript()
    {
        return new SaveSyncScriptCommand(configuration, resourcePath);
    }

    /**
     * @param propertyName Name of JVM property.
     * @param propertyValue Value of JVM property.
     * @return Set JVM property jython script.
     */
    public ScriptCommand setJvmPropertyScript(String propertyName, String propertyValue)
    {
        return new SetJvmPropertyScriptCommand(configuration, resourcePath, propertyName,
                propertyValue);
    }

    /**
     * @param propertyName Name of system property.
     * @param propertyValue Value of system property.
     * @return Set system property jython script.
     */
    public ScriptCommand setSystemPropertyScript(String propertyName, String propertyValue)
    {
        return new SetSystemPropertyScriptCommand(configuration, resourcePath, propertyName,
                propertyValue);
    }

    /**
     * @return Miscellaneous configuration jython script.
     */
    public ScriptCommand miscConfigurationScript()
    {
        return new MiscConfigurationScriptCommand(configuration, resourcePath);
    }

    /* Deployment configuration*/

    /**
     * @param sharedLibraryPath Shared library to be deployed.
     * @return Deploy shared library jython script.
     */
    public ScriptCommand deploySharedLibraryScript(String sharedLibraryPath)
    {
        return new DeploySharedLibraryScriptCommand(configuration, resourcePath, sharedLibraryPath);
    }

    /**
     * @param deployable Deployable to be deployed.
     * @return Deploy deployable jython script.
     */
    public ScriptCommand deployDeployableScript(Deployable deployable)
    {
        return new DeployDeployableScriptCommand(configuration, resourcePath, deployable);
    }

    /**
     * @param deployable Deployable to be deployed.
     * @param sharedLibraries Shared libraries used by this deployable.
     * @return Deploy deployable using shared libraries jython script.
     */
    public List<ScriptCommand> deployDeployableScript(Deployable deployable,
            Collection<String> sharedLibraries)
    {
        List<ScriptCommand> scriptCommands = new ArrayList<ScriptCommand>();
        scriptCommands.add(deployDeployableScript(deployable));

        for (String sharedLibrary : sharedLibraries)
        {
            scriptCommands.add(new AddSharedLibraryToDeployableScriptCommand(configuration,
                    resourcePath, deployable, sharedLibrary));
        }

        return scriptCommands;
    }

    /**
     * @param deployable Deployable to be undeployed.
     * @return Undeploy deployable jython script.
     */
    public ScriptCommand undeployDeployableScript(Deployable deployable)
    {
        return new UndeployDeployableScriptCommand(configuration, resourcePath, deployable);
    }

    /**
     * @param deployable Deployable containing roles which needs to be mapped.
     * @return Map roles jython script.
     */
    public List<ScriptCommand> mapApplicationSecurityRolesScript(Deployable deployable)
    {
        List<ScriptCommand> scriptCommands = new ArrayList<ScriptCommand>();

        if (deployable instanceof WAR)
        {
            try
            {
                WarArchive warArchive = WarArchiveIo.open(deployable.getFile());
                WebXml webXml = warArchive.getWebXml();
                List<String> securityRoleNames = WebXmlUtils.getSecurityRoleNames(webXml);
                for (String securityRoleName : securityRoleNames)
                {
                    scriptCommands.add(new MapApplicationSecurityRoleScriptCommand(configuration,
                            resourcePath, deployable, securityRoleName, securityRoleName));
                }
            }
            catch (Exception e)
            {
                throw new CargoException("Error when retrieving security roles!", e);
            }
        }

        return scriptCommands;
    }

    /* User/group configuration*/

    /**
     * @param user User to be created.
     * @return Create user jython script.
     */
    public List<ScriptCommand> createUserScript(User user)
    {
        List<ScriptCommand> scriptCommands = new ArrayList<ScriptCommand>();

        scriptCommands.add(new CreateUserScriptCommand(configuration, resourcePath, user));

        for (String role : user.getRoles())
        {
            scriptCommands.add(new CreateGroupScriptCommand(configuration, resourcePath, role));
            scriptCommands.add(new AddUserToGroupScriptCommand(configuration, resourcePath,
                    user, role));
        }

        return scriptCommands;
    }

    /* DataSource/Resource configuration*/

    /**
     * @param dataSource DataSource to be created.
     * @param sharedLibraries Shared libraries containing database drivers.
     * @return Create datasource jython script.
     */
    public List<ScriptCommand> createDataSourceScript(DataSource dataSource,
            Collection<String> sharedLibraries)
    {
        List<ScriptCommand> scriptCommands = new ArrayList<ScriptCommand>();

        scriptCommands.add(new DataSourceScriptCommand(configuration, resourcePath, dataSource,
                sharedLibraries));

        for (Entry<Object, Object> property : dataSource.getConnectionProperties().entrySet())
        {
            scriptCommands.add(new DataSourceConnectionPropertyScriptCommand(configuration,
                    resourcePath, dataSource, property));
        }

        return scriptCommands;
    }

    /**
     * @param resource Resource.
     * @return Create resource jython script.
     */
    public ScriptCommand createResourceScript(Resource resource)
    {
        Class<? extends ScriptCommand> resourceClass = resourceMap.get(resource.getType());
        ScriptCommand newInstance = null;

        if (resourceClass == null)
        {
            throw new CargoException("WebSphere doesn't support resource type "
                    + resource.getType());
        }

        try
        {
            newInstance = resourceClass.getConstructor(Configuration.class,
                    String.class, Resource.class).newInstance(configuration,
                            resourcePath, resource);
        }
        catch (Exception e)
        {
            throw new CargoException("Failed instantiation of resource command.", e);
        }

        return newInstance;
    }
}
