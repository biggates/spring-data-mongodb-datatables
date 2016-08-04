package org.springframework.data.mongodb.datatables.repository;

import java.io.Serializable;

import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface DataTablesRepository<T, ID extends Serializable>
extends PagingAndSortingRepository<T, ID> {

	/**
	 * Returns the filtered list for the given {@link DataTablesInput}.
	 *
	 * @param input
	 *            the {@link DataTablesInput} mapped from the Ajax request
	 * @return a {@link DataTablesOutput}
	 */
	DataTablesOutput<T> findAll(DataTablesInput input);

	/**
	 * Returns the filtered list for the given {@link DataTablesInput}.
	 *
	 * @param input
	 *            the {@link DataTablesInput} mapped from the Ajax request
	 * @param additionalSpecification
	 *            an additional {@link Specification} to apply to the query
	 *            (with an "AND" clause)
	 * @return a {@link DataTablesOutput}
	 */
	DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria);

	/**
	 * Returns the filtered list for the given {@link DataTablesInput}.
	 *
	 * @param input
	 *            the {@link DataTablesInput} mapped from the Ajax request
	 * @param additionalSpecification
	 *            an additional {@link Specification} to apply to the query
	 *            (with an "AND" clause)
	 * @param preFilteringSpecification
	 *            a pre-filtering {@link Specification} to apply to the query
	 *            (with an "AND" clause)
	 * @return a {@link DataTablesOutput}
	 */
	//	DataTablesOutput<T> findAll(DataTablesInput input, Query additionalQuery,
	//			Query preFilteringQuery);
	DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria, Criteria preFilteringCriteria);

}
