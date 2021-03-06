# Couchbase DCP Receiver

This example code shows how to consume the D(atabase) C(hange) P(rotocol) stream of a Couchbase Server cluster.

* A Core Environment

```
package com.couchbase.example.dcp;

import com.couchbase.client.core.env.DefaultCoreEnvironment;

/**
 * The environment of the receiver
 * 
 * Let's just use the DefaultCoreEnvirnment in this case
 * 
 * @author David Maier <david.maier at couchbase.com>
 */
public class ReceiverEnv extends DefaultCoreEnvironment {
    
    public ReceiverEnv()
    {  
        super(new ReceiverEnvBuilder());
    }
    
    public ReceiverEnv(Builder b)
    {
        super(b);
        
    }    
}
```

* A Core Environment Builder

```
package com.couchbase.example.dcp;

import com.couchbase.client.core.env.DefaultCoreEnvironment;

/**
 * The environment builder
 * 
 * Let's just use the DefaultCoreEnvironment.Builder in this case
 * 
 * @author David Maier <david.maier at couchbase.com>
 */
public class ReceiverEnvBuilder extends DefaultCoreEnvironment.Builder {
    

}
```

* A DCP Receiver

```
package com.couchbase.example.dcp;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseCore;
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.core.message.cluster.OpenBucketRequest;
import com.couchbase.client.core.message.cluster.SeedNodesRequest;
import com.couchbase.client.core.message.dcp.DCPRequest;
import com.couchbase.client.core.message.dcp.OpenConnectionRequest;
import com.couchbase.client.core.message.dcp.StreamRequestRequest;
import com.couchbase.client.core.message.dcp.StreamRequestResponse;
import com.couchbase.client.core.message.kv.GetBucketConfigRequest;
import com.couchbase.example.dcp.handler.IHandler;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import rx.Observable;


/**
 * This is the receiver of the DCP stream
 * 
 * @author David Maier <david.maier at couchbase.com>
 */
public class Receiver {
    
    /**
     * A logger
     */
    private static final Logger LOG = Logger.getLogger(Receiver.class.getName());
    
    
    /**
     * The connection timeout to the cluster in seconds
     */
    public static final int CONNECT_TIMEOUT = 2;
    
    /**
     * The name of the DCP stream
     */
    public static final String STREAM_NAME = "dcp-receiver";
    
    /**
     * At first a Core environment is required
     */
    private final ReceiverEnv env;
    
    /**
     * The core of the Couchbase client. DCP is not yet completely exposed, 
     * so we are using a core component here which is used by the Couchbase
     * client itself.
     */
    private final ClusterFacade core;
    
    I
    /**
     * The nodes to connect to during the bootstrap phase
     */
    private final String[] nodes;
    
    
    /**
     * The bucket to connect to
     */
    private final String bucket;
    
    /**
     * The bucket password
     */
    private final String password;
    
    /**
     * The connection state
     */
    private boolean connected = false;
    
    /**
     * To handle the DCP result
     */
    private IHandler handler;
    
    /**
     * Initialize the receiver
     * @param nodes
     * @param bucket
     * @param password
     */
    public Receiver(String[] nodes, String bucket, String password, IHandler handler) {
                
        this(nodes, bucket, password, handler, new ReceiverEnv());

    }
    
    /**
     * Initialize the receiver by passing an environment
     * 
     * @param nodes
     * @param bucket
     * @param env 
     * @param password
     */
    public Receiver(String[] nodes, String bucket, String password, IHandler handler, ReceiverEnv env) {
        
        this.env = env;
        this.core = new CouchbaseCore(env);
        this.nodes = nodes;
        this.bucket = bucket;
        this.password = password;
        this.handler = handler;
        
    }
    
    /**
     * Since the Receiver is initialized we want to connect to the Couchbase Cluster
     */
    public void connect()
    {
        //This sets up the bootstrap nodes for 
        core.send(new SeedNodesRequest(nodes))
                .timeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .toBlocking()
                .single();
        
        //Now open a bucket connection
        core.send(new OpenBucketRequest(bucket, password))
                .timeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .toBlocking()
                .single();
        
        connected = true;
    }
    
    /**
     * Open the DCP streams and handle them by using the passed handler
     * 
     */
    public void stream()
    {
        core.send(new OpenConnectionRequest(STREAM_NAME, bucket))
                .toList()
                .flatMap(resp -> numOfVBuckets()) //Send a cluster config request and map the result to the number of vBuckets
                .flatMap(count -> requestStreams(count)) //Stream by taking the number of vBuckets into account
                .toBlocking()
                .forEach(dcp -> this.handler.handle(dcp)); //Now handle every result of the stream here
    }
    
    /**
     * Retrieve the number of vBuckets of the Bucket
     * @return 
     */
    private Observable<Integer> numOfVBuckets()
    {
        return core.<GetClusterConfigResponse>send(new GetClusterConfigRequest())
                .map(cfg -> { 
                     
                    CouchbaseBucketConfig bucketCfg = (CouchbaseBucketConfig) cfg.config().bucketConfig(bucket);
                    return bucketCfg.numberOfPartitions();
                            
                } );
    }
    
    /**
     *  Request the streams for all vBuckets
     * 
     * @return 
     */
    private Observable<DCPRequest> requestStreams(int numOfVBuckets)
    {
        return Observable.merge( //Merge the streams to one stream
                Observable.range(0, numOfVBuckets) //For each vBucket
                .flatMap(vBucket -> core.<StreamRequestResponse>send(new StreamRequestRequest(vBucket.shortValue(), bucket))) //Request a stream
                .map(response -> response.stream()) //Return the stream as Observable of DCPRequest
        );               
    }  
}
```
* Log-Handler

```
package com.couchbase.example.dcp.handler;

import com.couchbase.client.core.message.dcp.DCPRequest;
import com.couchbase.client.core.message.dcp.MutationMessage;

/**
 * A logging DCP handler
 * 
 * @author David Maier <david.maier at couchbase.com>
 */
public class LogHandler implements IHandler {

    @Override
    public void handle(DCPRequest dcp) {
       
        if (dcp instanceof MutationMessage)
        {
            MutationMessage msg = (MutationMessage) dcp;
         
            System.out.println("key = " +  msg.key() + ", cas = " +  msg.cas());      
        }      
        
    }
}
```
* Test case
```
package com.couchbase.example.dcp;

import com.couchbase.example.dcp.handler.LogHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test case
 *
 * @author David Maier <david.maier at couchbase.com>
 */
public class ReceiverTest {
    
    
    private static Receiver r;
    
    public ReceiverTest() {
    
        //Make sure that DCP is usable
        System.setProperty("com.couchbase.dcpEnabled", "true");
        r = new Receiver(new String[]{"192.168.7.160"}, "test", "test", new LogHandler());
    }
    
    @BeforeClass
    public static void setUpClass() {
    }

    @Test
    public void testReceiveStream() {

        r.connect();
        r.stream();
    }
}
```
