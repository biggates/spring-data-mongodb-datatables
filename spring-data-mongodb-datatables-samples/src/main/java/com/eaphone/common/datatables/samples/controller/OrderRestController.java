package com.eaphone.common.datatables.samples.controller;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eaphone.common.datatables.samples.document.Order;
import com.eaphone.common.datatables.samples.repo.OrderRepository;
import com.eaphone.common.datatables.samples.support.SpringDateFormatter;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller returning {@link DataTablesOutput}
 *
 * @author biggates2010
 */
@RestController
@Slf4j
public class OrderRestController {

    @Autowired
    private OrderRepository repo;

    @JsonView(DataTablesOutput.View.class)
    @RequestMapping(value = "/data/orders", method = RequestMethod.GET)
    public DataTablesOutput<Order> getOrders(@Valid DataTablesInput input,
            @RequestParam(required = false) Date startDate, @RequestParam(required = false) Date endDate) {
        boolean preFiltering = false;
        Criteria crit = Criteria.where("date");

        if (startDate != null) {
            preFiltering = true;
            crit.gte(startDate);
        }
        if (endDate != null) {
            preFiltering = true;
            crit.lt(endDate);
        }

        if (preFiltering) {
            return repo.findAll(input, crit);
        } else {
            return repo.findAll(input);
        }
    }

    /**
     * Insert some data to Fongo
     */
    @PostConstruct
    public void insertSampleData() {
        log.debug("initializing default data...");

        // some random orders
        for (int i = 0; i < 200; i++) {
            Order o = Order.random();

            repo.save(o);
        }

        // some orders with specific values
        Order o = Order.random();
        o.setOrderNumber("O10001");
        repo.save(o);

        o = Order.random();
        o.setOrderNumber("O10002");
        repo.save(o);

        log.debug("default data successfully initialized.");
    }

    /**
     * 为 param 添加常用的日期格式
     *
     * @param binder
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.addCustomFormatter(new SpringDateFormatter());
    }
}
