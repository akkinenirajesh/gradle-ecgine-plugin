package org.ecgine.gradle

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.osgi.OsgiPlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 */
class EcginePlugin implements Plugin<Project> {

    Project project;

    @Override
    void apply(final Project project) {
        this.project = project;

        // We need the node plugin to run and install the react-tools.
        project.plugins.apply(JavaPlugin.class)
        project.plugins.apply(EclipsePlugin.class)
        project.plugins.apply(OsgiPlugin.class)

        // Add the install task to this project.
        //project.tasks.create(ReactInstallTask.NAME, ReactInstallTask.class )

        // replace the jar task to this project.
        project.tasks.remove(project.tasks.getByName('jar'))
        project.tasks.create('jar', EcgineJarTask.class)
        // adding the task to the extra properties makes it available as task type in this project.
        //addGlobalTaskType(JSXTask.class)

        // create the extension to configure the tasks
        project.extensions.create(EcgineExtension.NAME, EcgineExtension.class)
        // when the project was evaluated and before any task is running we are able to configure the tasks
        project.afterEvaluate {
            //project.tasks.getByName(JSXTask.NAME).updateSettings()
        }
    }

    private void addGlobalTaskType( Class type ) {
        this.project.extensions.extraProperties.set( type.getSimpleName(), type )
    }
}
