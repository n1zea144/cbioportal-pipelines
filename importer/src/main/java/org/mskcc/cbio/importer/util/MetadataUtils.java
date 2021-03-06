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
package org.mskcc.cbio.importer.util;

// imports
import org.mskcc.cbio.importer.*;
import org.mskcc.cbio.importer.model.*;
import org.mskcc.cbio.portal.model.ClinicalAttribute;
import org.mskcc.cbio.portal.scripts.ImportClinicalData;

import org.apache.commons.csv.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.base.Strings;
import com.google.common.collect.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.Method;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.internal.Preconditions;
import org.apache.log4j.Logger;

/**
 * Class which provides utilities shared across metadata objects (yes this could be in an abstract class).
 */
public class MetadataUtils {

	private static final Log LOG = LogFactory.getLog(MetadataUtils.class);
	private static final Pattern ENVIRONMENT_VAR_REGEX = Pattern.compile("\\$(\\w*)");

	/**
	 * Helper function to determine root directory for cancer study to install.
	 *
	 * @param portalMetadata PortalMetadata
	 * @param dataSourcesMetadata Collection<DataSourcesMetadata>
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @return String
	 */
	public static String getCancerStudyRootDirectory(PortalMetadata portalMetadata,
													 Collection<DataSourcesMetadata> dataSourcesMetadata,
													 CancerStudyMetadata cancerStudyMetadata) {

		// check portal staging area - should work for all tcga
		File cancerStudyDirectory =
			new File(portalMetadata.getStagingDirectory() + File.separator + cancerStudyMetadata.getStudyPath());
		if (cancerStudyDirectory.exists()) {
			return portalMetadata.getStagingDirectory();
		}

		// made it here, check other datasources 
		for (DataSourcesMetadata dataSourceMetadata : dataSourcesMetadata) {
			if (dataSourceMetadata.isAdditionalStudiesSource()) {
				cancerStudyDirectory =
					new File(dataSourceMetadata.getDownloadDirectory() + File.separator + cancerStudyMetadata.getStudyPath());
				if (cancerStudyDirectory.exists()) {
					return dataSourceMetadata.getDownloadDirectory();
				}
			}
		}

		// outta here
		return null;
	}

	/**
	 * Helper function used to get canonical path for given path. It will translate
	 * environment variables.
	 *
	 * @param path String
	 */
	public static String getCanonicalPath(String path) {
	
		String toReturn = path;

		Matcher lineMatcher = ENVIRONMENT_VAR_REGEX.matcher(path);
		if (lineMatcher.find()) {
			String envValue = System.getenv(lineMatcher.group(1));
			if (envValue != null) {
				toReturn = path.replace("$" + lineMatcher.group(1), envValue);
			}
		}

		// outta here
		return toReturn;
	}

	public static List<Boolean> getHeadersMissingMetadata(Config config, CancerStudyMetadata cancerStudyMetadata, List<String> normalizedColumnHeaderNames, boolean supplyDefaultClinicalAttributeValues)
	{
        Set<String> unknownAttributes = new HashSet<String>();
		List<Boolean> headersWithMissingMetadata = new ArrayList<Boolean>();

        int lc = -1;
        for (String columnHeader : normalizedColumnHeaderNames) {
            Collection<ClinicalAttributesMetadata> metadata = config.getClinicalAttributesMetadata(columnHeader.toUpperCase());
            if (!metadata.isEmpty() && !metadata.iterator().next().missingAttributes()) {
                headersWithMissingMetadata.add(++lc, false);
            }
            else {
                if (metadata.isEmpty()) {
                    unknownAttributes.add(columnHeader);
                }
            	headersWithMissingMetadata.add(++lc, (!supplyDefaultClinicalAttributeValues));
            }
        }
        if (!unknownAttributes.isEmpty()) {
            config.flagMissingClinicalAttributes(cancerStudyMetadata.toString(), cancerStudyMetadata.getTumorType(), unknownAttributes);
        }
        return headersWithMissingMetadata;	
	}

    public static String getClinicalMetadataHeaders(Config config, List<String> normalizedColumnHeaderNames, boolean supplyDefaultClinicalAttributeValues, String stagingFile) throws Exception
    {
		StringBuilder clinicalDataHeader = new StringBuilder();
        Map<String, ClinicalAttributesMetadata> clinicalAttributesMetadata = getClinicalAttributesMetadata(config, normalizedColumnHeaderNames, supplyDefaultClinicalAttributeValues, stagingFile);

        clinicalDataHeader.append(addClinicalDataHeader(normalizedColumnHeaderNames, clinicalAttributesMetadata, "getDisplayName"));
        clinicalDataHeader.append(addClinicalDataHeader(normalizedColumnHeaderNames, clinicalAttributesMetadata, "getDescription"));
        clinicalDataHeader.append(addClinicalDataHeader(normalizedColumnHeaderNames, clinicalAttributesMetadata, "getDatatype"));
        clinicalDataHeader.append(addClinicalDataHeader(normalizedColumnHeaderNames, clinicalAttributesMetadata, "getAttributeType"));
        clinicalDataHeader.append(addClinicalDataHeader(normalizedColumnHeaderNames, clinicalAttributesMetadata, "getPriority"));
        clinicalDataHeader.append(addClinicalDataColumnHeaders(normalizedColumnHeaderNames, clinicalAttributesMetadata));
        return clinicalDataHeader.toString();
    }

    private static Map <String, ClinicalAttributesMetadata> getClinicalAttributesMetadata(Config config, List<String> normalizedColumnHeaderNames, boolean supplyDefaultClinicalAttributeValues, String stagingFile)
            throws IOException, FileNotFoundException
    {
        Map<String, ClinicalAttributesMetadata> toReturn = new HashMap<String, ClinicalAttributesMetadata>();

        Reader reader;
        reader = new FileReader(Paths.get(stagingFile).toFile());
        CSVParser parser = new CSVParser(reader, CSVFormat.TDF.withHeader());
        List<CSVRecord> records = parser.getRecords();

        for (String columnHeader : normalizedColumnHeaderNames) {
            Collection<ClinicalAttributesMetadata> metadata = config.getClinicalAttributesMetadata(columnHeader.toUpperCase());
            if (!metadata.isEmpty()) {
                toReturn.put(columnHeader, metadata.iterator().next());
            }
            else if (supplyDefaultClinicalAttributeValues) {
                String[] properties = new String[] { columnHeader, columnHeader, columnHeader, detectDataTypeOfUnkownClinicalAttribute(records, columnHeader), "PATIENT", "1"};
                ClinicalAttributesMetadata m = new ClinicalAttributesMetadata(properties);
                toReturn.put(columnHeader, m);
            }
        }
        return toReturn;
    }

    private static String detectDataTypeOfUnkownClinicalAttribute(List<CSVRecord> records, String columnHeader) {
        String dataType = "NUMBER";
        for (CSVRecord record : records) {
            if (record.isConsistent() && StringUtils.isNotEmpty(record.get(columnHeader)) &&
                (!NumberUtils.isNumber(record.get(columnHeader)))) {
                dataType = "STRING";
                break;
            }
        }
        return dataType;
    }

    private static String addClinicalDataHeader(List<String> normalizedColumnHeaderNames,
                                                Map<String, ClinicalAttributesMetadata> clinicalAttributesMetadata,
                                                String metadataAccessor) throws Exception
    {
        StringBuilder header = new StringBuilder();
        header.append(ImportClinicalData.METADATA_PREFIX);
        for (String columnHeader : normalizedColumnHeaderNames) {
            ClinicalAttributesMetadata metadata = clinicalAttributesMetadata.get(columnHeader);
            if (metadata != null && !metadata.missingAttributes()) {
                Method m = clinicalAttributesMetadata.get(columnHeader).getClass().getMethod(metadataAccessor);
                header.append((String)m.invoke(metadata) + ImportClinicalData.DELIMITER);
            }
            else {
                logMessage(String.format("Unknown clinical attribute (or missing metadata): %s", columnHeader));
                continue;
            }
        }
        return header.toString().trim() + "\n";
    }

    private static String addClinicalDataColumnHeaders(List<String> normalizedColumnHeaderNames,
                                                       Map<String, ClinicalAttributesMetadata> clinicalAttributesMetadata)
    {
        StringBuilder header = new StringBuilder();
    	for (String columnHeader : normalizedColumnHeaderNames) {
            ClinicalAttributesMetadata metadata = clinicalAttributesMetadata.get(columnHeader);
            if (metadata != null && !metadata.missingAttributes()) {
            	header.append(columnHeader + ImportClinicalData.DELIMITER);
            }
        }
        return header.toString().trim() + "\n";
    }

    private static void logMessage(String message)
    {
        if (LOG.isInfoEnabled()) {
            LOG.info(message);
        }
    }
}
