package dev.angerm.ag_server.database

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.Singleton
import com.uchuhimo.konf.Config
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.springframework.r2dbc.core.DatabaseClient

class DatabaseModule : AbstractModule() {
    @Provides
    @Inject
    @Singleton
    fun getDbs(
        conf: Config,
    ): Map<String, DatabaseClient> {
        val dbConfigs = conf[DatabaseSpec.database]
        return dbConfigs.map {
            val dbConf = it.value
            val cfo = ConnectionFactoryOptions.builder().apply {
                this.option(ConnectionFactoryOptions.DRIVER, "pool")
                this.option(ConnectionFactoryOptions.PROTOCOL, dbConf.protocol)
                this.option(ConnectionFactoryOptions.DATABASE, dbConf.database)
                this.option(ConnectionFactoryOptions.HOST, dbConf.hostname)
                this.option(ConnectionFactoryOptions.PORT, dbConf.port)
                this.option(ConnectionFactoryOptions.SSL, dbConf.ssl)
                this.option(ConnectionFactoryOptions.USER, dbConf.username)
                this.option(ConnectionFactoryOptions.PASSWORD, System.getenv(dbConf.passwordEnvVar))
                this.option(Option.valueOf("initialSize"), dbConf.poolInitialSize)
                this.option(Option.valueOf("maxSize"), dbConf.poolMaxSize)
                dbConf.otherOptions.forEach { (optionName, value) ->
                    this.option(Option.valueOf(optionName), value)
                }
            }.build()
            val cf = ConnectionFactories.get(cfo)
            it.key to DatabaseClient.create(cf)
        }.toMap()
    }
}