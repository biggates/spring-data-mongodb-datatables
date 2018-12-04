# spring-data-mongodb-datatables
Datatables binding for [spring-data-mongodb](http://projects.spring.io/spring-data-mongodb/).

This is a [sample project](spring-data-mongodb-datatables-samples) showing how it works.

This project is inspired from [darrachequesne/spring-data-jpa-datatables](https://github.com/darrachequesne/spring-data-jpa-datatables/), which works with spring-data-jpa.

## Deprecation ##

Because [Damien](https://github.com/darrachequesne) now has [darrachequesne/spring-data-mongodb-datatables](https://github.com/darrachequesne/spring-data-mongodb-datatables) released, and this project has its own modification on `DataTablesInput`, I think it is time to mark it as `@Deprecated` now.

As Damien's implementation does not implement `Aggregation`, this repository will remain open , but will NOT be maintained anymore.

If you need similar features (customizable query with spring-data) but don't use DataTables, my new project [eaphone-spring-data-query](https://github.com/biggates/eaphone-spring-data-query) will be another choice. 

## Usage ##

Basic usage is the same with [darrachequesne/spring-data-jpa-datatables](https://github.com/darrachequesne/spring-data-jpa-datatables/)

### Introduce into project ###

`TODO` Not uploaded to any public Maven Repository yet.

```xml
<dependency>
    <groupId>com.eaphone</groupId>
    <artifactId>spring-data-mongodb-datatables</artifactId>
    <version>0.3.2-SNAPSHOT</version>
</dependency>
```

### Initialization ###

In any `@Configuration` class, add:

```java
@EnableMongoRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class)
```

### Write new Repo ###

Just as spring-data-mongodb does:

```java
@Repository
public interface UserRepository extends DataTablesRepository<Order, String> {
}
```

Note that `DataTablesRepository` extends `PagingAndSortingRepository` so it already contains functionalities like `findAll(Pageable)` and `save()`.

### Expose fields on view ###

```java
@Data
@Document(collection = "order")
public class Order {

    @Id
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
}
```

### On the browser side ###

Include `jquery.spring-friendly.js` so `column[0][data]` is changed to `column[0].data` and is correctly parsed by SpringMVC.

### On the Server Side ###

The repository has the following methods:

* Using Query
  * `DataTablesOutput<T> findAll(DataTablesInput input);`
  * `DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria);`
  * `DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria, Criteria preFilteringCriteria);`
* Using Aggregation
  * `<View> DataTablesOutput<View> findAll(Class<View> classOfView, DataTablesInput input, AggregationOperation... operations);`
  * `<View> DataTablesOutput<View> findAll(Class<View> classOfView, DataTablesInput input, Collection<? extends AggregationOperation> operations);`

### Examples ###

```java
@GetMapping("/data/orders")
public DataTablesOutput<Order> getOrders(@Valid DataTablesInput input) {
    return repo.findAll(input);
}
```

Or: 

```java
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@GetMapping("/")
public DataTablesOutput<DataView> getAll(@Valid DataTablesInput input) {
    return repo.findAll(DataView.class,
        input,
        // just provide your aggregation pipeline here
        lookup("user", "userId", "id", "user"),
        unwind("user"),
        project()
            .and("user.sex").as("user.gender")
            .andInclude(
                "timestamp", "createTime", "sensorType",
                "batchId", "source", "beginTime",
                "endTime", "text", "url", "value")
            );
}
```

## Filter ##

In addition to DataTables' `columns[x].search` parameters, `columns[x].filter` is a new way to define more complex queries.  

A more detailed document in Simplified Chinese (zh_CN) is provided [here](doc/DataTablesInput.zh-CN.md).

## Future Plans ##

In the near future:

* In Criteria converting, more types (Date) must be handled, as currently only `String` and `Boolean` are handled
* More tests (and verifications)

## Known Issues ##

* `$match`, `$sum: 1`, `$limit` and `$skip` are attached to given aggregation pipeline so in some cases the logic may be broken.
* Text search is simply converted to Regular Expressions with `Literal` flag and may contain some logical flaws.
* Global search is NOT implementd yet (as discussed in #1).
* Querydsl support is REMOVED, as my own project does not use it.

