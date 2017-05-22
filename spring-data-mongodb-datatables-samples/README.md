spring-data-mongodb-datatables-samples

This is a simple Spring Boot project using [biggates/spring-data-mongodb-datatables](https://github.com/biggates/spring-data-mongodb-datatables).

The original sample project is [darrachequesne/spring-data-jpa-datatables-sample](https://github.com/darrachequesne/spring-data-jpa-datatables-sample) 

## How to run ##

```
mvn spring-boot:run
```

The project will start at `http://localhost:8080/`.

## Features ##

The page contains one DataTables grid, with some pre-defined searching values.

On the page you can try the following features:

* Search by pre-defined columns
* Restrict the date range by providing a custom criteria
* Change order by any single column
* Pagination

## Some details ##

* This project uses [fakemongo/fongo](https://github.com/fakemongo/fongo) to create an in-memory MongoDB server named `test` (see `SampleConfiguration`).

* During the starting process, it inserts `200` completely random rows of `Order` item in `OrderRestController#insertSampleData()`, plus 2 specific `Order` item, in order to provide at least one search result in pre-defined queries.

* The project serves a static web page, displaying a grid using DataTables. The detailed initialization script is in `/home.js`. 

* The params `startDate` and `endDate` is used to restrict the range of a value. DataTables only handles "match" type of search, which is usually not enough. The criteria and `preFiltering` is used to further define a criteria.  

## Usage in your project ##

If you want to use `spring-data-mongodb-datatables` in your own project, these steps must be done:

1. Put the library in your project (it's not in any Maven repository yet)
2. Add `@JsonView(DataTablesOutput.View.class)` on every property of your `view` class (or directly on `document` class, as in this sample)
3. Declare another spring-data-mongodb repository, extending `DataTablesRepository<DocumentType, KeyType>`
4. Create your controller, basically as: 

```
    @Autowired
    private OrderRepo repo;

    @GetMapping()
    public DataTablesOutput<Order> getOrders(@Valid DataTablesInput input){
        return repo.findAll(input);
    }
```

5. In your html page, add `jquery.spring-friendly.js` before initializing Datatables.

## Known Issues ##

* MongoDB aggregation is not supported yet.
* Unlike the jpa version, the usage is currently restricted in queries on ONE document only.
* You may have to manually exclude some jpa-related dependencies, especially in spring-boot projects and you do not need them.
* Text search is simply converted to Literal Regular Expressions and may contain some flaws