# ghostparsers

## requirements

application depends on graalvm or jvm. we strongly suggest build and run it on graalvm for performance and ability purposes.

you can download graalvm at [offical website](https://www.graalvm.org/)

building and runtime requirements:

- `graalvm-ce >= 20.0.0`
- `sbt == 1.3.8`

## building

in order to build or run tests, you don't need to install scala manually. change your work directory to source directory 
of ghostparsers, then just run `sbt` command to let it download and install scala language and compiler.

when you start to build or test application, sbt will start download dependencies automatically.

### test

in project directory, run `sbt` command, it will start sbt's interactive shell.

then you can run test command to run platform dependent tests.

- graalvm compatible tests: `graalvm / test`
- jvm compatible tests: `jvm / test`

### compile

in sbt's interactive shell, you should use platform dependent commands as well. after completing compile progress,
it will create a `.tar.gz` file which satisfies your all expectation. you can reach this file in `target` directory of 
platform directory.just copy that `.tar.gz` file, extract in a directory and then go through `bin` directory which 
extracted from archive file, and you are now able to run `./ghostparsers` command to let it run!

- graalvm build: `graalvm / packArchive`
- jvm build: `jvm / packArchive`

example:
```shell
$> sbt
sbt:ghostparsers> graalvm / packArchive
sbt:ghostparsers> exit
$> cp -v graalvm/target/graalvm-0.1.0.tar.gz path/to/deployment/dir
$> cd path/to/deployment/dir
$> tar zxvf graalvm-0.1.0.tar.gz
$> mv -v graalvm-0.1.0 ghostparsers-0.1.0
$> cd ghostparsers-0.1.0/bin
$> ./ghostparsers
```

## usage

### configuration

to specify a custom configuration file, you can pass a cli parameter like:

`./ghostparsers -Dconfig.file="/path/to/conf/app.conf"` 

example conf file:
```hocon
queue {
  capacity = 64
  ttl = 300000  # milliseconds
}

worker {
  size = 8
  output = "/path/to/location/store/parsed/files"
}

http {
  host = "localhost"
  port = 8080
}
```

### endpoints

#### Health check

`GET /_health`

Response:
```json
{
    "queued": 0,
    "engine": "graalvm-polyglot",
    "message": "If you've had a dose of a freaky eods, baby, you better call, ghostparsers!"
}
```

#### Create job

`POST /`

Request:
```json
{
	"file": "/Users/baris.yuksel/Downloads/S_2655059129_01.06.2016_01.06.2016_IDY.xls",
	"format": "excel",
	"options": {
		"sheet_at": 0,
		"header_starts_at": 0,
		"rows_start_at": 1
	},
	"source": "function map(data){let d=JSON.parse(data);let res=mapper(d);return JSON.stringify(res)};function reduce(data){let d=JSON.parse(data);let res=reducer(data);return JSON.stringify(res)};function line_data(data,groupBy){return{data:data,groupBy:groupBy}};function map_tx_type(t){let tx_type={'Satış':'charge','İade':'refund'};return tx_type[t]};function clear_amount(a){return a.replace(',','').replace('-','')};function clear_installments(i){if(i=='0'){return'1'};return i};function calculate_commission(data){let merchant_comm=parseFloat(clear_amount(data['İşyeri Kom. Tutarı']));let service_comm=parseFloat(clear_amount(data['Hizmet Kom. Tutarı']));let point_comm=parseFloat(clear_amount(data['İşyeri Puan Katkı Tutarı']));let discount_comm=parseFloat(clear_amount(data['İskonto Tutarı']));let comm=merchant_comm+service_comm+point_comm+discount_comm;return comm.toString()};function map_currency(c){if(c=='TL'){return'TRY'};return c};function mapper(data){let out={date:data['İşlem Tarihi'],type:map_tx_type(data['İşlem Tipi']),tx_amount:clear_amount(data['İşlem Tutarı']),installments:clear_installments(data['Taksit Sayısı']),eod_date:data['Gün Sonu Tarihi'],valor_date:data['Hesaba Geçiş Tarihi'],auth_code:data['Onay Kodu'],commissions:calculate_commission(data),currency:map_currency(data['Para Birimi']),net_amount:clear_amount(data['Net Tutar']),ref_id:data['Üye İşyeri Bilgisi']};return line_data(out,null)};function reducer(data){return data[0]}"
}
```

Response:
```json
{
    "id": "2d9bf388-8152-49b7-ae8f-a6b842414303",
    "status": "pending",
    "updated_at": "2020-03-02T11:41:30.941Z",
    "result": null,
    "completed_at": null
}
```

#### Job Status

`GET /2d9bf388-8152-49b7-ae8f-a6b842414303`

Response:
```json
{
    "id": "2d9bf388-8152-49b7-ae8f-a6b842414303",
    "status": "completed",
    "updated_at": "2020-03-02T11:41:35.116Z",
    "result": {
        "succeed": true,
        "path": "/Users/baris.yuksel/Desktop/ghostparsers/2d9bf388-8152-49b7-ae8f-a6b842414303.csv"
    },
    "completed_at": "2020-03-02T11:41:35.116Z"
}
```

### scripting

ghostparsers requires a javascript code in order to evaulate on every read line. you have to provide code when creating job request,
it should be at `source` field in payload.

a quote from graalvm official documentation about JavaScript compatibility:

> GraalVM includes an ECMAScript compliant JavaScript engine. It is designed to be fully standard compliant ...

your source code just needs to have only 2 functions: `map` and `reduce`. these two functions takes a string parameter which
holds a JSON data and returns a string which holds a JSON data as well. Both returned json data's values should be string.

#### map function

`map` function takes a string value that it actually holds a JSON data which associated with the header and the line in your source file (csv, xls, xlsx).
you can prepare a new map data by using prodived. `map` function should also return a string that holds JSON.

return value object should like:
```javascript
{data: your_mapped_data, groupBy: null}
```

assume that your csv file looks like below:
```csv
a,b,c,d
1,2,3,4
2,3,4,5
3,4,5,6
```

your `map` function will take a JSON data as parameter on first line, like:
```json
{"a": "1", "b": "2", "c": "3", "d": "4"}
```

so your `map` function can be defined like below:
```javascript
function map(d) {
    let data = JSON.parse(d);
    
    let a = parseInt(data["a"]);
    let b = parseInt(data["b"]);
    let c = parseInt(data["c"]);
    let d = parseInt(data["d"]);

    let total = a + b + c + d;
    let is_a_odd = a % 2 == 1;
    let is_b_odd = b % 2 == 1;
    let is_c_odd = c % 2 == 1;
    let is_d_odd = d % 2 == 1;

    let out = {
        total: total.toString(),
        a: is_a_odd.toString(),
        b: is_b_odd.toString(),
        c: is_c_odd.toString(),
        d: is_d_odd.toString()
    };

    let ret = {data: out, groupBy: null};
    return JSON.stringify(ret);
}
```

after mapping lines by function above, you will have a csv file like:
```csv
a,b,c,d,total
true,false,true,false,10
false,true,false,true,14
true,false,true,false,18
```

#### reduce function

when your `map` function indicates the data that should be grouped by a key via `groupBy` property, ghostparsers holds the
mapped data together in a list. after completing parsing file, these grouped data will be passed to `reduce` function as a
list.

assume that you have a csv like below:
```csv
id,amount,installment
charge1,100,3
charge2,25,1
charge3,10,1
charge1,100,3
charge4,15,1
charge1,100,3
```

you can see that `charge1` payments are splitted and reported line-by-line. if you want to merge these lines, your `map`
function should inform ghostparsers that they will be grouped.

```javascript
function map(d) {
    let data = JSON.parse(d);
    
    let out = {
        id: data["id"],
        amount: data["amount"]
    };
    
    let installment = parseInt(data["installment"]);

    var groupBy = null;
    if(installment > 1) {
        groupBy = data["id"];
    }

    let ret = {data: out, groupBy: groupBy}
}
```

after parsing entire file, your `reduce` function will be invoked with a list that contains grouped lines.
for our example, the input data will be like below:

```json
[
  {"id": "charge1", "amount": "100"},
  {"id": "charge1", "amount": "100"},
  {"id": "charge1", "amount": "100"}
]
```

so your `reduce` function merge these three lines into a line:
```javascript
function reduce(d) {
    let data = JSON.parse(d);
    val ret = data.reduce((acc, e) => {
        let total = parseFloat(acc["amount"]) + parseFloat(e["amount"]);
        acc["amount"] = total.toString();
        return acc;
    });
    
    // in reduce function, you should return only data,
    // you don't need to envelope it into another object as like map function.
    return JSON.stringify(ret);
}
```
