package guru.spaeth.logback;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import ch.qos.logback.classic.LoggerContext;

/**
 * Servlet class to able users to configure log levels.
 * 
 * @author Francisco Spaeth
 * 
 */
public class JMXConfiguratorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final TemplateEngine templateEngine;

	private static final List<String> LOG_LEVELS;

	private static final String CONTEXT_URL_PARAM_NAME = "c";
	private static final String ACTION_URL_PARAM_NAME = "a";
	private static final String LEVEL_URL_PARAM_NAME = "l";
	private static final String NAME_URL_PARAM_NAME = "n";

	static {
		templateEngine = new TemplateEngine();
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setSuffix(".tmpl");
		templateResolver.setPrefix("guru/spaeth/logback/");
		templateEngine.setTemplateResolver(templateResolver);

		LOG_LEVELS = Arrays.asList(new String[] { "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF" });
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			Map<String, ContextEntry> contexts = getContextEntries();
			if (contexts.isEmpty()) {
				templateEngine.process("empty", new WebContext(req, resp, req.getServletContext()), resp.getWriter());
			} else {
				processRequest(req, resp, contexts);
			}

		} catch (Exception e) {

			e.printStackTrace();
			Map<String, ?> variables = Collections.singletonMap("exception", e);
			templateEngine.process("error", new WebContext(req, resp, req.getServletContext(), Locale.getDefault(),
					variables), resp.getWriter());

		}

	}

	private void processRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, ContextEntry> contexts)
			throws AttributeNotFoundException, InstanceNotFoundException, MalformedObjectNameException, MBeanException,
			ReflectionException, IOException {

		ContextEntry selectedContext = getSelectedContext(req, contexts);

		String actionName = req.getParameter(ACTION_URL_PARAM_NAME);
		if (actionName != null) {
			executeAction(selectedContext, actionName, req, resp);
		}

		listLoggers(req, resp, contexts, selectedContext);

	}

	private void executeAction(ContextEntry selectedContext, String actionName, HttpServletRequest req,
			HttpServletResponse resp) throws InstanceNotFoundException, MalformedObjectNameException,
			NullPointerException, ReflectionException, MBeanException, IOException {

		if ("set".equals(actionName)) {

			String name = req.getParameter(NAME_URL_PARAM_NAME);
			String level = req.getParameter(LEVEL_URL_PARAM_NAME);

			if (name == null) {
				throw new IllegalArgumentException("name should not be null");
			}

			if (!LOG_LEVELS.contains(level) && !"NULL".equals(level)) {
				throw new IllegalArgumentException("invalid level was provided: " + level);
			}

			setLogLevel(selectedContext, name, level);

		} else {

			throw new IllegalArgumentException("not able to find action named: " + actionName);

		}

	}

	private void listLoggers(HttpServletRequest req, HttpServletResponse resp, Map<String, ContextEntry> contexts,
			ContextEntry selectedContext) throws AttributeNotFoundException, InstanceNotFoundException,
			MalformedObjectNameException, MBeanException, ReflectionException, IOException {

		final WebContext ctx = new WebContext(req, resp, req.getServletContext());
		ctx.setVariable("selectedContext", selectedContext);
		ctx.setVariable("contexts", contexts.values());
		ctx.setVariable("loggers", getLoggerEntries(selectedContext));
		ctx.setVariable("levels", LOG_LEVELS);

		templateEngine.process("list", ctx, resp.getWriter());
	}

	private ContextEntry getSelectedContext(HttpServletRequest req, Map<String, ContextEntry> contextEntries) {

		if (contextEntries.isEmpty()) {
			return null;
		}

		String contextName = req.getParameter(CONTEXT_URL_PARAM_NAME);

		if (null != contextName && contextEntries.containsKey(contextName)) {
			return contextEntries.get(contextName);
		}

		LoggerContext ctx = (LoggerContext) (LoggerFactory.getILoggerFactory());
		if (contextEntries.containsKey(ctx.getName())) {
			return contextEntries.get(ctx.getName());
		} else {
			return contextEntries.values().iterator().next();
		}

	}

	private List<LoggerEntry> getLoggerEntries(ContextEntry contextEntry) throws AttributeNotFoundException,
			InstanceNotFoundException, MalformedObjectNameException, MBeanException, ReflectionException,
			NullPointerException {

		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(contextEntry.getObjectName());

		Object value = mBeanServer.getAttribute(objectName, "LoggerList");
		List<?> list = (List<?>) value;

		List<LoggerEntry> result = new ArrayList<LoggerEntry>();
		for (Object l : list) {
			String name = String.valueOf(l);
			String level = (String) mBeanServer.invoke(objectName, "getLoggerLevel", new Object[] { name },
					new String[] { "java.lang.String" });
			String effectiveLevel = (String) mBeanServer.invoke(objectName, "getLoggerEffectiveLevel",
					new Object[] { name }, new String[] { "java.lang.String" });
			result.add(new LoggerEntry(name, level, effectiveLevel));
		}

		return result;
	}

	private Map<String, ContextEntry> getContextEntries() throws MalformedObjectNameException, NullPointerException {

		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		Set<ObjectInstance> mBeans = mBeanServer.queryMBeans(new ObjectName(
				"ch.qos.logback.classic:*,Type=ch.qos.logback.classic.jmx.JMXConfigurator"), null);

		Map<String, ContextEntry> result = new HashMap<String, ContextEntry>();
		for (ObjectInstance oi : mBeans) {
			String name = oi.getObjectName().getKeyProperty("Name");
			ContextEntry contextEntry = new ContextEntry(oi.getObjectName().getCanonicalName(), name);
			result.put(name, contextEntry);
		}

		return result;

	}

	private void setLogLevel(ContextEntry context, String name, String level) throws InstanceNotFoundException,
			MalformedObjectNameException, NullPointerException, ReflectionException, MBeanException {

		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(context.getObjectName());
		mBeanServer.invoke(objectName, "setLoggerLevel", new Object[] { name, level }, new String[] {
				"java.lang.String", "java.lang.String" });

	}

}
