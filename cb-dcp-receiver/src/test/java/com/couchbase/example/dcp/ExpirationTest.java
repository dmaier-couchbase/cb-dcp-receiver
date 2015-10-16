/*
 * Copyright 2015 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.example.dcp;

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static com.couchbase.example.dcp.TestConstants.*;
import com.couchbase.example.dcp.handler.ExpirationHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import rx.Observable;

/**
 *
 * @author David Maier <david.maier at couchbase.com>
 */
public class ExpirationTest {
   
    private static final Logger LOG = Logger.getLogger(ExpirationTest.class.getName());
    private static Receiver r;
    private static AsyncBucket bucket;
    
    
    public ExpirationTest() {
    
        //Make sure that DCP is usable
        System.setProperty("com.couchbase.dcpEnabled", "true");
       
        bucket = CouchbaseCluster.create(new String[]{HOST}).openBucket(BUCKET, PWD).async();
        r = new Receiver(new String[]{HOST}, BUCKET, PWD, new ExpirationHandler());   
    }
    
    @BeforeClass
    public static void setUpClass() {
    }

    /** 
     * These are the test instructions.
     * 
     * Expiration can happen multiple ways
     *  
     *  a.) Via the expiry pager
     *  The expiry pager is a job which frequently runs by making sure that expired
     *  documents are deleted. In this case just make sure that you see the impact by scheduling the
     *  expiry pager more frequently for testing purposes:
     *  
     *  e.g.: ./cbepctl localhost:11210 set flush_param exp_pager_stime 60 -b dcp -p test
     * 
     *  b.) Lazy expiration at access time
     *  If documents are accessed (even via the Admin UI) and they should be already
     *  expired then they will be deleted
     * 
     *  c.) Via compaction
     *  Couchbase has an append only storage engine. Compaction makes sure that 
     *  tombstone objects are removed by rewriting the vBucket file.
     * 
     * How to run this test?
     * 
     * (1) Create an empty bucket
     * (2) Run this test
     * (2a) Wait until the expiry pager did run
     * (2b) After the expiration period access all keys
     * (2c) Run compaction
     *  e.g. : /couchbase-cli bucket-compact -c localhost:8091\
     *         --bucket=dcp -u couchbase -p couchbase
     * (3) You should see the line 'INFORMATION: So far 1.000 documents were deleted.'
     * in the output
     * 
     */
    @Test
    public void testReceiveDeletionStream() throws Exception {

        LOG.info("-- testReceiveStream");
        
        
        LOG.info("Creating some documents with expiration times ...");
        
        List<JsonDocument> docs = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            
              docs.add(JsonDocument.create("key::" + i, 60, JsonObject.fromJson("{ \"content\" : \"This is the document #" + i  +"\"}")));    
        }
        
        Observable.from(docs).flatMap(d -> bucket.upsert(d)).subscribe(
        
                res -> LOG.log(Level.INFO, "Created document with id {0}", res.id())
        );
        
        //Wait a moment before streaming
        Thread.sleep(5000);
        
   
        LOG.info("Connecting ...");
        r.connect();
        LOG.info("Streaming ...");
        r.stream();
    }
}
