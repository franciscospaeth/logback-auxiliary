logback-auxiliary
=================

Auxiliary tools for easy [logback](https://github.com/qos-ch/logback) configuration.

Web Interface Log Level Configuration
-------------------------------------

In order to enable a web interface to configure log levels the following code can be added to your web.xml file:

	<servlet>
		<servlet-name>logback-jmx-configurator-servlet</servlet-name>
		<servlet-class>guru.spaeth.logback.JMXConfiguratorServlet</servlet-class>
	</servlet>

	<servlet-mapping>
		<servlet-name>logback-jmx-configurator-servlet</servlet-name>
		<url-pattern>/logback-configurator</url-pattern>
	</servlet-mapping>
	
Accessing the servlet using the mapping declared you will have access to a web interface to configure the different logback contexts within your JMX registry.
