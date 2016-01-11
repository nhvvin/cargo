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
package org.codehaus.cargo.container.weblogic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.configuration.entry.DataSource;
import org.codehaus.cargo.container.configuration.entry.Resource;
import org.codehaus.cargo.container.configuration.script.ScriptCommand;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import static org.codehaus.cargo.container.spi.configuration.AbstractLocalConfiguration.RESOURCE_PATH;
import org.codehaus.cargo.container.weblogic.internal.AbstractWebLogicWlstExistingLocalConfiguration;
import org.codehaus.cargo.container.weblogic.internal.WebLogicExistingLocalConfigurationCapability;
import org.codehaus.cargo.container.weblogic.internal.WebLogicLocalContainer;
import org.codehaus.cargo.container.weblogic.internal.WebLogicLocalScriptingContainer;

/**
 * WebLogic 12.1.x existing
 * {@link org.codehaus.cargo.container.spi.configuration.ContainerConfiguration} implementation.
 * WebLogic 12.1.x uses WLST for container configuration.
 */
public class WebLogic121xExistingLocalConfiguration extends
    AbstractWebLogicWlstExistingLocalConfiguration
{

    /**
     * {@inheritDoc}
     *
     * @see AbstractWebLogicWlstExistingLocalConfiguration#AbstractWebLogicWlstExistingLocalConfiguration(String, String)
     */
    public WebLogic121xExistingLocalConfiguration(String dir)
    {
        super(dir, "org/codehaus/cargo/container/internal/resources/weblogicWlst/");

        setProperty(WebLogicPropertySet.ADMIN_USER, "weblogic");
        setProperty(WebLogicPropertySet.ADMIN_PWD, "weblogic1");
        setProperty(WebLogicPropertySet.SERVER, "server");
        setProperty(ServletPropertySet.PORT, "7001");
        setProperty(GeneralPropertySet.HOSTNAME, "localhost");
        setProperty(WebLogicPropertySet.JMS_SERVER, "testJmsServer");
        setProperty(WebLogicPropertySet.JMS_MODULE, "testJmsModule");
        setProperty(WebLogicPropertySet.JMS_SUBDEPLOYMENT, "testJmsSubdeployment");
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationCapability getCapability()
    {
        return new WebLogicExistingLocalConfigurationCapability();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doConfigure(LocalContainer container) throws Exception
    {
        WebLogicLocalScriptingContainer weblogicContainer =
            (WebLogicLocalScriptingContainer) container;
        List<ScriptCommand> configurationScript = new ArrayList<ScriptCommand>();

        // create new domain
        configurationScript.add(getConfigurationFactory().createDomainScript(
                weblogicContainer.getWeblogicHome()));

        // add datasources to script
        for (DataSource dataSource : getDataSources())
        {
            configurationScript.addAll(getConfigurationFactory().dataSourceScript(dataSource));
        }

        // add missing resources to list of resources
        addMissingResources();

        // sort resources
        sortResources();

        // add resources to script
        for (Resource resource : getResources())
        {
            configurationScript.add(getConfigurationFactory().resourceScript(resource));
        }

        // add deployments to script
        WebLogicWlstOfflineInstalledLocalDeployer deployer =
            new WebLogicWlstOfflineInstalledLocalDeployer(weblogicContainer);
        for (Deployable deployable : getDeployables())
        {
            configurationScript.add(deployer.getDeployScript(deployable));
        }

        // write new domain to domain folder
        configurationScript.add(getConfigurationFactory().writeDomainScript());

        getLogger().info("Creating new WebLogic domain.", this.getClass().getName());

        // execute script
        weblogicContainer.executeScript(configurationScript);

        // deploy cargo ping
        deployCargoPing(weblogicContainer);
    }

    /**
     * Deploy the Cargo Ping utility to the container.
     *
     * @param container the container to configure
     * @throws IOException if the cargo ping deployment fails
     */
    private void deployCargoPing(WebLogicLocalContainer container) throws IOException
    {
        // as this is an initial install, this directory will not exist, yet
        String deployDir =
            getFileHandler().createDirectory(getDomainHome(), container.getAutoDeployDirectory());

        // Deploy the cargocpc web-app by copying the WAR file
        getResourceUtils().copyResource(RESOURCE_PATH + "cargocpc.war",
            getFileHandler().append(deployDir, "cargocpc.war"), getFileHandler());
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return "WebLogic 12.1.x Existing Configuration";
    }
}