package com.eaphone.common.datatables.samples.repo;

import org.springframework.data.mongodb.datatables.repository.DataTablesRepository;
import org.springframework.stereotype.Repository;

import com.eaphone.common.datatables.samples.document.Order;

/**
 * User repository extending {@link DataTablesRepository}
 *
 * @author Xiaoyu Guo
 */
@Repository
public interface OrderRepository extends DataTablesRepository<Order, String> {

}
