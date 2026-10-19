package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Account;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountChargeConfigPersistenceImplTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Session session;
    @Mock
    private Connection connection;
    @Mock
    private Savepoint savepoint;

    private final AccountChargeConfigPersistenceImpl persistence = new AccountChargeConfigPersistenceImpl();

    private Account candidate;

    @BeforeEach
    void setUp() throws SQLException {
        candidate = new Account();
        ReflectionTestUtils.setField(persistence, "entityManager", entityManager);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(connection.setSavepoint(anyString())).thenReturn(savepoint);
        when(session.doReturningWork(any())).thenAnswer(invocation -> {
            ReturningWork<?> work = invocation.getArgument(0);
            return work.execute(connection);
        });
        doAnswer(invocation -> {
            Work work = invocation.getArgument(0);
            work.execute(connection);
            return null;
        }).when(session).doWork(any());
    }

    @Test
    void saveChargeConfigOrFetchExisting_shouldPersistAndFlushWhenUnique() throws SQLException {
        Account saved = persistence.saveChargeConfigOrFetchExisting(candidate, Optional::empty);

        assertThat(saved).isSameAs(candidate);
        verify(entityManager).persist(candidate);
        verify(entityManager).flush();
        verify(entityManager, never()).detach(candidate);
        verify(connection).releaseSavepoint(savepoint);
    }

    @Test
    void saveChargeConfigOrFetchExisting_shouldDetachAndReturnExistingOnUniqueConstraintViolation() throws SQLException {
        Account existing = new Account();
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
                "duplicate key",
                new SQLException("duplicate key value violates unique constraint", "23505"),
                "uq_account_ar_master_charge_type");
        doThrow(constraintViolation).when(entityManager).flush();

        Account saved = persistence.saveChargeConfigOrFetchExisting(
                candidate, () -> Optional.of(existing));

        assertThat(saved).isSameAs(existing);
        verify(entityManager).persist(candidate);
        verify(entityManager).detach(candidate);
        verify(connection).rollback(savepoint);
    }

    @Test
    void saveChargeConfigOrFetchExisting_shouldRethrowWhenConflictCannotBeResolved() throws SQLException {
        doThrow(new ConstraintViolationException(
                        "duplicate key",
                        new SQLException("duplicate key value violates unique constraint", "23505"),
                        "uq_account_ar_master_charge_type"))
                .when(entityManager)
                .flush();

        Supplier<Optional<Account>> fetchExisting = Optional::empty;

        assertThatThrownBy(() -> persistence.saveChargeConfigOrFetchExisting(candidate, fetchExisting))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(entityManager).detach(candidate);
        verify(connection).rollback(savepoint);
    }

    @Test
    void saveChargeConfigOrFetchExisting_shouldRethrowNonConstraintFailures() throws SQLException {
        doThrow(new IllegalStateException("db unavailable")).when(entityManager).flush();

        assertThatThrownBy(() -> persistence.saveChargeConfigOrFetchExisting(candidate, Optional::empty))
                .isInstanceOf(IllegalStateException.class);
        verify(entityManager, never()).detach(candidate);
        verify(connection).rollback(savepoint);
    }

}
