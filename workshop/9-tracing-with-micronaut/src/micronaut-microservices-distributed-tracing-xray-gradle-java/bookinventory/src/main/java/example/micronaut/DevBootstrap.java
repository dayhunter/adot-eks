package example.micronaut;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

// @Requires(env = Environment.DEVELOPMENT)
@Singleton
public class DevBootstrap implements ApplicationEventListener<StartupEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(DevBootstrap.class);
    
    private final IdGenerator idGenerator;
    private final BookRepository bookRepository;

    public DevBootstrap(IdGenerator idGenerator,
                     BookRepository bookRepository) {
        this.idGenerator = idGenerator;
        this.bookRepository = bookRepository;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        if (!bookRepository.existsTable()) {
            bookRepository.createTable();
            LOG.info("create table");
            for (int i = 0; i < 50000; i++) {
                for (int j = 0; j < 50000; j++) {
                    for (int k = 0; k < 50000; k++) {
                    }
                }
            }
            LOG.info("seed data");
            seedData();
        } else {
            LOG.info("create table found, seed more data");
            seedData();
        }
    }

    private void seedData(){
        LOG.info("seed data+++");
        bookRepository.save(new Book(idGenerator.generate(), "1680502395", "Release It!", 2));
        bookRepository.save(new Book(idGenerator.generate(), "0321601912", "Continuous Delivery", 3));
        bookRepository.save(new Book(idGenerator.generate(), "1491950358", "Building Microservices", 4));
    }
}
