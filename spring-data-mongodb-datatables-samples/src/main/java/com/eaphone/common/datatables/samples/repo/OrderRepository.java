package com.eaphone.common.datatables.samples.repo;

import org.springframework.data.mongodb.datatables.repository.DataTablesRepository;

import com.eaphone.common.datatables.samples.document.Order;

/**
 * User repository extending {@link DataTablesRepository}
 *
 * @author Damien Arrachequesne
 */
public interface OrderRepository extends DataTablesRepository<Order, Integer> {

}
