package org.red5.server.io.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.red5.io.flv.IFLV;
import org.red5.io.flv.impl.FLV;
import org.red5.io.flv.impl.FLVService;
import org.red5.io.flv.meta.ICueType;
import org.red5.io.flv.meta.IMetaCue;
import org.red5.io.flv.meta.MetaCue;
import org.red5.io.flv.meta.MetaData;
import org.red5.io.flv.meta.MetaService;
import org.red5.io.flv.meta.Resolver;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

import junit.framework.TestCase;

public class MetaServiceTest extends TestCase {

	private FLVService service;
	private MetaService metaService;

	protected void setUp() throws Exception {
		super.setUp();
				
		// Create a FLV Service
		service = new FLVService();
		
		// Create a Meta Service
		metaService = new MetaService();
		metaService.setSerializer(new Serializer());
		metaService.setDeserializer(new Deserializer());
		metaService.setResolver(new Resolver());
	}
	
	/**
	 * Test writing meta data
	 * @throws IOException
	 */
	public void testWrite() throws IOException {		

		// Get MetaData to embed
		MetaData meta = createMeta();
		// Read in a FLV file for reading tags
		// set the MetaService
		// set the MetaData
		File tmp = new File("tests/test.flv");
		IFLV flv = service.getFLV(tmp);
		flv.setMetaService(metaService);
		flv.setMetaData(meta);
		
	}

	/**
	 * Create some test Metadata for insertion
	 * @return MetaData meta
	 */
	private MetaData createMeta() {
		
		IMetaCue metaCue[] = new MetaCue[2];
		
	  	IMetaCue cp = new MetaCue();
		cp.setName("cue_1");
		cp.setTime(0.01);
		cp.setType(ICueType.EVENT);
		
		IMetaCue cp1 = new MetaCue();
		cp1.setName("cue_2");
		cp1.setTime(0.03);
		cp1.setType(ICueType.EVENT);
		
		// add cuepoints to array
		metaCue[0] = cp;
		metaCue[1] = cp1;		
		
		MetaData meta = new MetaData();
		meta.setMetaCue(metaCue);
		meta.setCanSeekToEnd(true);
		meta.setDuration(300);
		meta.setframeRate(15);
		meta.setHeight(400);
		meta.setWidth(300);
		
		return meta;
		
	}
	

}
