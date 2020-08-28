package example.springdata.cassandra.resources;

import example.springdata.cassandra.model.DummyTable;
import example.springdata.cassandra.persistence.DummyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class DummyResource {

    @Autowired
    private DummyRepository dummyRepository;

    @GetMapping("dummy")
    public Flux<DummyTable> dummy() {
        return dummyRepository.findAll();
    }
}
