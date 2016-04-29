package org.ecgine.gradle

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.osgi.OsgiPlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification


class EcginePluginSpec extends Specification {

  def "applying the plugin replaces jar task to project"() {
    given:
    def project = ProjectBuilder.builder().build()

    when:
    project.apply plugin: 'org.ecgine.gradle'

    then:
    project.tasks.jar instanceof EcgineJarTask
  }

  def "applying the plugin applies the java, maven, osgi, eclipse plugins to the project"() {
    given:
    def project = ProjectBuilder.builder().build()

    when:
    project.apply plugin: 'org.ecgine.gradle'

    then:
    project.plugins.hasPlugin(JavaPlugin.class)
    project.plugins.hasPlugin(OsgiPlugin.class)
    project.plugins.hasPlugin(EclipsePlugin.class)
  }

  def "applying the plugin add the extension to the project"() {
    given:
    def project = ProjectBuilder.builder().build()

    when:
    project.apply plugin: 'org.ecgine.gradle'

    then:
    null != project.extensions.findByName('ecgine')
    null != project.extensions.findByType(EcgineExtension.class)
  }

}
