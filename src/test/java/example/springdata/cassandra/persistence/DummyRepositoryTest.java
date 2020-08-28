package example.springdata.cassandra.persistence;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import example.springdata.cassandra.model.DummyTable;
import example.springdata.cassandra.util.cassandra.CassandraTestsConfig;
import example.springdata.cassandra.util.migrations.CassandraDBTestMigrator;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedCassandra
@ContextConfiguration(classes = CassandraTestsConfig.class)
@EnableConfigurationProperties
public class DummyRepositoryTest {

    @Autowired
    private DummyRepository dummyRepository;

    @Autowired
    private CassandraDBTestMigrator cassandraDBTestMigrator;

    @Autowired
    private ReactiveCassandraOperations cassandraOperations;

    @BeforeClass
    public static void setupCassandra() throws InterruptedException, IOException, TTransportException {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
    }

    @Before
    public void setUp() {
        cassandraDBTestMigrator.migrateCassandra();
    }

    @Test
    public void test1() {
        dummyRepository.insert(new DummyTable("1", "john doe")).subscribe();

        List<String> listMono = cassandraOperations.select("SELECT table_name FROM system_schema.tables WHERE keyspace_name = 'cqlmigrate';", String.class).collectList().block();
        System.out.println("tables" + listMono);

        List<DummyTable> dummyTables = dummyRepository.findAll().collectList().block();

        assert dummyTables != null;
        assertEquals(1, dummyTables.size());

        assertEquals("john doe", dummyTables.get(0).getDummyData());
    }

    @Test
    public void test2()  {
        CqlSession build = CqlSession.builder().withKeyspace("cqlmigrate")
                .withLocalDatacenter("datacenter1")
                .addContactPoint(new InetSocketAddress("localhost", 9142))
                .build();
        build.execute("Insert into dummytable(first_key, dummy_data) values('1', 'jane doe');");

        ResultSet execute2 = build.execute("Select * from dummytable;");

        ArrayList<DummyTable> dummTables = new ArrayList<>();

        execute2.forEach(r -> {
            dummTables.add(new DummyTable(
                    r.getString("first_key"),
                    r.getString("dummy_data")));
        });

        assertEquals(1, dummTables.size());
    }
}