package com.github.dmtk;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.log4j.Logger;

public class Telnet extends Terminal implements Runnable, TelnetNotificationHandler {

    private static TelnetClient tc = null;
    private final String remoteip;
    private final int remoteport;
    private final PrintStream out;
    private final static Logger log = Logger.getLogger(Telnet.class);
    private final int defaultTelnetPort = 23;
    public Telnet(String remoteip, PrintStream out) {

        this.remoteip = remoteip;
        this.remoteport = defaultTelnetPort;
        this.out = out;

    }

    public Telnet(String remoteip, int remoteport, PrintStream out) {

        this.remoteip = remoteip;
        this.remoteport = remoteport;
        this.out = out;

    }

    public void execute() {

        end_loop = false;
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream("file.log", true);
        } catch (IOException ex) {
            log.error(ex);
        }
        tc = new TelnetClient();
        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, false);
        EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);
        try {
            tc.addOptionHandler(ttopt);
            tc.addOptionHandler(echoopt);
            tc.addOptionHandler(gaopt);
        } catch (InvalidTelnetOptionException ex) {
            log.error(ex);
        }

        while (!end_loop) {

            try {
                tc.connect(remoteip, remoteport);
                Thread reader = new Thread(new Telnet(remoteip, remoteport, out));
                tc.registerNotifHandler(new Telnet(remoteip, remoteport, out));
                reader.start();
                OutputStream outstr = tc.getOutputStream();
                byte[] buff = new byte[1024];
                ByteArrayInputStream in;
                int ret_read = 0;
                do {
                    buff = this.waitCommand();
                    in = new ByteArrayInputStream(buff);
                    try {
                        ret_read = in.read(buff);
                        //System.out.print(ret_read + " " + new String(buff, 0, ret_read) + "\n");
                        if (ret_read > 0) {
                            if ((new String(buff, 0, ret_read)).startsWith("AYT")) {
                                try {
                                    log.info("AYT response:" + tc.sendAYT(5000));
                                } catch (IOException e) {
                                    log.error("Exception waiting AYT response: " + e.getMessage());
                                } catch (IllegalArgumentException ex) {
                                    log.error(ex);
                                } catch (InterruptedException ex) {
                                    log.error(ex);
                                }
                            } else if ((new String(buff, 0, ret_read)).startsWith("OPT")) {
                                log.info("Status of options:");
                                for (int ii = 0; ii < 25; ii++) {
                                    log.info("Local Option " + ii + ":" + tc.getLocalOptionState(ii) + " Remote Option " + ii + ":" + tc.getRemoteOptionState(ii));
                                }
                            } else if ((new String(buff, 0, ret_read)).startsWith("REGISTER")) {
                                StringTokenizer st = new StringTokenizer(new String(buff));
                                try {
                                    st.nextToken();
                                    int opcode = Integer.parseInt(st.nextToken());
                                    boolean initlocal = Boolean.parseBoolean(st.nextToken());
                                    boolean initremote = Boolean.parseBoolean(st.nextToken());
                                    boolean acceptlocal = Boolean.parseBoolean(st.nextToken());
                                    boolean acceptremote = Boolean.parseBoolean(st.nextToken());
                                    SimpleOptionHandler opthand = new SimpleOptionHandler(opcode, initlocal, initremote,
                                            acceptlocal, acceptremote);
                                    tc.addOptionHandler(opthand);
                                } catch (NumberFormatException | InvalidTelnetOptionException e) {
                                    if (e instanceof InvalidTelnetOptionException) {
                                        log.error("Error registering option: " + e.getMessage());
                                    } else {
                                        log.error("Invalid REGISTER command.");
                                        log.error("Use REGISTER optcode initlocal initremote acceptlocal acceptremote");
                                        log.error("(optcode is an integer.)");
                                        log.error("(initlocal, initremote, acceptlocal, acceptremote are boolean)");
                                    }
                                }
                            } else if ((new String(buff, 0, ret_read)).startsWith("UNREGISTER")) {
                                StringTokenizer st = new StringTokenizer(new String(buff));
                                try {
                                    st.nextToken();
                                    int opcode = (new Integer(st.nextToken())).intValue();
                                    tc.deleteOptionHandler(opcode);
                                } catch (Exception e) {
                                    if (e instanceof InvalidTelnetOptionException) {
                                        log.error("Error unregistering option: " + e.getMessage());
                                    } else {
                                        log.error("Invalid UNREGISTER command.");
                                        log.error("Use UNREGISTER optcode");
                                        log.error("(optcode is an integer)");
                                    }
                                }
                            } else if ((new String(buff, 0, ret_read)).startsWith("SPY")) {
                                tc.registerSpyStream(fout);
                            } else if ((new String(buff, 0, ret_read)).startsWith("UNSPY")) {
                                tc.stopSpyStream();
                            } else {
                                try {
                                    outstr.write(buff, 0, ret_read);
                                    outstr.flush();
                                } catch (IOException e) {
                                    end_loop = true;
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("Exception while reading keyboard:" + e.getMessage());
                        end_loop = true;
                    }
                } while ((ret_read > 0) && (end_loop == false));

                try {
                    tc.disconnect();
                } catch (IOException e) {
                	System.out.println("TEST1 disconnect");
                    log.error("Exception while connecting:" + e.getMessage());

                }
            } catch (IOException e) {
            	System.out.println("TEST2 disconnect");
                log.error("Exception while connecting:" + e.getMessage());

            }
        }
        System.out.println("TEST3 disconnected");
    }

    /**
     * *
     * Callback method called when TelnetClient receives an option negotiation
     * command.
     * <p>
     * @param negotiation_code - type of negotiation command received
     * (RECEIVED_DO, RECEIVED_DONT, RECEIVED_WILL, RECEIVED_WONT)
     * <p>
     * @param option_code - code of the option negotiated
     * <p>
     **
     */
//    @Override
    public void receivedNegotiation(int negotiation_code, int option_code) {
        String command = null;
        if (negotiation_code == TelnetNotificationHandler.RECEIVED_DO) {
            command = "DO";
        } else if (negotiation_code == TelnetNotificationHandler.RECEIVED_DONT) {
            command = "DONT";
        } else if (negotiation_code == TelnetNotificationHandler.RECEIVED_WILL) {
            command = "WILL";
        } else if (negotiation_code == TelnetNotificationHandler.RECEIVED_WONT) {
            command = "WONT";
        }
        log.info("Received " + command + " for option code " + option_code);
    }

    /**
     * *
     * Reader thread. Reads lines from the TelnetClient and echoes them on the
     * screen. *
     */
//    @Override
    public void run() {
        InputStream instr = tc.getInputStream();
        try {
            byte[] buff = new byte[1024];
            int ret_read = 0;

            do {
                ret_read = instr.read(buff);
                if (ret_read > 0) {
                	String response = new String(buff, 0, ret_read);
                    out.println(response);
                    if(response.trim().length() > 0)
                    {
                    	responseList.add(response);
                    }
                }
            } while (ret_read >= 0);
        } catch (Exception e) {
            log.error("Exception while reading socket:" + e.getMessage());

        }
        try {
            tc.disconnect();
            System.out.println("TesT 4 Disconnected");
        } catch (Exception e) {
            log.error("Exception while closing telnet:" + e.getMessage());

        }
        System.out.println("TesT 5 Disconnected");
    }

    @Override
    public void disconnect() throws IOException {
        end_loop = true;
        if (tc.isConnected()) {
            tc.disconnect();
        }

    }
    
    public String getResponse()
    {
    	String responseFull = "";
    	try {
	    	for(String response : responseList)
			{
	    		responseFull = responseFull + response;
			}
    	} catch (Exception e)
    	{
    		
    	}
    	return responseFull;
    }
    
    public void clearResponse()
    {
    	responseList.clear();
    }

}
