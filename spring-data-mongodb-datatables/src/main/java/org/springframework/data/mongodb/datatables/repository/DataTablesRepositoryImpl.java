package org.springframework.data.mongodb.datatables.repository;

import static org.springframework.data.mongodb.core.query.Query.*;

import java.io.Serializable;
import java.util.Collections;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;

/**
 * Repository implementation
 *
 * @author Damien Arrachequesne
 */
public class DataTablesRepositoryImpl<T, ID extends Serializable> extends SimpleMongoRepository<T, ID>
implements DataTablesRepository<T, ID> {

	private final MongoEntityInformation<T, ID> entityInformation;
	private final MongoOperations mongoOperations;

	public DataTablesRepositoryImpl(MongoEntityInformation<T, ID> metadata, MongoOperations mongoOperations) {
		super(metadata, mongoOperations);
		this.entityInformation = metadata;
		this.mongoOperations = mongoOperations;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mongodb.datatables.repository.DataTablesRepository#findAll(org.springframework.data.jpa.datatables.mapping.DataTablesInput)
	 */
	@Override
	public DataTablesOutput<T> findAll(DataTablesInput input) {
		return findAll(input, null, null);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mongodb.datatables.repository.DataTablesRepository#findAll(org.springframework.data.jpa.datatables.mapping.DataTablesInput, org.springframework.data.mongodb.core.query.Criteria)
	 */
	@Override
	public DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria) {
		return findAll(input, additionalCriteria, null);
	}

	private long count(Criteria crit) {
		Query q = query(crit);
		return this.mongoOperations.count(q, this.entityInformation.getCollectionName());
	}

	private <S extends T> Page<S> findAll(Query q, Pageable p, Class<S> classOfS) {
		q.with(p);

		long count = mongoOperations.count(q, this.entityInformation.getCollectionName());

		if (count == 0) {
			return new PageImpl<S>(Collections.<S>emptyList());
		}

		return new PageImpl<S>(mongoOperations.find(q, classOfS, this.entityInformation.getCollectionName()), p, count);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mongodb.datatables.repository.DataTablesRepository#findAll(org.springframework.data.jpa.datatables.mapping.DataTablesInput, org.springframework.data.mongodb.core.query.Criteria, org.springframework.data.mongodb.core.query.Criteria)
	 */
	@Override
	public DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCrit, Criteria preFilteringCrit) {
		DataTablesOutput<T> output = new DataTablesOutput<T>();//date类型在additionalCrit中
		output.setDraw(input.getDraw());

		try {
			long recordsTotal = preFilteringCrit == null ? count() : count(preFilteringCrit);
			if (recordsTotal == 0) {
				return output;
			}
			output.setRecordsTotal(recordsTotal);

			Query query = DataTablesUtils.getQuery(this.entityInformation.getCollectionName(), input);
			if (additionalCrit != null) {
				query.addCriteria(additionalCrit);
			}

			if (preFilteringCrit != null) {
				query.addCriteria(preFilteringCrit);
			}

			Pageable pageable = DataTablesUtils.getPageable(input);

			Page<T> data = findAll(query, pageable, this.entityInformation.getJavaType());

			output.setData(data.getContent());
			output.setRecordsFiltered(data.getTotalElements());

		} catch (Exception e) {
			output.setError(e.toString());
			output.setRecordsFiltered(0L);
		}

		return output;
	}

}
