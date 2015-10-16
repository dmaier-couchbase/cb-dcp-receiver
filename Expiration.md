* Creating somde documents with an expiration time

```
       List<JsonDocument> docs = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            
              docs.add(JsonDocument.create("key::" + i, 60, JsonObject.fromJson("{ \"content\" : \"This is the document #" + i  +"\"}")));    
        }
        
        Observable.from(docs).flatMap(d -> bucket.upsert(d)).subscribe(
        
                res -> LOG.log(Level.INFO, "Created document with id {0}", res.id())
        );
```

* Expiry pager config

```
./cbepctl localhost:11210 set flush_param exp_pager_stime 60 -b dcp -p test
```

* Manual compaction

```
./couchbase-cli bucket-compact -c localhost:8091 --bucket=dcp -u couchbase -p couchbase
```

* Expiration handler

```

package com.couchbase.example.dcp.handler;

import com.couchbase.client.core.message.dcp.DCPRequest;
import com.couchbase.client.core.message.dcp.RemoveMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A logging DCP handler for remove messages
 * 
 * @author David Maier <david.maier at couchbase.com>
 */
public class ExpirationHandler implements IHandler {

    private static final Logger LOG = Logger.getLogger(ExpirationHandler.class.getName());
    private int count = 0;
    
    @Override
    public void handle(DCPRequest dcp) {
     
        if (dcp instanceof RemoveMessage) {
            
            this.count++;
            
            RemoveMessage msg = (RemoveMessage) dcp;
            
            LOG.log(Level.INFO, "Removed document with bucket/vBucket/key: {0}/{1}/{2}", new Object[]{msg.bucket(), msg.partition(), msg.key()});
            LOG.log(Level.INFO, "So far {0} documents were deleted.", this.count);
        }
    } 
    
}
```

* Expiration test

```
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
     * Create 1000 docs with an expiration time and then 
     * consume the DCP stream by handling remove messages
     * 
     * (1) Create an empty bucket
     * (2) Run this test
     * (2a) Wait until the expiry pager did run
     * (2b) After the expiration period access all keys
     * (2c) Run compaction
     * (3) You should see the line 'INFORMATION: So far 1.000 documents were deleted.'
     * in the output
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
```
