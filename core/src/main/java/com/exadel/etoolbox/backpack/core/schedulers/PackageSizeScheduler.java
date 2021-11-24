/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exadel.etoolbox.backpack.core.schedulers;

import com.exadel.etoolbox.backpack.core.services.pckg.PackageSizeService;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = PackageSizeScheduler.Config.class)
@Component(service = Runnable.class)
public class PackageSizeScheduler implements Runnable {

    @ObjectClassDefinition(name = "Package Size Scheduler",
            description = "Scheduler to define size rate")
    public static @interface Config {

        @AttributeDefinition(name = "Cron-job expression")
        String scheduler_expression() default "0 0 12 1/1 * ? *";

        @AttributeDefinition(name = "Concurrent task",
                description = "Whether or not to schedule this task concurrently")
        boolean scheduler_concurrent() default true;
    }

    @Reference
    private Scheduler scheduler;

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageSizeScheduler.class);

    @Reference
    private PackageSizeService packageSizeService;



    @Activate
    protected void activate(final Config config) {
    }

    @Override
    public void run() {
        packageSizeService.calculateAverageSize();
    }


}
