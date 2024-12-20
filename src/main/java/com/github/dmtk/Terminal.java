package com.github.dmtk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public abstract class Terminal {

    private final static Logger log = Logger.getLogger(Terminal.class);
    protected OutputStream streamOut;
    protected boolean end_loop = false;
    protected byte[] buff = new byte[1024];
    protected ByteArrayInputStream in = new ByteArrayInputStream(buff);
    protected String cmd = "";
    protected boolean interruptWaiting = false;
    public static List<String> responseList = new ArrayList<>();
    public void sendCommand(String command) {
    	responseList.clear();
        cmd = command;
        interruptWaiting = true;
        
    }

    public byte[] waitCommand() {

        while (!interruptWaiting) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
                log.error(ex);
            }
        }
        interruptWaiting = false;
        char[] chars = cmd.toCharArray();
        byte[] bytes = Charset.forName("ASCII").encode(CharBuffer.wrap(chars)).array();//encode chars to bytes
        cmd = "";
        return bytes;
    }
    
    public abstract void disconnect() throws IOException ;
    
    
}
