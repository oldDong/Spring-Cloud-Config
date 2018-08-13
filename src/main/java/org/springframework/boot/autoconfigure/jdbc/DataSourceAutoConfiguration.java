package org.springframework.boot.autoconfigure.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceInitializerPostProcessor.Registrar;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.sql.SQLException;

@Configuration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({ Registrar.class, DataSourcePoolMetadataProvidersConfiguration.class })
@Profile("db")
public class DataSourceAutoConfiguration {

	private static final Log logger = LogFactory
			.getLog(DataSourceAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public DataSourceInitializer dataSourceInitializer(DataSourceProperties properties,
			ApplicationContext applicationContext) {
		return new DataSourceInitializer(properties, applicationContext);
	}

	/**
	 * Determines if the {@code dataSource} being used by Spring was created from
	 * {@link EmbeddedDataSourceConfiguration}.
	 * @param beanFactory the bean factory
	 * @return true if the data source was auto-configured.
	 */
	public static boolean containsAutoConfiguredDataSource(
			ConfigurableListableBeanFactory beanFactory) {
		try {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition("dataSource");
			return EmbeddedDataSourceConfiguration.class.getName()
					.equals(beanDefinition.getFactoryBeanName());
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	@Configuration
	@Conditional(EmbeddedDatabaseCondition.class)
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	@Import(EmbeddedDataSourceConfiguration.class)
	protected static class EmbeddedDatabaseConfiguration {

	}

	@Configuration
	@Conditional(PooledDataSourceCondition.class)
	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
	@Import({ DataSourceConfiguration.Tomcat.class, DataSourceConfiguration.Hikari.class,
			DataSourceConfiguration.Dbcp.class, DataSourceConfiguration.Dbcp2.class,
			DataSourceConfiguration.Generic.class })
	@SuppressWarnings("deprecation")
	protected static class PooledDataSourceConfiguration {

	}

	@Configuration
	@ConditionalOnProperty(prefix = "spring.datasource", name = "jmx-enabled")
	@ConditionalOnClass(name = "org.apache.tomcat.jdbc.pool.DataSourceProxy")
	@Conditional(DataSourceAutoConfiguration.DataSourceAvailableCondition.class)
	@ConditionalOnMissingBean(name = "dataSourceMBean")
	protected static class TomcatDataSourceJmxConfiguration {

		@Bean
		public Object dataSourceMBean(DataSource dataSource) {
			if (dataSource instanceof DataSourceProxy) {
				try {
					return ((DataSourceProxy) dataSource).createPool().getJmxPool();
				}
				catch (SQLException ex) {
					logger.warn("Cannot expose DataSource to JMX (could not connect)");
				}
			}
			return null;
		}

	}

	/**
	 * {@link AnyNestedCondition} that checks that either {@code spring.datasource.type}
	 * is set or {@link PooledDataSourceAvailableCondition} applies.
	 */
	static class PooledDataSourceCondition extends AnyNestedCondition {

		PooledDataSourceCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "spring.datasource", name = "type")
		static class ExplicitType {

		}

		@Conditional(PooledDataSourceAvailableCondition.class)
		static class PooledDataSourceAvailable {

		}

	}

	/**
	 * {@link Condition} to test if a supported connection pool is available.
	 */
	static class PooledDataSourceAvailableCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("PooledDataSource");
			if (getDataSourceClassLoader(context) != null) {
				return ConditionOutcome
						.match(message.foundExactly("supported DataSource"));
			}
			return ConditionOutcome
					.noMatch(message.didNotFind("supported DataSource").atAll());
		}

		/**
		 * Returns the class loader for the {@link DataSource} class. Used to ensure that
		 * the driver class can actually be loaded by the data source.
		 * @param context the condition context
		 * @return the class loader
		 */
		private ClassLoader getDataSourceClassLoader(ConditionContext context) {
			Class<?> dataSourceClass = new DataSourceBuilder(context.getClassLoader())
					.findType();
			return (dataSourceClass == null ? null : dataSourceClass.getClassLoader());
		}

	}

	/**
	 * {@link Condition} to detect when an embedded {@link DataSource} type can be used.
	 * If a pooled {@link DataSource} is available, it will always be preferred to an
	 * {@code EmbeddedDatabase}.
	 */
	static class EmbeddedDatabaseCondition extends SpringBootCondition {

		private final SpringBootCondition pooledCondition = new PooledDataSourceCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("EmbeddedDataSource");
			if (anyMatches(context, metadata, this.pooledCondition)) {
				return ConditionOutcome
						.noMatch(message.foundExactly("supported pooled data source"));
			}
			EmbeddedDatabaseType type = EmbeddedDatabaseConnection
					.get(context.getClassLoader()).getType();
			if (type == null) {
				return ConditionOutcome
						.noMatch(message.didNotFind("embedded database").atAll());
			}
			return ConditionOutcome.match(message.found("embedded database").items(type));
		}

	}

	/**
	 * {@link Condition} to detect when a {@link DataSource} is available (either because
	 * the user provided one or because one will be auto-configured).
	 */
	@Order(Ordered.LOWEST_PRECEDENCE - 10)
	static class DataSourceAvailableCondition extends SpringBootCondition {

		private final SpringBootCondition pooledCondition = new PooledDataSourceCondition();

		private final SpringBootCondition embeddedCondition = new EmbeddedDatabaseCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("DataSourceAvailable");
			if (hasBean(context, DataSource.class)
					|| hasBean(context, XADataSource.class)) {
				return ConditionOutcome
						.match(message.foundExactly("existing data source bean"));
			}
			if (anyMatches(context, metadata, this.pooledCondition,
					this.embeddedCondition)) {
				return ConditionOutcome.match(message
						.foundExactly("existing auto-configured data source bean"));
			}
			return ConditionOutcome
					.noMatch(message.didNotFind("any existing data source bean").atAll());
		}

		private boolean hasBean(ConditionContext context, Class<?> type) {
			return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					context.getBeanFactory(), type, true, false).length > 0;
		}

	}

}