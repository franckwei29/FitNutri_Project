package com.fitnutri.backend.profile;

import com.fitnutri.backend.support.PostgresIntegrationSupport;
import com.fitnutri.backend.user.User;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

class AccountVersionIntegrationTest extends PostgresIntegrationSupport {
    @Autowired private EntityManagerFactory factory;
    @Test void twoRealJpaTransactionsCannotSilentlyOverwriteAUserVersion() {
        try(var first=factory.createEntityManager();var second=factory.createEntityManager()) {
            first.getTransaction().begin();
            User seed=new User();seed.setEmail("synthetic-"+UUID.randomUUID()+"@example.test");seed.setDisplayName("Original");seed.setPasswordHash("synthetic-not-login");
            first.persist(seed);first.getTransaction().commit();Long id=seed.getId();first.clear();
            first.getTransaction().begin();second.getTransaction().begin();
            User a=first.find(User.class,id),b=second.find(User.class,id);
            assertEquals(0,a.getVersion());assertEquals(0,b.getVersion());
            a.setDisplayName("First transaction");first.getTransaction().commit();
            b.setDisplayName("Stale transaction");
            assertThrows(OptimisticLockException.class,second::flush);second.getTransaction().rollback();
            first.clear();User saved=first.find(User.class,id);
            assertEquals("First transaction",saved.getDisplayName());assertEquals(1,saved.getVersion());
        }
    }
}
