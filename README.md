# Duct migrator.ragtime [![Build Status](https://github.com/duct-framework/migrator.ragtime/actions/workflows/test.yml/badge.svg)](https://github.com/duct-framework/migrator.ragtime/actions/workflows/test.yml)

[Integrant][] methods for running database migrations using
[Ragtime][].

[integrant]: https://github.com/weavejester/integrant
[ragtime]:   https://github.com/weavejester/ragtime

## Installation

Add the following dependency to your deps.edn file:

    org.duct-framework/migrator.ragtime {:mvn/version "0.6.0"}

Or to your Leiningen project file:

    [org.duct-framework/migrator.ragtime "0.6.0"]

## Usage

This library provides the `:duct.migrator/ragtime` Integrant key,
which takes five options:

```edn
{:duct.migrator/ragtime
 {:database         #ig/ref :duct.database/sql
  :logger           #ig/ref :duct/logger
  :strategy         :rebase
  :migrations       [...]
  :migrations-table "ragtime_migrations"}}
```

### :database

The `:database` key should be a SQL database compatible with the Duct
[database.sql][] library, such as [database.sql.hikaricp][]:

```edn
{:duct.database.sql/hikaricp {:jdbcUrl "jdbc:sqlite:db/foo.sqlite"}}
```

[database.sql]: https://github.com/duct-framework/database.sql
[database.sql.hikaricp]: https://github.com/duct-framework/database.sql.hikaricp

### :logger

The `:logger` key should be a logger compatible with the Duct
[logger][] library, such as [logger.simple][]:

```edn
{:duct.logger/simple {:appenders [{:type :stdout}]}}
```

[logger]: https://github.com/duct-framework/logger

### :strategy

The `:strategy` is either `:apply-new`, `:raise-error` or
`:rebase`. These correspond to the [ragtime strategies][] with the
same names.

[ragtime strategies]: https://weavejester.github.io/ragtime/ragtime.strategy.html

### :migrations

A data structure containing a vector of [Ragtime SQL migrations][]. As
this can be large, it may be a a good idea to factor this out using the
`#duct/include` reader tag, or an equivalent.

[ragtime sql migrations]: https://github.com/weavejester/ragtime/wiki/SQL-Migrations#edn

### :migrations-table

Finally, the `:migrations-table` optional key corresponds
to the [ragtime sql-database][] option with the same name. It is the
name of the table to store the applied migrations (defaults to
"ragtime_migrations" if not specified).

[ragtime sql-database]: https://weavejester.github.io/ragtime/ragtime.jdbc.html#var-sql-database

## License

Copyright © 2025 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
