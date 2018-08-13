package org.springframework.cloud.config.server.environment;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.support.AbstractScmAccessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;

public abstract class AbstractScmEnvironmentRepository extends AbstractScmAccessor
		implements EnvironmentRepository, SearchPathLocator, Ordered {

	private EnvironmentCleaner cleaner = new EnvironmentCleaner();
	private int order = Ordered.LOWEST_PRECEDENCE;

	public AbstractScmEnvironmentRepository(ConfigurableEnvironment environment) {
		super(environment);
	}

	@Override
	public synchronized Environment findOne(String application, String profile, String label) {
		NativeEnvironmentRepository delegate = new NativeEnvironmentRepository(
				getEnvironment());
		Locations locations = getLocations(application, profile, label);
		delegate.setSearchLocations(locations.getLocations());

		//重写取值逻辑
		Environment result = delegate.findOne(application, profile, "");
		Environment resultBase = delegate.findOne("base", "base", "");
		List<PropertySource> sources = result.getPropertySources();
		List<PropertySource> sourcesBase = resultBase.getPropertySources();
		sources.addAll(sourcesBase);

		result.setVersion(locations.getVersion());
		result.setLabel(label);
		return this.cleaner.clean(result, getWorkingDirectory().toURI().toString(),
				getUri());
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}