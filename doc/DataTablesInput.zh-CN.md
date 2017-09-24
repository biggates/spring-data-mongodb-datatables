# DataTablesInput #

在实际操作中，在 DataTables 的通信协议的基础上，遇到了一些不足，主要有以下方面：

* 通过 `columns[i][search][value]` 实现的按列搜索功能，不能指定范围。比如 “指定日期范围” 或者 “大于 xx 的值” 这种场景，无法在统一的一个通信协议里传递
* 通过 `columns[i][search][regex]` 只能指定 `普通搜索` 和 `按正则表达式搜索` 两种场景，

## 具体定义 ##

下面是 DataTables 协议的原始内容：

| 参数名   |    类型    | 是否必须 |  默认值  |               描述              |
|----------|-----------|----------|----------|---------------------------------|
| `draw`   | `integer` |   必须   |    `1`   | 用于在异步场景中将响应和响应的请求对应起来。 |
| `start`  | `integer` |   可选   |    `0`   | 用于分页，描述本页的第一条数据在实际数据集中的 index 。|
| `length` | `integer` |   可选   |   `10`   | 用于分页，描述本页的数据的长度。如果取值为 `-1` 则表示返回从 `start` 开始的全部数据。 |
| `search` | `object` | 必须 |     | 用于全局搜索，描述全局搜索的参数。 |
| `search[value]` | `string` | 必须 |     | 用于全局搜索，描述全局搜索的参数。 |
| `search[regex]` | `boolean` | 必须 |     | 用于全局搜索，描述全局搜索是否按正则表达式进行。 |
| `order`  | `array` | 必须 |     | 用于指定排序。 |
| `order[i][column]` | `integer` | 必须 |     | 用于指定排序的列的 index。 |
| `order[i][dir]` | `string(asc|desc)` | 必须 |   | 用于指定排序的列的排序方向。 |
| `columns[i][data]` | `string` | 必须 |    | 用于描述每一列的数据的来源。 |
| `columns[i][name]` | `string` | 必须 |    | 用于描述每一列的数据的名称。 |
| `columns[i][searchable]` | `boolean` | 可选 |  `false`  | 用于描述每一列是否可被搜索 |
| `columns[i][orderable]` | `boolean` | 可选 |  `false`   | 用于描述每一列是否可被排序 |
| `columns[i][search][value]` | `string` | 可选 |  `""`   | 按列搜索的值。 |
| `columns[i][search][regex]` | `boolean` | 可选 |  `false`   | 按列搜索是否按正则表达式进行。 |

在实际操作中，首先经由 `jquery.spring-friendly.js` 将数组形式做了转换，其次根据需要对这些参数进行了一些扩展。整理后的参数如下：

|    来源    |     参数名      |    类型    | 是否必须 |  默认值   |               描述              |  修改之处 |
|------------|----------------|------------|----------|----------|---------------------------------|----------|
|   本项目   |    `draw`      | `integer`  |   可选   |    `1`   | 用于在异步场景中将响应和响应的请求对应起来。 | 改为可选的 |
| DataTables |    `start`     | `integer`  |   可选   |    `0`   | 用于分页，描述本页的第一条数据在实际数据集中的 index 。| 无修改 |
| DataTables |    `length`    | `integer`  |   可选   |   `10`   | 用于分页，描述本页的数据的长度。如果取值为 `-1` 则表示返回从 `start` 开始的全部数据。 | 无修改 |
|   本项目   |    `search`    |  `object`  |   可选   |  `null`   | 用于全局搜索，描述全局搜索的参数。 | 全局搜索修改为可选的 |
|   本项目   | `search.value` |  `string`  |   必须   |           | 用于全局搜索，描述全局搜索的值。 | 全局搜索修改为可选的 |
|   本项目   | `search.regex` |  `boolean` |   可选   |  `false`  | 用于全局搜索，描述全局搜索是否按正则表达式进行。 | 全局搜索修改为可选的 |
|   本项目   |     `order`    |  `array`   |   可选   |  `null`   | 用于指定排序方式。 | `order` 参数改为可选的 |
|   本项目   | `order[i].data` | `string` |  可选  |     | 用于指定排序的列的来源。该值和 `column` 必须传一个。 | 新增该参数，用于当没有按列搜索的时候简化参数（整个 `columns` 都不用传）。 |
|   本项目   | `order[i].column` | `integer` |  可选  |     | 用于指定排序的列的 index。该值和 `data` 必须传一个。 |  |
| DataTables | `order[i].dir` | `string(asc/desc)` | 必须 |   | 用于指定排序的列的排序方向。 | 无修改 |
|   本项目   |   `columns`                 | `array`   | 可选 |    | 用于描述每一列。 | `columns` 参数整个改为可选的 |
| DataTables | `columns[i].data`           | `string`  | 必须 |    | 用于描述每一列的数据的来源。 | 无修改 |
|   本项目   | `columns[i].name`           | `string`  | 可选 |    | 用于描述每一列的数据的名称。 | 后端不处理该参数 |
|   本项目   | `columns[i].searchable`     | `boolean` | 可选 |  `true`  | 用于描述每一列是否可被搜索 | 默认值改为 `true` |
|   本项目   | `columns[i].orderable`      | `boolean` | 可选 |  `true`   | 用于描述每一列是否可被排序 | 默认值改为 `true` |
|   本项目   | `columns[i].type`           | `string(integer/double/string/date)` | 可选 |  `string`   | 描述数据的类型。 | 新增 |
|   本项目   | `columns[i].search`         | `object`  | 可选 |  `null`   | 按列搜索的具体参数。注意 `filter` 和 `search` 并存时，以 `search` 为主。 | 按列搜索整个改为可选的 |
|   本项目   | `columns[i].search.value`   | `string`  | 可选 |  `""`   | 按列搜索的值。 | 无修改 |
|   本项目   | `columns[i].search.regex`   | `boolean` | 可选 |  `false`   | 按列搜索是否按正则表达式进行。 | 无修改 |
|   本项目   | `columns[i].filter`         | `object`  | 可选 |  `null`   | 按列筛选的具体参数。注意 `filter` 和 `search` 并存时，以 `search` 为主。 | 新增 |
|   本项目   | `columns[i].filter.gt`      | `string` | 可选 |  `null`   | 按列筛选的 `>` 条件。 | 新增 |
|   本项目   | `columns[i].filter.gte`     | `string` | 可选 |  `null`   | 按列筛选的 `>=` 条件。 | 新增 |
|   本项目   | `columns[i].filter.lt`      | `string` | 可选 |  `null`   | 按列筛选的 `<` 条件。 | 新增 |
|   本项目   | `columns[i].filter.lte`     | `string` | 可选 |  `null`   | 按列筛选的 `<=` 条件。 | 新增 |
|   本项目   | `columns[i].filter.eq`      | `string` | 可选 |  `null`   | 按列筛选的 `=` 条件。 | 新增 |
|   本项目   | `columns[i].filter.in`      | `string(csv)` | 可选 |  `null`   | 按列筛选的 `in` 条件，用于筛选该列的值在指定的数组中的情况。给出的多个数据用 `,` 隔开。 | 新增 |
|   本项目   | `columns[i].filter.regex`      | `string` | 可选 |  `null`   | 按列筛选的 `in` 条件，用于筛选该列的值在指定的数组中的情况。给出的多个数据用 `,` 隔开。 | 新增 |

## 实例 ##

### 最简单的请求 ###

因为 `order`, `columns`, `search` 全都改为可选的，因此最简单的请求就变成了

```
(无参数)
```

### 分页 ###

单纯分页只需要传入 `start` 和 `length` 。（当然如果 `length` 和后端一致的话也可以不传了）

```
?start=30&length=10
```

### 按某一列排序 ###

在最简单的情况下，通过新增的 `order[i].data` 可以不传 `columns` 那一大堆定义：

```
?order[0].data=createTime&order[0].dir=desc
```

### 按某一列搜索 ###

由于 `columns[i].searchable` 和 `columns[i].search.regex` 都修改了默认值，因此最简单的搜索是这样的：

```
?columns[0].data=user.name&columns[0].search.value=张三丰
```

### 按某一列筛选 ###

按数值

```
?columns[0].data=user.name&columns[0].filter.eq=张三丰
```

按数值范围

```
?columns[0].data=price.value&columns[0].type=double&columns[0].filter.lt=9.5&columns[0].filter.gte=9.0
```

按时间范围

```
?columns[0].data=createTime&columns[0].type=date&columns[0].filter.lt=2017-09-20&columns[0].filter.gt=2017-09-19
```

按枚举

```
?columns[0].data=status&columns[0].filter.in=已删除,失效
```

## 参考 ##

* [DataTables: Server-side processing](https://datatables.net/manual/server-side) 和响应的 [中文版](http://datatables.club/manual/server-side.html)

