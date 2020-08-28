package example.springdata.cassandra.persistence;

import example.springdata.cassandra.model.DummyTable;
import org.springframework.data.cassandra.core.mapping.MapId;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;

public interface DummyRepository extends ReactiveCassandraRepository<DummyTable, MapId> {
}
