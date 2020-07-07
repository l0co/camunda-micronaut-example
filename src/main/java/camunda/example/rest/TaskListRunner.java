package camunda.example.rest;

import com.kpavlov.netty.jaxrs.jersey.JaxrsNettyServer;
import io.micronaut.context.annotation.Context;
import org.camunda.bpm.engine.rest.impl.CamundaRestResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.Application;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Lukasz Frankowski
 */
@Context
public class TaskListRunner {

	public static final Logger logger = LoggerFactory.getLogger(TaskListRunner.class);

	protected JaxrsNettyServer server;

	@PostConstruct
	protected void start() {
		logger.debug("Starting tasklist...");

		server = new JaxrsNettyServer(
			"localhost", 8081, new CamundaRestApplication()
		);
		server.start();
	}

	@PreDestroy
	protected void stop() {
		logger.debug("Stopping tasklist...");

		server.stop();
	}

	private static class CamundaRestApplication extends Application {

		protected Set<Class<?>> classes = new LinkedHashSet<>();

		public CamundaRestApplication() {
			classes.addAll(CamundaRestResources.getConfigurationClasses());
			classes.addAll(CamundaRestResources.getResourceClasses());
		}

		@Override
		public Set<Class<?>> getClasses() {
			return classes;
		}
		
	}

}
