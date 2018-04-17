# Duct migrator.ragtime

[![Build Status](https://travis-ci.org/duct-framework/migrator.ragtime.svg?branch=master)](https://travis-ci.org/duct-framework/migrator.ragtime)

[Integrant][] methods for running database migrations using
[Ragtime][].

[integrant]: https://github.com/weavejester/integrant
[ragtime]:   https://github.com/weavejester/ragtime

## Installation

To install, add the following to your project `:dependencies`:

    [duct/migrator.ragtime "0.2.1"]

## Usage

This library provides the `:duct.migrator/ragtime` Integrant key,
which takes four options:

```edn
{:duct.migrator/ragtime
 {:database   #ig/ref :duct.database/sql
  :logger     #ig/ref :duct/logger
  :strategy   :rebase
  :migrations [#ig/ref :foo.migration/create-foo-table]}}
```

The `:database` key should be a SQL database compatible with the Duct
[database.sql][] library. For example:

```edn
{:duct.database.sql/hikaricp
 {:jdbc-url "jdbc:sqlite:db/foo.sqlite"}}
```

The `:logger` key should be a logger compatible with the Duct
[logger][] library:

```edn
{:duct.logger.timbre/println {}
 :duct.logger/timbre
 {:level    :info
  :appender #ig/ref :duct.logger.timbre/println}}
```

The `:strategy` is either `:apply-new`, `:raise-error` or
`:rebase`. These correspond to the [ragtime strategies][] with the
same names.

The `:migrations` key is a vector of migrations. The easiest way to
create these is to use a composite key that is derived from
`:duct.migrator.ragtime/sql`.

```edn
{[:duct.migrator.ragtime/sql :foo.migration/create-foo-table]
 {:up   ["CREATE TABLE foo (id int);"]
  :down ["DROP TABLE foo;"]}}
```

Migrations built in this way expect an `:up` and a `:down` key that
contain vectors of SQL, either as strings, or URLs to resources on the
classpath.

[database.sql]:       https://github.com/duct-framework/database.sql
[logger]:             https://github.com/duct-framework/logger
[ragtime strategies]: https://weavejester.github.io/ragtime/ragtime.strategy.html

## License

Copyright © 2017 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
