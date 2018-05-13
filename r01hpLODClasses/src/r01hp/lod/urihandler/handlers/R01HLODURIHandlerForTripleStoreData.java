package r01hp.lod.urihandler.handlers;

import lombok.extern.slf4j.Slf4j;
import r01f.types.url.Url;
import r01f.types.url.UrlPath;
import r01hp.lod.config.R01HLODURIHandlerConfig;
import r01hp.lod.urihandler.R01HLODHandledURIData;
import r01hp.lod.urihandler.R01HLODHandledURIDataForTripleStoreQuery;
import r01hp.lod.urihandler.R01HLODRequestedURIData;
import r01hp.lod.urihandler.R01HLODTripleStoreQuery;
import r01hp.lod.urihandler.R01HLODURIHandlerEngine;
import r01hp.lod.urihandler.R01HLODURIType;
import r01hp.lod.urihandler.R01HMIMEType;

/**
 * Handles uris like: <pre>/data/{resource}</pre>
 * These URIs are CLIENT-REDIRs after a /id/{resource} with MIME=RDF (see {@link R01HLODURIHandlerEngine})
 * <pre>
 *               |
 *        /data/{resource}  (MIME=RDF)
 *               |
 *   +-----------------------+
 *   |          WEB          |
 *   +-----------+-----------+
 *             proxy 
 *   			 |
 * http://appServer/r01hpLODWar/data/{resource}     
 *               |
 *      +--------v--------+
 *      |     LOD WAR     |
 *      +-----------------+
 *             proxy
 *               |
 *               |
 * http://triplestore/sparql?query=DESCRIBE <IRI>
 *               |
 *      +--------v--------+
 *      |                 |
 *      |   Triple-Store  |
 *      |                 |
 *      +-----------------+
 * </pre>
 */
@Slf4j
public class R01HLODURIHandlerForTripleStoreData 
	 extends R01HLODURIHandlerBase {
/////////////////////////////////////////////////////////////////////////////////////////
//	CONSTRUCTOR
/////////////////////////////////////////////////////////////////////////////////////////	
	public R01HLODURIHandlerForTripleStoreData(final R01HLODURIHandlerConfig config) {
		super(config);
	}
/////////////////////////////////////////////////////////////////////////////////////////
//	
/////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public R01HLODHandledURIData handle(final R01HLODRequestedURIData data) {
		// a) guess the mime from the requested file extension
		R01HMIMEType mime = data.isRequestingHTMLFromMimes() 
									? R01HMIMEType.fromFileExtension(data.getRequestedResourceUrlPath().getFileExtension())
									: data.getBestAcceptedMimeTypeOrDefault(R01HMIMEType.RDFXML);
		if (mime == null) {
			log.warn("Could NOT guess the mime-type from {}; asumming RDF/XML",
					  data.getRequestedResourceUrlPath());
			mime = R01HMIMEType.RDFXML;
		}

		// b) proxy to the triple-store
		// 	  Create a triple-store query like:
		//			http://tripleStoreHost/SPARQLEndPoint?query=DESCRIBE <URI>
		//    where uri={resource}
		UrlPath resourcePath = data.getRequestedResourceUrlPath()
								   .urlPathAfter(R01HLODURIType.DATA.getPathToken());
		Url resourceURI = Url.from(_config.getDataSite(),
					       		   _sanitizeUrlPath(resourcePath));
		R01HLODTripleStoreQuery qry = R01HLODTripleStoreQuery.describe(resourceURI);		
		return new R01HLODHandledURIDataForTripleStoreQuery(data.getLanguage(),
															qry,
															mime);
	}
/////////////////////////////////////////////////////////////////////////////////////////
//	
/////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Sanitizes the UrlPath: when requesting an RDF file (ie: /xxx.rdf), 
	 * the resource URI does NOT have the .rdf extension
	 * <pre>/id/{resource}</pre>
	 * @return
	 */
	public UrlPath _sanitizeUrlPath(final UrlPath requestedUrlPath) {
		if (requestedUrlPath.isFolderPath()) {
			// it's a folder path
			return requestedUrlPath;
		} else {
			// it's a file path... remove the extension
			return UrlPath.from(requestedUrlPath.getFolderPathAssumingFileWithExtension(),
								requestedUrlPath.getFileNameWithoutExtension());
		}
	}
}
