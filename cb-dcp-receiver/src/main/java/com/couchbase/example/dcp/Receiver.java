/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.couchbase.example.dcp;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseCore;
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.core.message.cluster.OpenBucketRequest;
import com.couchbase.client.core.message.cluster.SeedNodesRequest;
import com.couchbase.client.core.message.config.ClusterConfigResponse;
import com.couchbase.client.core.message.dcp.DCPRequest;
import com.couchbase.client.core.message.dcp.OpenConnectionRequest;
import com.couchbase.client.core.message.dcp.StreamRequestRequest;
import com.couchbase.client.core.message.dcp.StreamRequestResponse;
import com.couchbase.client.core.message.kv.GetBucketConfigRequest;
import com.couchbase.example.dcp.handler.AHandler;
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
    private AHandler handler;
    
    /**
     * Initialize the receiver
     * @param nodes
     * @param bucket
     * @param password
     */
    public Receiver(String[] nodes, String bucket, String password, AHandler handler) {
                
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
    public Receiver(String[] nodes, String bucket, String password, AHandler handler, ReceiverEnv env) {
        
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
     * Retrieve the bucket partion size by receiving the Bucket configuration from the cluster configuration
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
