package com.couchbase.example.dcp;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.couchbase.example.dcp.handler.LogHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
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
