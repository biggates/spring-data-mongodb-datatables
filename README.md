# spring-data-mongodb-datatables
Datatables binding for [spring-data-mongodb](http://projects.spring.io/spring-data-mongodb/)

This is a simple Spring Boot project using [biggates/spring-data-mongodb-datatables](https://github.com/biggates/spring-data-mongodb-datatables).

## Known Issues ##

* MongoDB aggregation is NOT supported yet.
* Unlike the jpa version, the usage is currently restricted in queries on ONE document only.
* You may have to manually exclude some jpa-related dependencies, especially in spring-boot projects and you do not need them.
* Text search is simply converted to Regular Expressions with Literal flag and may contain some logical flaws.
* Global search is NOT implementd yet (as discussed in #1).
* Querydsl support is NOT verified yet.
