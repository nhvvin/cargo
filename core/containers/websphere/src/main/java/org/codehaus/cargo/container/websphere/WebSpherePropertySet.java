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

/**
 * Gathers all WebSphere properties.
 * 
 */
public interface WebSpherePropertySet
{
    /**
     * User with administrator rights.
     */
    String ADMIN_USERNAME = "cargo.websphere.administrator.user";

    /**
     * Password for user with administrator rights.
     */
    String ADMIN_PASSWORD = "cargo.websphere.administrator.password";

    /**
     * WebSphere profile name.
     */
    String PROFILE = "cargo.websphere.profile";

    /**
     * WebSphere node name.
     */
    String NODE = "cargo.websphere.node";

    /**
     * WebSphere cell name.
     */
    String CELL = "cargo.websphere.cell";

    /**
     * WebSphere server name.
     */
    String SERVER = "cargo.websphere.server";

    /**
     * Log level used in the server log.
     */
    String LOGGING = "cargo.websphere.logging";

    /**
     * Classloader mode used when deploying/starting the deployable(s).
     * Possible values are PARENT_FIRST and PARENT_LAST
     *
     * Specifies whether the classes are first loaded from the container
     * and only after that from the deployable (PARENT_FIRST) or the
     * other way around - first from the deployable and then from the
     * container (PARENT_LAST).
     *
     */
    String CLASSLOADER_MODE = "cargo.websphere.classloader.mode";

    /**
     * Classloader policy used when deploying/starting the deployable(s).
     * Possible values are MULTIPLE (default) and SINGLE.
     *
     * Specifies whether there is one classloader for all war files
     * in the application or separate classloader for each war.
     *
     */
    String WAR_CLASSLOADER_POLICY = "cargo.websphere.war.classloader.policy";

    /**
     * JVM arguments and system properties are permanently stored within a profiles configuration.
     * To avoid overwriting configuration of existing profiles, set this property accordingly. Has
     * no effect when used on standalone configuration.<br>
     * <br>
     * Allowed values are:<br>
     * <ul>
     *     <li>ALL: Default value. Both JVM args and system properties will be overwritten.</li>
     *     <li>JVM: Only JVM values (initialHeapSize, maximumHeapSize, genericJvmArguments) get
     *     changed.</li>
     *     <li>SystemProperties: Original system properties get removed, and replaced by those
     *     defined within the container.</li>
     *     <li>NONE: Existing profile stays unchanged. Provided system properties and JVM arguments
     *     get ignored.</li>
     * </ul>
     *
     */
    String OVERWRITE_EXISTING_CONFIGURATION = "cargo.websphere.overwriteExistingConfiguration";

    /**
     * WebSphere JMS SIBus.
     */
    String JMS_SIBUS = "cargo.websphere.jms.sibus";

    /**
     * WebSphere binding of EJB to activation specification.<br>
     * <br>
     * Example:<br>
     *          deployable name:EJB name:queue jndi name|<br>
     *          deployable name:another EJB name:another queue jndi name<br>
     * <br>
     * Used for mapping of queues to their appropriate messaging EJBs.
     */
    String EJB_TO_ACT_SPEC_BINDING = "cargo.websphere.ejb.act.binding";

    /**
     * WebSphere binding of EJB to resource reference.<br>
     * <br>
     * Example:<br>
     * deployable name:EJB name:EBJ resource name:resource jndi name|<br>
     * deployable name:another EJB name:EBJ resource2 name:resource2 jndi name<br>
     * <br>
     * Used for mapping of resources like JMS factories to their appropriate EJBs references.<br>
     * Can be used to map resources in EBJ annotated with @Resource.
     */
    String EJB_TO_RES_REF_BINDING = "cargo.websphere.ejb.res.binding";

    /**
     * Whether to enable or disable application security for deployed applications.
     * Possible values: true or false.
     */
    String APPLICATION_SECURITY = "cargo.websphere.security.application";
}
