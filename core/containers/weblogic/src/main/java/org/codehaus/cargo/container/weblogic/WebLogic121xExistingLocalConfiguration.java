/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol, 2012-2016 Ali Tokmen.
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

import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.weblogic.internal.AbstractWebLogicWlstExistingLocalConfiguration;
import org.codehaus.cargo.container.weblogic.internal.WebLogicExistingLocalConfigurationCapability;
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
     * @see AbstractWebLogicWlstExistingLocalConfiguration#AbstractWebLogicWlstExistingLocalConfiguration(String)
     */
    public WebLogic121xExistingLocalConfiguration(String dir)
    {
        super(dir);

        setProperty(WebLogicPropertySet.ADMIN_USER, "weblogic");
        setProperty(WebLogicPropertySet.ADMIN_PWD, "weblogic1");
        setProperty(WebLogicPropertySet.SERVER, "server");
        setProperty(ServletPropertySet.PORT, "7001");
        setProperty(GeneralPropertySet.HOSTNAME, "localhost");
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

        // This directory might not exist yet
        String deployDir = getFileHandler().createDirectory(getDomainHome(),
            weblogicContainer.getAutoDeployDirectory());

        // Deploy the cargocpc web-app by copying the WAR file
        getResourceUtils().copyResource(RESOURCE_PATH + "cargocpc.war",
            getFileHandler().append(deployDir, "cargocpc.war"), getFileHandler());

        for (Deployable deployable : getDeployables())
        {
            String deployableName = getFileHandler().getName(deployable.getFile());
            String deployablePath = getFileHandler().append(deployDir, deployableName);

            if (getFileHandler().exists(deployablePath))
            {
                // If deployable is deployed to autodeploy directory then redeploy
                // by deleting and copying.
                if (getFileHandler().isDirectory(deployable.getFile()))
                {
                    getFileHandler().copyDirectory(deployable.getFile(), deployablePath);
                }
                else
                {
                    getFileHandler().copyFile(deployable.getFile(), deployablePath, true);
                }
            }
            else
            {
                // If deployable isn't deployed to autodeploy directory then redeploy
                // using WLST deployer.
                WebLogicWlstOfflineInstalledLocalDeployer deployer =
                        new WebLogicWlstOfflineInstalledLocalDeployer(weblogicContainer);
                deployer.redeploy(deployable);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "WebLogic 12.1.x Existing Configuration";
    }
}
