package dev.angerm.modules

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides
import org.springframework.r2dbc.core.DatabaseClient

class DatabaseClientModule: AbstractModule() {
    @Provides
    @Inject
    fun getDb(
        impl: DatabaseClient
    ): DatabaseClient {
        return impl
    }
}