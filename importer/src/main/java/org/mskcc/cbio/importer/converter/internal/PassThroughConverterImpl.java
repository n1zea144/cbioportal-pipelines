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
package org.mskcc.cbio.importer.converter.internal;

import org.mskcc.cbio.importer.*;
import org.mskcc.cbio.importer.util.MapperUtil;
import org.mskcc.cbio.importer.model.*;

import org.apache.commons.logging.*;

import java.util.*;

/**
 * Class which implements the Converter interface.
 */
public class PassThroughConverterImpl implements Converter {

	// our logger
	private static Log LOG = LogFactory.getLog(PassThroughConverterImpl.class);

	// ref to configuration
	private Config config;

	// ref to file utils
	private FileUtils fileUtils;

	// ref to caseids
	private CaseIDs caseIDs;

	// ref to IDMapper
	private IDMapper idMapper;

	/**
	 * Constructor.
     *
     * @param config Config
	 * @param fileUtils FileUtils
	 * @param caseIDs CaseIDs;
	 * @param idMapper IDMapper
	 */
	public PassThroughConverterImpl(Config config, FileUtils fileUtils,
							 CaseIDs caseIDs, IDMapper idMapper) {

		// set members
		this.config = config;
        this.fileUtils = fileUtils;
		this.caseIDs = caseIDs;
		this.idMapper = idMapper;
	}

	/**
	 * Converts data for the given portal.
	 *
     * @param portal String
	 * @param runDate String
	 * @param applyOverrides Boolean
	 * @throws Exception
	 */
    @Override
	public void convertData(String portal, String runDate, Boolean applyOverrides) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Generates case lists for the given portal.
	 *
     * @param portal String
	 * @throws Exception
	 */
    @Override
	public void generateCaseLists(String portal) throws Exception {
		throw new UnsupportedOperationException();
	}

    /**
	 * Applies overrides to the given portal using the given data source.
	 * Any datatypes within the excludes datatypes set will not have be overridden.
	 *
	 * @param portal String
	 * @param excludeDatatypes Set<String>
	 * @param applyCaseLists boolean
	 * @throws Exception
	 */
    @Override
	public void applyOverrides(String portal, Set<String> excludeDatatypes, boolean applyCaseLists) throws Exception {
		throw new UnsupportedOperationException();
    }

	/**
	 * Creates a staging file from the given import data.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param datatypeMetadata DatatypeMetadata
	 * @param dataMatrices DataMatrix[]
	 * @throws Exception
	 */
	@Override
	public void createStagingFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
								  DatatypeMetadata datatypeMetadata, DataMatrix[] dataMatrices) throws Exception {

		// sanity check
		if (dataMatrices.length != 1) {
			throw new IllegalArgumentException("dataMatrices.length != 1, aborting...");
		}
		DataMatrix dataMatrix = dataMatrices[0];

		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), writing staging file.");
		}
		fileUtils.writeStagingFile(portalMetadata.getStagingDirectory(), cancerStudyMetadata, datatypeMetadata, dataMatrix);

		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), complete.");
		}
	
		if (datatypeMetadata.requiresMetafile()){
			if (LOG.isInfoEnabled()) {
				LOG.info("createStagingFile(), writing metadata file.");
			}
			fileUtils.writeMetadataFile(portalMetadata.getStagingDirectory(), cancerStudyMetadata, datatypeMetadata, dataMatrix);
		}	
	}
}
