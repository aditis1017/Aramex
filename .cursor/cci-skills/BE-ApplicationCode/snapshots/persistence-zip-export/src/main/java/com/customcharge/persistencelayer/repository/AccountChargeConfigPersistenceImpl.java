package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Account;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Savepoint;
import java.util.Optional;
import java.util.function.Supplier;

public class AccountChargeConfigPersistenceImpl implements AccountChargeConfigPersistence {

    private static final String INSERT_SAVEPOINT = "account_charge_config_insert";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Account saveChargeConfigOrFetchExisting(
            Account candidate, Supplier<Optional<Account>> fetchExisting) {
        Session session = entityManager.unwrap(Session.class);
        Savepoint savepoint = setSavepoint(session);
        try {
            entityManager.persist(candidate);
            entityManager.flush();
            releaseSavepoint(session, savepoint);
            return candidate;
        } catch (RuntimeException ex) {
            rollbackToSavepoint(session, savepoint);
            if (!isUniqueConstraintViolation(ex)) {
                throw ex;
            }
            entityManager.detach(candidate);
            return fetchExisting.get().orElseThrow(() -> toDataIntegrityViolation(ex));
        }
    }

    private static Savepoint setSavepoint(Session session) {
        ReturningWork<Savepoint> work = connection -> connection.setSavepoint(INSERT_SAVEPOINT);
        return session.doReturningWork(work);
    }

    private static void rollbackToSavepoint(Session session, Savepoint savepoint) {
        if (savepoint == null) {
            return;
        }
        Work work = connection -> connection.rollback(savepoint);
        session.doWork(work);
    }

    private static void releaseSavepoint(Session session, Savepoint savepoint) {
        if (savepoint == null) {
            return;
        }
        Work work = connection -> connection.releaseSavepoint(savepoint);
        session.doWork(work);
    }

    private static boolean isUniqueConstraintViolation(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException
                    || current instanceof DataIntegrityViolationException) {
                return true;
            }
        }
        return false;
    }

    private static DataIntegrityViolationException toDataIntegrityViolation(RuntimeException ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof DataIntegrityViolationException dive) {
                return dive;
            }
        }
        return new DataIntegrityViolationException(ex.getMessage(), ex);
    }
}
