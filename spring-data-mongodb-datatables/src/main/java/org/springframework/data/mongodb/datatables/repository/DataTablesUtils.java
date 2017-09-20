package org.springframework.data.mongodb.datatables.repository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.datatables.mapping.Column;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.Search;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.datatables.model.DataTablesCount;
import org.springframework.util.StringUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringExpression;

public class DataTablesUtils {

    private final static String OR_SEPARATOR = "+";

    private final static char ESCAPE_CHAR = '\\';

    public static <T> Query getQuery(String collectionName, final DataTablesInput input) {
        Query q = new Query();
        List<Criteria> criteriaList = getCriteria(input);
        if (criteriaList != null) {
            for (final Criteria c : criteriaList) {
                q.addCriteria(c);
            }
        }
        return q;
    }

    /**
     * Convert a {@link DataTablesInput} to Criteia
     * 
     * @param input
     * @return
     */
    private static List<Criteria> getCriteria(final DataTablesInput input) {
        List<Criteria> result = new LinkedList<>();
        // check for each searchable column whether a filter value exists
        List<Column> columnParametersList = input.getColumns();

        for (Column column : columnParametersList) {
            Search sParameter = column.getSearch();
            if (sParameter == null) {
                continue;
            }
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
                    // the filter contains only one value, add a 'WHERE ... LIKE' clause
                    if (isBoolean(filterValue)) {
                        // boolean type
                        c.is(Boolean.valueOf(filterValue));
                    } else {
                        // mimic "LIKE" clause using $regex
                        c.regex(getLikeFilterPattern(filterValue));
                    }
                }
                result.add(c);
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

        return result;
    }

    /**
     * Experimental support for Querydsl, not verified yet
     * 
     * @param entity
     * @param input
     * @return
     */
    static com.querydsl.core.types.Predicate getPredicate(PathBuilder<?> entity, DataTablesInput input) {

        BooleanBuilder predicate = new BooleanBuilder();
        // check for each searchable column whether a filter value exists
        for (Column column : input.getColumns()) {
            if (column.getSearchable() == null) {
                continue;
            }
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
            if (order.getColumn() != null && input.getColumns().size() > order.getColumn()) {
                Column column = input.getColumns().get(order.getColumn());
                if (column != null && column.getOrderable()) {
                    String sortColumn = column.getData();
                    Direction sortDirection = Direction.fromString(order.getDir());
                    orders.add(new Order(sortDirection, sortColumn));
                }
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

    /**
     * Convert {@link DataTablesInput} to {@link AggregationOperation}[], mainly for column searches.
     * 
     * @param input
     * @return
     */
    private static List<AggregationOperation> toAggregationOperation(DataTablesInput input) {
        List<AggregationOperation> result = new LinkedList<>();
        List<Criteria> criteriaList = getCriteria(input);
        if (criteriaList != null) {
            for (final Criteria c : criteriaList) {
                result.add(match(c));
            }
        }
        return result;
    }

    /**
     * Create an {@link TypedAggregation} with specified {@link DataTablesInput} as filter, plus specified
     * {@link AggregationOperation}[], but only act as <code>$count</code>
     * <p>This basically creates an aggregation pipeline as follows:</p>
     * 
     * <pre>
     * <code>
     * [
     *      ...operations,
     *      {$group: {"_id": null, "_count": {$sum: 1}}}
     * ]
     * </code>
     * </pre>
     * 
     * @param classOfT
     * @param input
     * @param operations
     * @return
     */
    public static TypedAggregation<DataTablesCount> makeAggregationCountOnly(DataTablesInput input,
            AggregationOperation[] operations) {
        List<AggregationOperation> opList = new LinkedList<>();
        if (operations != null) {
            for (int i = 0; i < operations.length; i++) {
                opList.add(operations[i]);
            }
        }

        opList.addAll(toAggregationOperation(input));

        opList.add(group().count().as("_count"));
        return newAggregation(DataTablesCount.class, opList);
    }

    /**
     * Create an {@link TypedAggregation} with specified {@link DataTablesInput} as filter, plus specified
     * {@link AggregationOperation}[]
     * 
     * @param classOfT
     * @param input
     * @param pageable
     * @param operations
     * @return
     */
    public static <T> TypedAggregation<T> makeAggregation(Class<T> classOfT, DataTablesInput input, Pageable pageable,
            AggregationOperation[] operations) {
        List<AggregationOperation> opList = new LinkedList<>();
        if (operations != null) {
            for (int i = 0; i < operations.length; i++) {
                opList.add(operations[i]);
            }
        }

        opList.addAll(toAggregationOperation(input));

        if (pageable != null) {
            final Sort s = pageable.getSort();
            if (s != null) {
                opList.add(sort(s));
            }
            opList.add(skip((long) pageable.getOffset()));
            opList.add(limit(pageable.getPageSize()));
        }
        return newAggregation(classOfT, opList);
    }

}
