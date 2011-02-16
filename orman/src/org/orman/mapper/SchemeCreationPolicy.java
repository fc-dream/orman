package org.orman.mapper;

public enum SchemeCreationPolicy {
	/**
	 * Drops the table (with truncating its data) if exists and reconstructs the
	 * whole table from scratch each initialization process. WARNING: Loss of
	 * data.
	 * 
	 * Required on changes in the scheme.
	 */
	CREATE,

	/**
	 * Assumes that the table is identical to the existing scheme and does not
	 * recreate tables, it uses existing ones.
	 * 
	 * If physical scheme is not up to date, various runtime exceptions
	 * may thrown due to mismatches between persistency scheme and physical
	 * during query execution.
	 * 
	 * Recommended to switch {@link SchemeCreationPolicy.CREATE} for once.
	 */
	UPDATE;
}
