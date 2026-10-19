package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Account;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Persists charge-config {@link Account} rows and recovers when concurrent consumers
 * hit the unique (master, charge_type) constraint.
 */
public interface AccountChargeConfigPersistence {

    Account saveChargeConfigOrFetchExisting(Account candidate, Supplier<Optional<Account>> fetchExisting);
}
