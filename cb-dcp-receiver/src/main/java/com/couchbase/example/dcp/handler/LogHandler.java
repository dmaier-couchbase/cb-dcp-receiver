/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
