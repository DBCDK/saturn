package dk.dbc.saturn;

import dk.dbc.saturn.entity.PasswordEntry;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.OffsetDateTime;
import java.util.List;

@Stateless
public class PasswordRepository {
    @PersistenceContext(unitName = "saturn_PU")
    EntityManager entityManager;

    /**
     * Lists passwords and their startdates
     * @param host host as featured in harvesterConfig
     * @param username username for this host
     * @param limit of how many passwords are listed
     * @return list of password entries
     */
    public List<PasswordEntry> list(String host, String username, int limit) {
        TypedQuery<PasswordEntry> query =
                entityManager.createNamedQuery(PasswordEntry.GET_PASSWORDS_NAME, PasswordEntry.class);
        query.setParameter("host", host);
        query.setParameter("username", username);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public PasswordEntry get(int id) {
        return entityManager.find(PasswordEntry.class, id);
    }

    public void delete(int id) {
        PasswordEntry entry = entityManager.find(PasswordEntry.class, id);
        if (entry != null) {
            entityManager.remove(entry);
        }
        else {
            throw new IllegalArgumentException(String.format("No password entry with id:'%d' found", id));
        }
    }

    /**
     *
     * @param host to connect to
     * @param username to connect with
     * @return the password that is active for this date. Or null, if none found.
     */
    public PasswordEntry getPasswordForDate(String host, String username, OffsetDateTime date) {
        TypedQuery<PasswordEntry> query =
                entityManager.createNamedQuery(PasswordEntry.GET_PASSWORD_FOR_DATE_NAME, PasswordEntry.class);
        query.setParameter("host", host);
        query.setParameter("username", username);
        query.setParameter("date", date);
        query.setMaxResults(1);
        List<PasswordEntry> resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    /**
     *
     * @param entry the entry that must be persisted
     * @return the new and merged password entry
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public PasswordEntry save(PasswordEntry entry) {
        final PasswordEntry originalEntry = entityManager.find(PasswordEntry.class, entry.getId());
        if (originalEntry == null) {
            entityManager.persist(entry);
            return entry;
        }
        entityManager.detach(originalEntry);
        return entityManager.merge(entry);
    }
}
