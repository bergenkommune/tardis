# tardis

[![Build Status](https://travis-ci.org/bergenkommune/tardis.svg?branch=master)](https://travis-ci.org/bergenkommune/tardis)

Tardis is a time machine for your data sources. It works by periodically dumping the contents of your tables to 
disk and committing these files to git. When you want to know what data has changed between two moments in time, tardis
uses git to compare the contents of these files at the given moments.

## Installation
Prerequisites: You need to install `openjdk-8` before installing tardis. The name of the package will vary between 
distributions. On CentOS, the command is: 

    yum install java-1.8.0-openjdk-devel.x86_64

Then you can download the latest Tardis RPM from github: https://github.com/bergenkommune/tardis/releases/latest and 
install it:

    rpm -Uvh tardis*.rpm
 
This will give you the following files and directories:
 
    /etc/init.d/tardis
    /etc/logrotate.d/tardis
    /opt/tardis
           ├── data
           ├── lib
           ├── status
           ├── tardis.conf
           └── tardis.jar

The application lives in `/opt/tardis`. The rpm will also add a user called `tardis`, which will be responsible
for running the application. 

## Add jdbc drivers

Before you can connect to any data sources, you need to download a jdbc driver for the type of rdbms you are 
connecting to. 

 - MS SQL Server: https://www.microsoft.com/en-us/download/details.aspx?displaylang=en&id=11774
 - Oracle: http://www.oracle.com/technetwork/database/features/jdbc/default-2280470.html

Starting with jdbc 4.0, drivers do not have to be loaded with `Class.forName(<driver class name>)` to become available 
to the application. Tardis relies on this feature. If you get an error that says something like
 `java.sql.SQLException: No suitable driver found`, your jdbc driver is probably too old.
  
Specifically, if you are using the MS SQL Server jdbc driver, you'll need to get the file called `sqljdbc42.jar`, and
if you are using Oracle, you'll need to get the file called `ojdbc7-<version>.jar`. If you have another RDBMS, find 
the appropriate jdbc 4.0 compliant jdbc driver online. 

When you have downloaded the jdbc driver for your RDBMS, put it into the `/opt/tardis/lib` directory. It should look
something like this:

    $ ls -l /opt/tardis/lib
    total 4192
    -rw-r--r--. 1 tardis root 3397734 Nov  3 13:46 ojdbc7-12.1.0.1.jar
    -rw-r--r--. 1 tardis root  891084 Nov  3 14:26 sqljdbc42.jar

The files need to be readable by the tardis user. 

## Configuration

You need to add configuration for each table (or view or query) you want to expose through tardis. Tables are grouped
by data source, and the table name and data source name becomes part of the url through which the data will become
available. 

Tardis expects to find a file called `/opt/tardis/application.yml`. This is a spring boot configuration file, and it 
is written in YAML format. The file must be readable by the `tardis` user. As it contains the passwords for your data
sources, it may be a good idea to make sure that the file is not readable by anyone else. 

### Data sources
A data source configuration provides the connection to the database, specifies the 

Configuration key: `tardis.dataSources` 

 - `name`: a unique name for this data source. The name should be all lower case. Spaces and special characters should
    be avoided.
 - `url`: the jdbc url used to connect to this datasource. The appropriate jdbc driver is derived from this url. 
 - `username`: a valid database user. MS SQL Integrated Security is not supported, so this needs to be a proper 
    sql user. 
 - `password`: the password of the database user. The password must be entered in clear text, so make sure to 
    protect the `application.yml` file so that unauthorized people cannot read it. 
 - `cronExpression`: A cron expression that defines when tables in this data source will be dumped into tardis, e.g., 
   `0 45 05-17 *  *  *` which will dump the contents at 45 minutes, 0 seconds after each hour in the time period
    05 in the morning until 17 in the afternoon, every day. See 
    http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06 for a detailed description
    of the format. 
 - `properties`: A key-value map of any extra data source properties to pass on to the jdbc driver. 

If you need to specify different `cronExpression`s for tables that are physically stored in the same database, you can
just add two data source configurations with different configuration. If, for instance, one database contains 
tables containing information about products and orders, and the products rarely change, but the orders change often, 
you can add one data source called `orders` and one called `products` with different `cronExpression`s but the same
`url`. 

### Tables

In Tardis, every data set that you want to make available is called a table. It doesn't matter if it is actually
a table or a view (or even just an ad-hoc sql query) in your database, as long as you conform to these simple rules: 

 - The query must contain a primary key. The primary key can be a single column or composed of multiple columns, 
   but it must be unique to the data being selected. Tardis uses the primary key to compute a diff when you ask it
   what has changed in a table between point A and B in time.
 - The query must be ordered by the primary key
 - The primary key must not contain `null`s. 
 - The query must be deterministic. If the underlying data has not changed, the query must return the same data, in
   the same column and row order every time you run it. This means that it is highly recommended to specify every 
   single column you want instead of using `select *`. 

Please note that if you change the columns in a query, Tardis will interpret this as a change to every row. This means
that if you ask Tardis for any changes in a table right before and right after you added or removed a column from 
the query, Tardis will return all rows with status `changed`. The old record will of course contain the old columns, 
and the new record will contain the new columns.


Configuration key: `tardis.tables`
 
 - `name`: a unique name for this table. The name should be all lower case. Spaces and special characters should
   be avoided.
 - `dataSourceName`: refers to the unique name for the data source that contains the table. 
 - `query`: the query used to fetch the data. Must contain an `order by` clause that orders the rows according to 
   the primary key. 
 - `primaryKeys`: a comma separated list of primary key columns. If you have more than one column, list the columns
   in the same order as in the `orderBy` clause above.

### Complete configuration file

#### `/opt/tardis/application.yml` 
    
    tardis:
      tables:
        - name: emp
          dataSourceName: hr
          query: select empno, ename, job, mgr, hiredate, sal, comm, deptno from emp order by empno
          primaryKeys: empno
        
        - name: dept
          dataSourceName: hr
          query: select deptno, dname from dept order by deptno
          primaryKeys: deptno
    
        - name: customers
          dataSourceName: northwind
          query: select CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax from Customers order by CustomerID
          primaryKeys: CustomerID          
      
      dataSources:
        - name: hr
          url: jdbc:oracle:thin:@localhost:1521:XE
          username: scott
          password: tiger
          cronExpression: 0 45 05-17 *  *  *
          properties:
            implicitCachingEnabled: true
            oracle.jdbc.timezoneAsRegion: false
        
        - name: northwind
          url: jdbc:sqlserver://localhost;databaseName=NORTHWND
          username: sa
          password: secret
          cronExpression: 0 * * * * *

If you need to change other settings, such as what port tardis will listen on, please check out 
http://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html and look for 
`server.port`.


## Running tardis

Tardis comes with an `init.d` script and the rpm will automatically add this script to the default runlevel. To 
restart Tardis, run

    $ sudo /etc/init.d/tardis restart
    
You can then check that tardis is running by querying its log: 
    
    $ curl http://localhost:8080/log
    5816eab00cb5c882f828abc94cbc3919f3d15af5 2016-11-04T09:01:17Z hr
    hr.emp.txt
    
    fc2c0f7c9f7a2cc8458af016d12c108ed9eff29c 2016-11-04T09:00:56Z hr
    hr.dept.txt
    
    20bd4fdaea550d942352f7a54a2c11a85acf2027 2016-11-04T01:26:32Z hr
    hr.emp.txt, hr.dept.txt
    
    efbc32d5ac859633799d1405a28cdec9ff181baf 2016-11-03T13:48:04Z hr
    hr.emp.txt, hr.dept.txt
    
    ab0234acd325236aa425ab343ff2311972f00ba4 2016-11-03T13:37:55Z northwind
    northwind.customers.txt
    
    72d4514140f864f9554627ed53848be9d1c9f827 2016-11-03T13:37:00Z initial commit
    
## HTTP endpoints
Tardis listens to port 8080 by default. The basic url scheme is 

    http://<servername>:<port>/<dataSourceName>/<tableName>?fromDate=yyyy-MM-ddTHH:mm:ssZ&toDate=yyyy-MM-ddTHH:mm:ssZ
    
where `dataSourceName` and `tableName` refer to the names you gave the data sources and tables in the configuration.

For example: 

    curl 'http://localhost:8080/hr/emp?fromDate=2016-11-04T08:50:00Z&toDate=2016-11-06T10:00:00Z'

    {"changeType": "delete", "oldRecord": {"EMPNO": 7499, "ENAME": "ALLEN", "JOB": "SALESMAN", "MGR": 7698, "HIREDATE": "1981-02-20", "SAL": 1600, "COMM": 300, "DEPTNO": 30 }}
    {"changeType": "add", "newRecord": {"EMPNO": 1234, "ENAME": "DUCK", "JOB": "CLERK", "MGR": 7902, "HIREDATE": "2016-01-02", "SAL": 400, "COMM": null, "DEPTNO": 20 }}
    {"changeType": "change", "oldRecord": {"EMPNO": 7369, "ENAME": "SMITH", "JOB": "CLERK", "MGR": 7902, "HIREDATE": "1980-12-17", "SAL": 800, "COMM": null, "DEPTNO": 20 } "newRecord": {"EMPNO": 7369, "ENAME": "SMITH", "JOB": "MANAGER", "MGR": 7902, "HIREDATE": "1980-12-17", "SAL": 1800, "COMM": 20, "DEPTNO": 20 }}

Please note that all dates in the URL scheme are in UTC. Dates found in your data will be formatted according to your 
locale. 

## Output data format

Tardis will output data in its own format, shown above. Each line in the returned data is a valid json document, and must be 
parsed by itself. The reason behind this non-standard use of json is that large change sets can eat up a lot of memory if you parse the entire
set in one big bite. With the tardis data format, you can simply loop through the results and parse one line at the
time. 

Each line in the returned data is a json document that contains the following attributes: 
 - `changeType`: enum containing either `add`, `change` or `delete`
 - `oldRecord`: if `changeType = delete`, it contains the deleted record. If `changeType = change`, it contains
   the record before the change. It is not present when `changeType = add`.
 - `newRecord`: if `changeType = add`, it contains the  added record. If `changeType = change`, it contains
   the record after the change. It is not present when `changeType = delete`.
   
The records themselves contain the columns from the associated sql query, in the order they are listed in the query. 
The JDBC driver determines the case of the column names. 

## Directory structure

### /opt/tardis

The application home directory. Contains the following files: 

 - `tardis.jar` - the application itself
 - `application.yml` - your data source and table configuration.
 - `tardis.conf` - default configuration for the init script. Do not edit. Instead, add your own `override.conf`.
 - `override.conf` - loaded by the init script. Allows you to override any environment variables such as `JAVA_HOME`. 
    See http://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html#deployment-script-customization-when-it-runs
    for details.

### /opt/tardis/data

Tardis will dump the contents of your tables into `/opt/tardis/data`, which is managed by git. Each table is written
to its own file, which is called `<datasource name>_<table name>.txt`. After each dump, tardis will commit the files
to git. 

This directory is a regular git working directory. You can view the git history and the contents of your tables by 
inspecting the contents of this directory with regular unix tools. Just make sure you don't change or add any files, 
as this will interfere with the operation of Tardis. 

### /opt/tardis/lib

Put jdbc drivers or other jars in this directory, and they'll be available for Tardis. 

### /opt/tardis/status

Tardis will output status information for each data source in this directory. 

 - `/opt/tardis/status/<dataSourceName>.ok` touched at the end of every dump of `dataSourceName` that succeeds
 - `/opt/tardis/status/<dataSourceName>.status` contains the text "OK" or "ERROR" depending on the result of the last dump

  