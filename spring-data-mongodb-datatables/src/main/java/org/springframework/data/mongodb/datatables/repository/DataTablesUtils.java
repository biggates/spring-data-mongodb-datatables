package org.springframework.data.mongodb.datatables.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.datatables.mapping.Column;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.Search;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringExpression;

public class DataTablesUtils {

    private final static String OR_SEPARATOR = "+";

    private final static String ATTRIBUTE_SEPARATOR = ".";

    private final static char ESCAPE_CHAR = '\\';

    public static <T> Query getQuery(String collectionName, final DataTablesInput input) {

        Query q = new Query();

        // check for each searchable column whether a filter value exists
        List<Column> columnParametersList = input.getColumns();

        for (Column column : columnParametersList) {
            Search sParameter = column.getSearch();
            String filterValue = sParameter.getValue();
            boolean searchAble = column.getSearchable();
            if (searchAble && StringUtils.hasText(filterValue)) {
                Criteria c = Criteria.where(column.getData());

                if (filterValue.contains(OR_SEPARATOR)) {
                    // the filter contains multiple values, add a 'WHERE
                    // .. IN' clause
                    // Note: "\\" is added to escape special character
                    // '+'
                    String[] values = filterValue.split("\\" + OR_SEPARATOR);
                    if ((values.length > 0) && isBoolean(values[0])) {
                        Object[] booleanValues = new Boolean[values.length];
                        for (int i = 0; i < values.length; i++) {
                            booleanValues[i] = Boolean.valueOf(values[i]);
                        }
                        c.in(booleanValues);
                    } else {
                        c.in(Arrays.asList(values));
                    }
                } else {
                    // the filter contains only one value, add a 'WHERE
                    // .. LIKE' clause
                    if (isBoolean(filterValue)) {
                        // 处理 boolean 类型 ("TRUE"/ "FALSE") 不区分大小写
                        c.is(Boolean.valueOf(filterValue));
                    } else {
                        // 改 like 增加and的逻辑q.addCriteria进去
                        c.regex(getLikeFilterPattern(filterValue));// 用regex模糊匹配
                    }
                }
                q.addCriteria(c);
            }
        }

        // check whether a global filter value exists
        // TODO <pre>due to limitations of the BasicDBObject, you can't add a second "$or" expression</pre>
        // this conflicts with additionalCriteria and preFilteringCriteria
        /*
         * String globalFilterValue = input.getSearch().getValue();
         * if (StringUtils.hasText(globalFilterValue)) {
         * 
         * Criteria crit = new Criteria();
         * 
         * // add a 'WHERE .. LIKE' clause on each searchable column
         * for (ColumnParameter column : input.getColumns()) {
         * if (column.getSearchable()) {
         * 
         * Criteria c = Criteria.where(column.getData());
         * c.regex(getLikeFilterPattern(globalFilterValue));
         * 
         * crit.orOperator(c);
         * }
         * }
         * q.addCriteria(crit);
         * }
         */

        return q;
    }

    public static com.querydsl.core.types.Predicate getPredicate(PathBuilder<?> entity, DataTablesInput input) {

        BooleanBuilder predicate = new BooleanBuilder();
        // check for each searchable column whether a filter value exists
        for (Column column : input.getColumns()) {
            String filterValue = column.getSearch().getValue();
            if (column.getSearchable() && StringUtils.hasText(filterValue)) {

                if (filterValue.contains(OR_SEPARATOR)) {
                    // the filter contains multiple values, add a 'WHERE .. IN'
                    // clause
                    // Note: "\\" is added to escape special character '+'
                    String[] values = filterValue.split("\\" + OR_SEPARATOR);
                    if ((values.length > 0) && isBoolean(values[0])) {
                        List<Boolean> booleanValues = new ArrayList<Boolean>();
                        for (String value : values) {
                            booleanValues.add(Boolean.valueOf(value));
                        }
                        predicate = predicate.and(entity.getBoolean(column.getData()).in(booleanValues));
                    } else {
                        predicate.and(getStringExpression(entity, column.getData()).in(values));
                    }
                } else {
                    // the filter contains only one value, add a 'WHERE .. LIKE'
                    // clause
                    if (isBoolean(filterValue)) {
                        predicate = predicate.and(entity.getBoolean(column.getData()).eq(Boolean.valueOf(filterValue)));
                    } else {
                        predicate = predicate.and(getStringExpression(entity, column.getData()).lower()
                                .like(getLikeFilterValue(filterValue), ESCAPE_CHAR));
                    }
                }
            }
        }

        // check whether a global filter value exists
        String globalFilterValue = input.getSearch().getValue();
        if (StringUtils.hasText(globalFilterValue)) {
            BooleanBuilder matchOneColumnPredicate = new BooleanBuilder();
            // add a 'WHERE .. LIKE' clause on each searchable column
            for (Column column : input.getColumns()) {
                if (column.getSearchable()) {
                    matchOneColumnPredicate = matchOneColumnPredicate.or(getStringExpression(entity, column.getData())
                            .lower().like(getLikeFilterValue(globalFilterValue), ESCAPE_CHAR));
                }
            }
            predicate = predicate.and(matchOneColumnPredicate);
        }
        return predicate;
    }

    /**
     * Creates a 'LIMIT .. OFFSET .. ORDER BY ..' clause for the given {@link DataTablesInput}.
     * 
     * @param input the {@link DataTablesInput} mapped from the Ajax request
     * @return a {@link Pageable}, must not be {@literal null}.
     */
    public static Pageable getPageable(DataTablesInput input) {
        List<Order> orders = new ArrayList<Order>();
        for (org.springframework.data.jpa.datatables.mapping.Order order : input.getOrder()) {
            Column column = input.getColumns().get(order.getColumn());
            if (column.getOrderable()) {
                String sortColumn = column.getData();
                Direction sortDirection = Direction.fromString(order.getDir());
                orders.add(new Order(sortDirection, sortColumn));
            }
        }
        Sort sort = orders.isEmpty() ? null : new Sort(orders);

        if (input.getLength() == -1) {
            input.setStart(0);
            input.setLength(Integer.MAX_VALUE);
        }
        return new DataTablesPageRequest(input.getStart(), input.getLength(), sort);
    }

    private static boolean isBoolean(String filterValue) {
        return "TRUE".equalsIgnoreCase(filterValue) || "FALSE".equalsIgnoreCase(filterValue);
    }

    /**
     * 将用户的输入转换为搜索用的正则表达式
     * 
     * @param filterValue
     * @return
     */
    private static Pattern getLikeFilterPattern(String filterValue) {
        return Pattern.compile(filterValue, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    }

    @Deprecated
    private static String getLikeFilterValue(String filterValue) {
        return "%" + filterValue.toLowerCase().replaceAll("%", "\\\\" + "%").replaceAll("_", "\\\\" + "_") + "%";
    }

    private static StringExpression getStringExpression(PathBuilder<?> entity, String columnData) {
        return Expressions.stringOperation(Ops.STRING_CAST, entity.get(columnData));
    }

    private static class DataTablesPageRequest implements Pageable {

        private final int offset;
        private final int pageSize;
        private final Sort sort;

        public DataTablesPageRequest(int offset, int pageSize, Sort sort) {
            this.offset = offset;
            this.pageSize = pageSize;
            this.sort = sort;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public int getPageSize() {
            return pageSize;
        }

        @Override
        public Sort getSort() {
            return sort;
        }

        @Override
        public Pageable next() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Pageable previousOrFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Pageable first() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasPrevious() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getPageNumber() {
            throw new UnsupportedOperationException();
        }
    }

}
