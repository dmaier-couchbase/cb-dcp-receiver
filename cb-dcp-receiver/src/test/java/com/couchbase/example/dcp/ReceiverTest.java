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


import com.couchbase.example.dcp.handler.LogHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import static com.couchbase.example.dcp.TestConstants.*;
import java.util.logging.Logger;

/**
 *
 * @author David Maier <david.maier at couchbase.com>
 */
public class ReceiverTest {

    private static final Logger LOG = Logger.getLogger(ReceiverTest.class.getName());

    
    private static Receiver r;
    
    public ReceiverTest() {
    
        //Make sure that DCP is usable
        System.setProperty("com.couchbase.dcpEnabled", "true");
        
        //Init the Receiver
        r = new Receiver(new String[]{HOST}, BUCKET, PWD, new LogHandler());
    }
    
    @BeforeClass
    public static void setUpClass() {
    }

    @Test
    public void testReceiveStream() {

        LOG.info("-- testReceiveStream");
        LOG.info("Connecting ...");
        r.connect();
        LOG.info("Streaming ...");
        r.stream();
    }
}
