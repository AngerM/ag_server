package dev.angerm.ag_server.database

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import dev.angerm.ag_server.App
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DatabaseModuleTest {
    @Test
    fun testConfigParsing() {
        val config = Config {
            this.addSpec(DatabaseSpec)
        }.from.yaml.string(
            """
               database:
                 primary:
                   protocol: postgres
                   hostname: localhost
                   port: 5379
                   database: test
                   otherOptions:
                     someSpecificOption: testValue
            """.trimIndent()
        ).from.env()
        val dbConfigs = config[DatabaseSpec.database]
        assertEquals(1, dbConfigs.size)
        val first = dbConfigs[dbConfigs.keys.first()]!!
        assertEquals("localhost", first.hostname)
        assertEquals("postgres", first.protocol)
        assertEquals(5379, first.port)
        assertEquals("test", first.database)
        assertEquals(1, first.otherOptions.size)
        val value = first.otherOptions[first.otherOptions.keys.first()]!!
        assertEquals("someSpecificOption", first.otherOptions.keys.first())
        assertEquals("testValue", value)
    }

    @Test
    fun testH2Connection()  {
        App.testServer(DatabaseModule(),
            rawYamlConfig = """
               database:
                 primary:
                   protocol: h2:mem 
                   database: test
            """.trimIndent()) { server ->
            val dbs = server.getInjector()?.getInstance(DbContainer::class.java)
            val client = dbs?.clients?.entries?.firstOrNull()
            assertNotNull(client)
        }
    }
}