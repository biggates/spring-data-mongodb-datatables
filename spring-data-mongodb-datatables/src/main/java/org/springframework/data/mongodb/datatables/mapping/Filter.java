package org.springframework.data.mongodb.datatables.mapping;

import lombok.Data;

@Data
public class Filter {
    private String gt;
    private String gte;
    private String lt;
    private String lte;
    private String eq;
    private String ne;
    private String in;
    private String nin;
    private String regex;
    private Boolean exists;
    private Boolean isNull;
    private Boolean isEmpty;
}
