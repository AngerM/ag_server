package dev.angerm.ag_server.database

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import dev.angerm.ag_server.App
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.r2dbc.core.awaitSingle
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
                   driver: postgres
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
        assertEquals("postgres", first.driver)
        assertEquals(5379, first.port)
        assertEquals("test", first.database)
        assertEquals(1, first.otherOptions.size)
        val value = first.otherOptions[first.otherOptions.keys.first()]!!
        assertEquals("someSpecificOption", first.otherOptions.keys.first())
        assertEquals("testValue", value)
    }

    @Test
    fun testH2Connection() =
        App.testServer(
            DatabaseModule(),
            rawYamlConfig = """
               database:
                 primary:
                   driver: h2
                   protocol: mem 
                   database: test
            """.trimIndent()
        ) { server ->
            val dbs = server.getInjector()?.getInstance(DbContainer::class.java)
            val client = dbs?.clients?.entries?.firstOrNull()?.value
            assertNotNull(client)
            val result = client.sql(
                """
                CREATE TABLE MY_TABLE(ID INT, NAME VARCHAR(255));
                INSERT INTO MY_TABLE VALUES(1, 'USER1');
                SELECT COUNT(*) FROM MY_TABLE;
                """.trimIndent()
            ).fetch().awaitSingle().map {
                it.value
            }.firstOrNull()
            assertEquals(1, result as Long)

            val conn = client.connectionFactory.create().awaitSingle()
            conn.createStatement(
                "INSERT INTO MY_TABLE VALUES(1, 'USER1');"
            ).execute().awaitSingle()
            conn.createStatement(
                "INSERT INTO MY_TABLE VALUES(2, 'USER2');"
            ).execute().awaitSingle()
            val result2 = conn.createStatement(
                "SELECT COUNT(*) FROM MY_TABLE;"
            ).execute().awaitSingle().map { row, _ -> row.get(0) }.awaitSingle()
            assertEquals(3, result2 as Long)

            val conn2 = client.connectionFactory.create().awaitSingle()
            val result3 = conn2.createStatement(
                "SELECT COUNT(*) FROM MY_TABLE;"
            ).execute().awaitSingle().map { row, _ -> row.get(0) }.awaitSingle()
            assertEquals(3, result3 as Long)
        }
}