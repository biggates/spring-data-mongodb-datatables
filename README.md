# spring-data-mongodb-datatables
Datatables binding for [spring-data-mongodb](http://projects.spring.io/spring-data-mongodb/)

This is a simple Spring Boot project using [biggates/spring-data-mongodb-datatables](https://github.com/biggates/spring-data-mongodb-datatables).

## Usage ##

### Using Query ###

```java
@GetMapping("/data/orders")
public DataTablesOutput<Order> getOrders(@Valid DataTablesInput input) {
    return repo.findAll(input);
}
```

### Using Aggregation ###

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

## Known Issues ##

* ~~MongoDB aggregation is NOT supported yet.~~
* ~~Unlike the jpa version, the usage is currently restricted in queries on ONE document only.~~ `$lookup` pipeline is supported (on MongoDB 3.2 and above).
* ~~You may have to manually exclude some jpa-related dependencies, especially in spring-boot projects and you do not need them.~~ `jdbc`, `jpa` and `querydsl-jpa` are marked as excluded in pom.
* Text search is simply converted to Regular Expressions with `Literal` flag and may contain some logical flaws.
* Global search is NOT implementd yet (as discussed in #1).
* Querydsl support is NOT verified yet.
