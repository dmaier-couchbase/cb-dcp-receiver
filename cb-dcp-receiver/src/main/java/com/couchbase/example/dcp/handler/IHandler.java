/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.couchbase.example.dcp.handler;

import com.couchbase.client.core.message.dcp.DCPRequest;

/**
 * Handles a DCP result
 * 
 * @author David Maier <david.maier at couchbase.com>
 */
public interface IHandler {

    
    /**
     * Handles the dcp request
     * 
     * @param dcp 
     */
    public void handle(DCPRequest dcp);    
}
