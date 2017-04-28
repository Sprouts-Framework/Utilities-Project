/*
 * PopulateDatabase.java 
 * Copyright (C) 2013 Universidad de Sevilla
 * 
 * The use of this project is hereby constrained to the conditions of the 
 * TDG Licence, a copy of which you may download from 
 * http://www.tdg-seville.info/License.html
 */

package utilities;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.sql.DataSource;

import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import es.us.lsi.dp.domain.DomainEntity;

@SuppressWarnings("deprecation")
public class PopulateDatabase {

	public static void main(String[] args) {

		ApplicationContext dataSourceContext, populationContext;
		EntityManagerFactory entityManagerFactory;
		EntityManager entityManager;
		EntityTransaction entityTransaction;

		entityManagerFactory = null;
		entityManager = null;

		try {
			System.out.printf("PopulateDatabase 1.4%n");
			System.out.printf("----------------------------------%n%n");

			System.setProperty("spring.profiles.active", "population");

			System.out.println("----------------------------------");
			System.out.println("Reading datasource context...");
			System.out.println("----------------------------------\n");
			dataSourceContext = new ClassPathXmlApplicationContext(
					"classpath:spring/datasource.xml");

			System.out.println("\n----------------------------------");
			System.out.println("Reading PopulateDatabase.xml file...");
			System.out.println("----------------------------------\n");
			populationContext = new ClassPathXmlApplicationContext(
					"classpath:populateDatabase.xml");

			initialiseDatabase(dataSourceContext);

			entityManagerFactory = (EntityManagerFactory) dataSourceContext
					.getBean("entityManagerFactory");
			entityManager = entityManagerFactory.createEntityManager();
			entityTransaction = entityManager.getTransaction();

			System.out.println("\n----------------------------------");
			System.out.printf("Persisting %d entities...%n",
					populationContext.getBeanDefinitionCount());
			System.out.println("----------------------------------\n");

			entityTransaction.begin();
			for (Entry<String, Object> entry : populationContext
					.getBeansWithAnnotation(Entity.class).entrySet()) {
				String beanName;
				DomainEntity entity;

				beanName = entry.getKey();
				entity = (DomainEntity) entry.getValue();
				System.out.printf("> %s: %s", beanName, entity.getClass()
						.getName());
				entityManager.persist(entity);
				System.out.printf(" -> id = %d, version = %d%n",
						entity.getId(), entity.getVersion());
			}
			entityTransaction.commit();

			System.out.println("\n----------------------------------");
			System.out.println("The database has been successfully populated");
			System.out.println("----------------------------------\n");

		} catch (Throwable oops) {
			oops.printStackTrace();
		} finally {
			if (entityManager != null && entityManager.isOpen())
				entityManager.close();
			if (entityManager != null && entityManagerFactory.isOpen())
				entityManagerFactory.close();
		}
	}

	private static void initialiseDatabase(ApplicationContext appContext)
			throws SQLException {
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean;
		Map<String, Object> jpaPropertyMap;
		Configuration configuration;
		ServiceRegistry serviceRegistry;
		Dialect dialect;
		String[] statements;
		DataSource datasource;
		Connection connection;
		Statement statement;
		String dbName;

		entityManagerFactoryBean = (LocalContainerEntityManagerFactoryBean) appContext
				.getBean("&entityManagerFactory");
		datasource = (DataSource) appContext.getBean("dataSource");

		jpaPropertyMap = entityManagerFactoryBean.getJpaPropertyMap();
		configuration = buildConfiguration(entityManagerFactoryBean
				.getNativeEntityManagerFactory());
		serviceRegistry = (StandardServiceRegistryImpl) new ServiceRegistryBuilder()
				.applySettings(jpaPropertyMap).buildServiceRegistry();
		dialect = serviceRegistry.getService(JdbcServices.class).getDialect();
		statements = configuration.generateSchemaCreationScript(dialect);

		connection = datasource.getConnection();
		statement = connection.createStatement();
		dbName = System.getenv("JPA_DB_NAME");

		statement.addBatch(String.format("drop database if exists `%s`;",
				dbName));
		statement.addBatch(String.format("create database `%s`;", dbName));
		statement.addBatch(String.format("use `%s`;", dbName));

		for (String line : statements) {
			statement.addBatch(line);
		}

		statement.executeBatch();
	}

	private static Configuration buildConfiguration(
			EntityManagerFactory entityManagerFactory) {
		Configuration result;
		Metamodel metamodel;
		Collection<EntityType<?>> entities;
		Collection<EmbeddableType<?>> embeddables;

		result = new Configuration();
		metamodel = entityManagerFactory.getMetamodel();

		entities = metamodel.getEntities();
		for (EntityType<?> entity : entities)
			result.addAnnotatedClass(entity.getJavaType());

		embeddables = metamodel.getEmbeddables();
		for (EmbeddableType<?> embeddable : embeddables)
			result.addAnnotatedClass(embeddable.getJavaType());

		return result;
	}

}
