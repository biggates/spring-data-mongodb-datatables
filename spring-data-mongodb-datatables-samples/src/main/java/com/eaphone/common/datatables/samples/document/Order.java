package com.eaphone.common.datatables.samples.document;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

/**
 * Sample model
 *
 * @author Damien Arrachequesne
 */
@Data
@Document(collection = "order")
public class Order {

    @Id
    @GeneratedValue
    @JsonView(DataTablesOutput.View.class)
    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonView(DataTablesOutput.View.class)
    private Date date;

    @JsonView(DataTablesOutput.View.class)
    private String orderNumber;

    @JsonView(DataTablesOutput.View.class)
    private boolean isValid;

    @JsonView(DataTablesOutput.View.class)
    private int amount;

    @JsonView(DataTablesOutput.View.class)
    private double price;

    public static Order random() {
        Order o = new Order();
        Random r = new Random();

        Calendar c = Calendar.getInstance();
        c.set(2005 + r.nextInt(10), r.nextInt(12), r.nextInt(28), r.nextInt(24), r.nextInt(59), r.nextInt(59));
        o.date = c.getTime();

        o.orderNumber = String.format("O%05d", r.nextInt(99999));
        
        o.isValid = r.nextBoolean();
        
        o.amount = r.nextInt(25);
        
        o.price = Math.round( 100.0 * 100.0 * r.nextDouble() ) / 100.0;
        
        return o;
    }
}
