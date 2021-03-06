/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center 
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center 
 * has been advised of the possibility of such damage.
*/

// package
package org.mskcc.cbio.importer;

// imports
import javax.sql.DataSource;

/**
 * Interface used to create database/database schema dynamically.
 */
public interface DatabaseUtils {

	/**
	 * Returns the database user credential.
	 *
	 * @return String
	 */
    String getDatabaseUser();

	/**
	 * Returns the database password credential.
	 *
	 * @return String
	 */
    String getDatabasePassword();

	/**
	 * Returns the database connection string.
	 *
	 * @return String
	 */
    String getDatabaseConnectionString();

	/**
	 * Returns the database schema filename.
	 *
	 * @return String
	 */
    String getPortalDatabaseSchema();

	/**
	 * Returns the importer database name.
	 *
	 * @return String
	 */
    String getImporterDatabaseName();

	/**
	 * Returns the portal database name.
	 *
	 * @return String
	 */
    String getPortalDatabaseName();

    /**
	 * Creates a database and optional schema.
	 * 
	 * @param databaseName String
	 * @param createSchema boolean
	 */
	void createDatabase(String databaseName, boolean createSchema);

	/**
	 * Execute the given script on the given db.
	 *
	 * @param databaseName String
	 * @param databaseScript String
	 * @param databaseUser String
	 * @param databasePassword String
	 */
	boolean executeScript(String databaseName, String databaseScript,
						  String databaseUser, String databasePassword);
}
