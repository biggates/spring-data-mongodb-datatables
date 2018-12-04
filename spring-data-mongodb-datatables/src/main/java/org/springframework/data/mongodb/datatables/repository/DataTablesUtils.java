package org.springframework.data.mongodb.datatables.repository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.reflections.ReflectionUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.datatables.mapping.Column;
import org.springframework.data.mongodb.datatables.mapping.ColumnType;
import org.springframework.data.mongodb.datatables.mapping.DataTablesInput;
import org.springframework.data.mongodb.datatables.mapping.Filter;
import org.springframework.data.mongodb.datatables.mapping.Search;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataTablesUtils {

    private static final String COMMA = ",";

    /**
     * check jackson at startup
     */
    private static boolean IS_JACKSON_AVAILABLE = false;
    static {
        try {
            Class.forName("com.fasterxml.jackson.annotation.JsonProperty");
            IS_JACKSON_AVAILABLE = true;
        } catch (ClassNotFoundException cnfe) {
        }
    }

    public static <T, ID extends Serializable> Query getQuery(MongoEntityInformation<T, ID> entityInformation,
            final DataTablesInput input) {
        Query q = new Query();
        List<Criteria> criteriaList = getCriteria(input, entityInformation);
        if (criteriaList != null) {
            for (final Criteria c : criteriaList) {
                q.addCriteria(c);
            }
        }
        return q;
    }

    private static List<Object> convertArray(ColumnType type, String value) {
        final String[] parts = value.split(COMMA);
        final List<Object> convertedParts = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            convertedParts.add(type.tryConvert(parts[i]));
        }
        return convertedParts;
    }

    /**
     * Recursively get field name
     * 
     * @param javaType
     * @param fieldNameParts
     * @param currentIndex
     * @return
     */
    private static String getFieldName(Class<?> javaType, String[] fieldNameParts, int currentIndex) {
        if (javaType == null) {
            return null;
        }
        Objects.requireNonNull(fieldNameParts);

        if (currentIndex <= fieldNameParts.length - 1) {
            final String currentLevelName = fieldNameParts[currentIndex];
            String decidedName = null;
            Class<?> currentLevelFieldType = null;
            
            // do logic and append more
            @SuppressWarnings("unchecked")
            Set<Field> possibleFields = ReflectionUtils.getAllFields(javaType, new Predicate<Field>() {
                @Override
                public boolean apply(@Nullable Field input) {
                    if (input != null) {
                        if (currentLevelName.equals(input.getName())) {
                            return true;
                        } else if (IS_JACKSON_AVAILABLE) {
                            // direct matching with @JsonProperty
                            final JsonProperty jsonProperty = input.getAnnotation(JsonProperty.class);
                            if (jsonProperty != null && StringUtils.hasLength(jsonProperty.value())) {
                                if (currentLevelName.equals(jsonProperty.value())) {
                                    return true;
                                }
                            }

                            // TODO: Jackson PropertyNamingStrategy should also be considered
                        }
                    }
                    return false;
                }
            });

            if (possibleFields != null && !possibleFields.isEmpty()) {
                for (final Field field : possibleFields) {
                    decidedName = field.getName();
                    currentLevelFieldType = field.getType();
                    break;
                }
            }

            if (StringUtils.isEmpty(decidedName)) {
                return null;
            }

            if (currentIndex < fieldNameParts.length - 1) {
                String childrenFieldName = getFieldName(currentLevelFieldType, fieldNameParts, currentIndex + 1);
                if (childrenFieldName == null) {
                    return null;
                } else {
                    return decidedName + "." + childrenFieldName;
                }
            } else {
                return decidedName;
            }
        }
        return null;
    }

    /**
     * Determine actual MongoDB field name from input
     * (including jackson-databind, etc.)
     * 
     * @param javaType actual java type
     * @param fieldName frontend provided field name
     * @return
     */
    private static String getFieldName(Class<?> javaType, String fieldName) {
        if (javaType == null || StringUtils.isEmpty(fieldName)) {
            return null;
        }

        String result = null;
        if (fieldName.contains(".")) {
            final String[] parts = fieldName.split("\\.");
            result = getFieldName(javaType, parts, 0);
        } else {
            result = getFieldName(javaType, new String[] { fieldName }, 0);
        }
        log.trace("getFieldName({}, '{}') returns : '{}'", javaType.getSimpleName(), fieldName, result);
        return result;
    }

    /**
     * Convert a {@link DataTablesInput} to Criteia
     * 
     * @param input
     * @return
     */
    private static <T, ID extends Serializable> List<Criteria> getCriteria(final DataTablesInput input,
            MongoEntityInformation<T, ID> entityInformation) {
        List<Criteria> result = new LinkedList<>();
        // check for each searchable column whether a filter value exists
        List<Column> columns = input.getColumns();

        for (final Column column : columns) {
            final Search search = column.getSearch();
            final ColumnType type = ColumnType.parse(column.getType());
            final Filter filter = column.getFilter();
            if (column.hasValidSearch()) {
                // search != null && issearchable == true && search.value.length > 0
                final String searchValue = search.getValue();
                Criteria c = Criteria.where(getFieldName(entityInformation.getJavaType(), column.getData()));
                if (search.isRegex()) {
                    // is regex, so treat directly as regular expression
                    c.regex(searchValue);
                } else {
                    final Object parsedSearchValue = type.tryConvert(searchValue);
                    if (parsedSearchValue instanceof String) {
                        c.regex(getLikeFilterPattern(search.getValue()));
                    } else {
                        // numeric values , treat as $eq
                        c.is(parsedSearchValue);
                    }
                }
                result.add(c);
            } else {
                // handle column.filter
                if (filter != null) {
                    boolean hasValidCrit = false;
                    Criteria c = Criteria.where(getFieldName(entityInformation.getJavaType(), column.getData()));
                    if (StringUtils.hasLength(filter.getEq())) {
                        // $eq takes first place
                        c.is(type.tryConvert(filter.getEq()));
                        hasValidCrit = true;
                    } else if (StringUtils.hasLength(filter.getNe())) {
                        // $ne
                        c.ne(type.tryConvert(filter.getNe()));
                        hasValidCrit = true;
                    } else {
                        if (StringUtils.hasLength(filter.getIn())) {
                            // $in takes second place
                            c.in(convertArray(type, filter.getIn()));
                            hasValidCrit = true;
                        }

                        if (StringUtils.hasLength(filter.getNin())) {
                            c.nin(convertArray(type, filter.getNin()));
                            hasValidCrit = true;
                        }

                        if (StringUtils.hasLength(filter.getRegex())) {
                            // $regex also works here
                            c.regex(filter.getRegex());
                            hasValidCrit = true;
                        }

                        if (filter.getIsNull() != null && filter.getIsNull().booleanValue()) {
                            c.is(null);
                            hasValidCrit = true;
                        }

                        if (filter.getIsEmpty() != null && filter.getIsEmpty().booleanValue()) {
                            c.is("");
                            hasValidCrit = true;
                        }

                        if (filter.getExists() != null) {
                            c.exists(filter.getExists().booleanValue());
                            hasValidCrit = true;
                        }

                        if (type.isComparable()) {
                            // $gt, $lt, etc. only works if type is comparable
                            if (StringUtils.hasLength(filter.getGt())) {
                                c.gt(type.tryConvert(filter.getGt()));
                                hasValidCrit = true;
                            }
                            if (StringUtils.hasLength(filter.getGte())) {
                                c.gte(type.tryConvert(filter.getGte()));
                                hasValidCrit = true;
                            }
                            if (StringUtils.hasLength(filter.getLt())) {
                                c.lt(type.tryConvert(filter.getLt()));
                                hasValidCrit = true;
                            }
                            if (StringUtils.hasLength(filter.getLte())) {
                                c.lte(type.tryConvert(filter.getLte()));
                                hasValidCrit = true;
                            }
                        }
                    }
                    if (hasValidCrit) {
                        result.add(c);
                    }
                }
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
     * Creates a '$sort' clause for the given {@link DataTablesInput}.
     * 
     * @param input the {@link DataTablesInput} mapped from the Ajax request
     * @return a {@link Pageable}, must not be {@literal null}.
     */
    public static Pageable getPageable(DataTablesInput input) {
        List<Order> orders = new ArrayList<Order>();
        for (org.springframework.data.mongodb.datatables.mapping.Order order : input.getOrder()) {
            Column column = null;
            if (StringUtils.hasLength(order.getData())) {
                column = input.getColumn(order.getData());
            } else if (order.getColumn() != null && input.getColumns().size() > order.getColumn()) {
                if (order.getColumn() != null && input.getColumns() != null
                        && input.getColumns().size() > order.getColumn()) {
                    column = input.getColumns().get(order.getColumn());
                } else {
                    column = input.getColumn(order.getData());
                }
            }

            if (column == null) {
                if (StringUtils.hasLength(order.getData())) {
                    // in case if input has no columns defined
                    Direction sortDirection = Direction.fromString(order.getDir());
                    orders.add(new Order(sortDirection, order.getData()));
                } else {
                    log.debug("Warning: unable to find column by specified order {}", order);
                }
            } else if (!column.isOrderable()) {
                log.debug("Warning: column {} is not orderable, order is ignored", column);
            } else {
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

    /**
     * "LIKE" search is converted to $regex
     * 
     * @param filterValue
     * @return
     */
    private static Pattern getLikeFilterPattern(String filterValue) {
        return Pattern.compile(filterValue, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
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
    private static <T, ID extends Serializable> List<AggregationOperation> toAggregationOperation(
            MongoEntityInformation<T, ID> entityInformation, DataTablesInput input) {
        List<AggregationOperation> result = new LinkedList<>();
        List<Criteria> criteriaList = getCriteria(input, entityInformation);
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
    public static <T, ID extends Serializable> TypedAggregation<T> makeAggregationCountOnly(
            MongoEntityInformation<T, ID> entityInformation, DataTablesInput input, AggregationOperation[] operations) {
        List<AggregationOperation> opList = new LinkedList<>();
        if (operations != null) {
            for (int i = 0; i < operations.length; i++) {
                opList.add(operations[i]);
            }
        }

        opList.addAll(toAggregationOperation(entityInformation, input));

        opList.add(group().count().as("_count"));
        return newAggregation(entityInformation.getJavaType(), opList);
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

        opList.addAll(toAggregationOperation(null, input));

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
