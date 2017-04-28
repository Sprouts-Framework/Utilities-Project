package utilities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import utilities.internal.ConsoleReader;
import utilities.internal.CustomToStringBuilder;

public class Search {

	/**
	 * @param args
	 * @throws InterruptedException
	 */

	// Configuration ------------------------------------------

	public static Class<?> clazz = null; // example Gym.class;
	public static String[] fields = { null };// example
																				// {"name",
																				// "description"};
	// Write here custom queries to filter your query
	public static String query = ""; // example {cancelled:false}
	// --------------------------------------------------------

	public static EntityManager em = null;

	public static void main(String[] args) throws Throwable {
		System.out.printf("Search 1.1%n");
		System.out.println("Creating entity manager...");
		createEntityManager();

		ConsoleReader consoleReader = new ConsoleReader();
		String consoleInput = null;

		while (true) {
			System.out
					.println("\n\nPlease, enter your search (To exit type 'X')");
			consoleInput = consoleReader.readLine();
			if ("X".equalsIgnoreCase(consoleInput)) {
				em.close();
				System.out.println("End");
				System.exit(0);
			}

			List<?> result = search(consoleInput);
			System.out
					.println(">>>>>>Results found for '" + consoleInput + "'");
			printResultList(result);
		}
	}

	private static void printResultList(List<?> objects) {
		String text;
		Object obj;

		for (int i = 0; i < objects.size(); i++) {
			obj = objects.get(i);
			text = CustomToStringBuilder.toString(obj);
			System.out.printf("Object #%d = %s %n", i, text);
		}
	}

	private static void createEntityManager() {
		EntityManagerFactory entityManagerFactory;
		ApplicationContext dataSourceContext;

		dataSourceContext = new ClassPathXmlApplicationContext(
				"classpath:spring/datasource.xml");
		entityManagerFactory = (EntityManagerFactory) dataSourceContext
				.getBean("entityManagerFactory");

		em = entityManagerFactory.createEntityManager();

	}

	private static List<?> search(String toSearch) throws InterruptedException {
		if (!toSearch.isEmpty()) {
			org.apache.lucene.search.Query luceneQuery = null;
			org.apache.lucene.search.Query luceneQueryToSearch = null;
			org.apache.lucene.search.Query luceneQueryFilter = null;

			Session session;
			session = em.unwrap(Session.class);
			FullTextSession fullTextSession = org.hibernate.search.Search
					.getFullTextSession(session);
			SearchFactory searchFactory = fullTextSession.getSearchFactory();
			org.apache.lucene.queryparser.classic.QueryParser parser = new QueryParser(
					fields[0], searchFactory.getAnalyzer(clazz));

			QueryBuilder qb = fullTextSession.getSearchFactory()
					.buildQueryBuilder().forEntity(clazz).get();
			TermMatchingContext luceneQueryAux = qb.keyword().onFields(
					fields[0]);
			for (String field : fields) {
				luceneQueryAux.andField(field);
			}

			try {
				luceneQueryFilter = parser.parse(query);
			} catch (Throwable e) {
				// handle parsing failure
			}

			luceneQueryToSearch = luceneQueryAux.matching(toSearch)
					.createQuery();

			luceneQuery = qb.bool().must(luceneQueryFilter)
					.must(luceneQueryToSearch).createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
					luceneQuery, clazz);
			int totalNumber = fullTextQuery.list().size();

			List<?> result = fullTextQuery.list();
			System.out.println("total numer: " + totalNumber);
			// em.getTransaction().commit();
			return result;
		} else {
			return new ArrayList<Object>();
		}
	}

	// private static org.apache.lucene.search.Query fullTextCustomQueryBuilder(
	// FullTextCustomQuery customQuery,
	// org.apache.lucene.search.Query query, QueryBuilder qb) {
	// org.apache.lucene.search.Query result;
	// result = query;
	// if (customQuery.getConstraint().equals(FullTextConstraint.EQUALS)) {
	// result = qb
	// .bool()
	// .must(result)
	// .must(qb.keyword().onField(customQuery.getField())
	// .ignoreAnalyzer().matching(customQuery.getObject())
	// .createQuery()).createQuery();
	//
	// } else if (customQuery.getConstraint().equals(
	// FullTextConstraint.GREATER_OR_EQUALS_THAN)) {
	// result = qb
	// .bool()
	// .must(result)
	// .must(qb.range().onField(customQuery.getField())
	// .above(customQuery.getObject()).createQuery())
	// .createQuery();
	//
	// } else if (customQuery.getConstraint().equals(
	// FullTextConstraint.GREATER_THAN)) {
	// result = qb
	// .bool()
	// .must(result)
	// .must(qb.range().onField(customQuery.getField())
	// .above(customQuery.getObject()).excludeLimit()
	// .createQuery()).createQuery();
	//
	// } else if (customQuery.getConstraint().equals(
	// FullTextConstraint.LOWER_THAN)) {
	// result = qb
	// .bool()
	// .must(result)
	// .must(qb.range().onField(customQuery.getField())
	// .below(customQuery.getObject()).excludeLimit()
	// .createQuery()).createQuery();
	//
	// } else if (customQuery.getConstraint().equals(
	// FullTextConstraint.LOWER_OR_EQUALS_THAN)) {
	// result = qb
	// .bool()
	// .must(result)
	// .must(qb.range().onField(customQuery.getField())
	// .below(customQuery.getObject()).createQuery())
	// .createQuery();
	// }
	// return result;
	// }

}
