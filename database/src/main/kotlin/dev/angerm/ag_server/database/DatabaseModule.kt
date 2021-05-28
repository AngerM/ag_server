package dev.angerm.ag_server.database

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.multibindings.ProvidesIntoSet
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.springframework.r2dbc.core.DatabaseClient
import java.time.Duration

data class DbContainer(
    val clients: Map<String, DatabaseClient>
)

class DatabaseModule : AbstractModule() {
    @ProvidesIntoSet
    fun getConf(): ConfigSpec {
        return DatabaseSpec
    }

    @Provides
    @Inject
    @Singleton
    fun getDbs(
        conf: Config,
    ): DbContainer {
        val dbConfigs = conf[DatabaseSpec.database]
        val clients = dbConfigs.map {
            val dbConf = it.value
            val cfo = ConnectionFactoryOptions.builder().apply {
                this.option(ConnectionFactoryOptions.DRIVER, dbConf.driver)
                if (dbConf.protocol.isNotBlank()) {
                    this.option(ConnectionFactoryOptions.PROTOCOL, dbConf.protocol)
                }
                this.option(ConnectionFactoryOptions.DATABASE, dbConf.database)
                if (dbConf.hostname.isNotBlank()) {
                    this.option(ConnectionFactoryOptions.HOST, dbConf.hostname)
                }
                if (dbConf.port > 0) {
                    this.option(ConnectionFactoryOptions.PORT, dbConf.port)
                }
                this.option(ConnectionFactoryOptions.SSL, dbConf.ssl)
                if (dbConf.username.isNotBlank()) {
                    this.option(ConnectionFactoryOptions.USER, dbConf.username)
                }
                val pw = System.getenv(dbConf.passwordEnvVar)
                if (!pw.isNullOrBlank()) {
                    this.option(ConnectionFactoryOptions.PASSWORD, System.getenv(dbConf.passwordEnvVar))
                }
                dbConf.otherOptions.forEach { (optionName, value) ->
                    this.option(Option.valueOf(optionName), value)
                }
            }.build()
            val cf = ConnectionFactories.get(cfo)
            val poolConfiguration = ConnectionPoolConfiguration.builder(cf).apply {
                this.initialSize(dbConf.poolInitialSize)
                this.maxSize(dbConf.poolMaxSize)
                this.maxLifeTime(Duration.ofMinutes(15))
                this.maxIdleTime(Duration.ofMinutes(5))
                this.maxAcquireTime(Duration.ofSeconds(5))
            }.build()
            val pool = ConnectionPool(poolConfiguration)
            it.key to DatabaseClient.create(pool)
        }.toMap()
        return DbContainer(clients)
    }
}