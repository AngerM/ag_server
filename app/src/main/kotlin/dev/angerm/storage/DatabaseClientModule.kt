package dev.angerm.storage

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.multibindings.ProvidesIntoSet
import com.uchuhimo.konf.ConfigSpec
import org.springframework.r2dbc.core.DatabaseClient

class DatabaseClientModule : AbstractModule() {

    @ProvidesIntoSet
    fun getSpec(): ConfigSpec {
        return DatabaseSpec
    }

    @Provides
    @Inject
    fun getDb(
        impl: DatabaseClient
    ): DatabaseClient {
        return impl
    }
}
