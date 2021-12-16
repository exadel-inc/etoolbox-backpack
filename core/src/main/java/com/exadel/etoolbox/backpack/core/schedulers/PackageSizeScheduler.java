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
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules the task for gathering package statistics
 */
@Component(service = Runnable.class,
        immediate = true)
@Designate(ocd = PackageSizeScheduler.Config.class)
public class PackageSizeScheduler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageSizeScheduler.class);

    @Reference
    private Scheduler scheduler;

    @Reference
    private PackageSizeService packageSizeService;

    @Override
    public void run() {
        packageSizeService.calculateAverageSize();
    }

    @Activate
    protected void activate(final Config config) {
        ScheduleOptions scheduleOptions = scheduler.EXPR(config.cronExpression());
        scheduleOptions.name(PackageSizeService.class.getName());
        scheduleOptions.canRunConcurrently(config.isConcurrent());
        scheduler.schedule(this, scheduleOptions);
        LOGGER.info("Package size scheduler activated");
    }

    @Deactivate
    private void deactivate() {
        scheduler.unschedule(PackageSizeService.class.getName());
        LOGGER.info("Package size scheduler unactivated");
    }

    @ObjectClassDefinition(name = "Package size scheduler")
    public static @interface Config {

        @AttributeDefinition(name = "Cron-job expression")
        String cronExpression() default "0 0 12 1/1 * ? *";

        @AttributeDefinition(name = "Concurrent task",
                description = "Whether or not to schedule this task concurrently")
        boolean isConcurrent() default false;
    }
}
