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

