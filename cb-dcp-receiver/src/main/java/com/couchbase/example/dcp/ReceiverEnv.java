/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
