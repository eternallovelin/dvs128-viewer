/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and openChannel the template in the editor.
 */
package eu.visualize.ini.convnet;

import net.sf.jaer.util.TobiLogger;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.EventPacket;
import static net.sf.jaer.eventprocessing.EventFilter.log;
import net.sf.jaer.eventprocessing.FilterChain;
import net.sf.jaer.graphics.MultilineAnnotationTextRenderer;
import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Font;

/**
 * Extends DavisDeepLearnCnnProcessor to add annotation graphics to show
 * steering decision.
 *
 * @author Tobi
 */
@Description("Displays Visualise steering ConvNet results; subclass of DavisDeepLearnCnnProcessor")
@DevelopmentStatus(DevelopmentStatus.Status.Experimental)
public class VisualiseSteeringConvNet extends DavisDeepLearnCnnProcessor implements PropertyChangeListener {

    private static final int LEFT = 0, CENTER = 1, RIGHT = 2, INVISIBLE = 3; // define output cell types
    volatile private boolean hideSteeringOutput = getBoolean("hideOutput", false);
    volatile private boolean showAnalogDecisionOutput = getBoolean("showAnalogDecisionOutput", false);
    volatile private boolean showStatistics = getBoolean("showStatistics", true);
    private TargetLabeler targetLabeler = null;
    private Error error = new Error();
    protected TextRenderer renderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 10), true, true);
//    /** This object used to publish the results to ROS */
//    public VisualiseSteeringNetRosNodePublisher visualiseSteeringNetRosNodePublisher=new VisualiseSteeringNetRosNodePublisher();

    // UDP output to client, e.g. ROS
    volatile private boolean sendUDPSteeringMessages = getBoolean("sendUDPSteeringMessages", false);
    volatile private boolean sendOnlyNovelSteeringMessages = getBoolean("sendOnlyNovelSteeringMessages", true);
    volatile private byte lastUDPmessage = -1;
    volatile private boolean forceNetworkOutpout = getBoolean("forceNetworkOutpout", false);
    volatile private int forcedNetworkOutputValue = getInt("forcedNetworkOutputValue", 3); // default is prey invisible output
    private String host = getString("host", "localhost");
    private int remotePort = getInt("remotePort", 13331);
    private int localPort = getInt("localPort", 15555);
    private DatagramSocket socket = null;
    private InetSocketAddress client = null;
    private DatagramChannel channel = null;
    private String behavior = null;
    private ByteBuffer udpBuf = ByteBuffer.allocate(2);
    private int seqNum = 0;
    private int countRender = 0;
    private int[] decisionArray = new int[2];
    private int[] decisionLowPassArray = new int[3];
    private int savedDecision = -1;
    private int counterD = 0;
    private static Color colorBehavior=Color.RED;
    private float[] LCRNstate = new float[]{0.5f, 0.5f, 0.5f, 0.5f};
    private boolean flagBehavior = false;
    volatile private int renderingCyclesDecision = getInt("renderingCyclesDecision", 3);
    volatile private boolean apply_LR_RL_constraint = getBoolean("apply_LR_RL_constraint", false);
    volatile private boolean apply_LNR_RNL_constraint = getBoolean("apply_LNR_RNL_constraint", false);
    volatile private boolean apply_CN_NC_constraint = getBoolean("apply_CN_NC_constraint", false);
    volatile private float LCRNstep = getFloat("LCRNstep", 1f);
    private final TobiLogger descisionLogger = new TobiLogger("Decisions", "Decisions of CNN sent to Predator robot Summit XL");
    private final TobiLogger behaviorLogger = new TobiLogger("Behavior", "Behavior of robots as sent back by Predator robot Summit XL");
    BehaviorLoggingThread behaviorLoggingThread = new BehaviorLoggingThread();

    public VisualiseSteeringConvNet(AEChip chip) {
        super(chip);
        String deb = "3. Debug", disp = "1. Display", anal = "2. Analysis";
        String udp = "UDP messages";
        setPropertyTooltip(disp, "showAnalogDecisionOutput", "Shows output units as analog shading rather than binary. If LCRNstep=1, then the analog CNN output is shown. Otherwise, the lowpass filtered LCRN states are shown.");
        setPropertyTooltip(disp, "hideSteeringOutput", "hides steering output unit rendering as shading over sensor image. If the prey is invisible no rectangle is rendered when showAnalogDecisionOutput is deselected.");
        setPropertyTooltip(anal, "pixelErrorAllowedForSteering", "If ground truth location is within this many pixels of closest border then the descision is still counted as corret");
        setPropertyTooltip(disp, "showStatistics", "shows statistics of DVS frame rate and error rate (when ground truth TargetLabeler file is loaded)");
        setPropertyTooltip(udp, "sendUDPSteeringMessages", "sends UDP packets with steering network output to host:port in hostAndPort");
        setPropertyTooltip(udp, "sendOnlyNovelSteeringMessages", "only sends UDP message if it contains a novel command; avoids too many datagrams");
        setPropertyTooltip(udp, "host", "hostname or IP address to send UDP messages to, e.g. localhost");
        setPropertyTooltip(udp, "remotePort", "destination UDP port address to send UDP messages to, e.g. 13331");
        setPropertyTooltip(udp, "localPort", "our UDP port address to recieve UDP messages from robot, e.g. 15555");

        setPropertyTooltip(udp, "forcedNetworkOutputValue", "forced value of network output sent to client (0=left, 1=middle, 2=right, 3=invisible)");
        setPropertyTooltip(udp, "forceNetworkOutpout", "force (override) network output classification to forcedNetworkOutputValue");
        setPropertyTooltip(udp, "apply_LR_RL_constraint", "force (override) network output classification to make sure there is no switching from L to R or viceversa directly");
        setPropertyTooltip(udp, "apply_LNR_RNL_constraint", "force (override) network output classification to make sure there is no switching from L to N and to R or viceversa, since the predator will see the prey back from the same last seen steering output (it spins in the last seen direction)");
        setPropertyTooltip(udp, "apply_CN_NC_constraint", "force (override) network output classification to make sure there is no switching from C to N or viceversa directly");
        setPropertyTooltip(udp, "LCRNstep", "mixture of decisicion outputs over time (LCR or N) to another (if 1, no lowpass filtering, if lower, then slower transitions)");
        setPropertyTooltip(udp, "startLoggingUDPMessages", "start logging UDP messages to a text log file");
        setPropertyTooltip(udp, "stopLoggingUDPMessages", "stop logging UDP messages");
        setPropertyTooltip(disp, "renderingCyclesDecision", "Display robot behavior for these many rendering cycles");
        
        FilterChain chain = new FilterChain(chip);
        targetLabeler = new TargetLabeler(chip); // used to validate whether descisions are correct or not
        chain.add(targetLabeler);
        setEnclosedFilterChain(chain);
        apsDvsNet.getSupport().addPropertyChangeListener(DeepLearnCnnNetwork.EVENT_MADE_DECISION, this);
        descisionLogger.setAbsoluteTimeEnabled(true);
        descisionLogger.setNanotimeEnabled(false);
        behaviorLogger.setAbsoluteTimeEnabled(true);
        behaviorLogger.setNanotimeEnabled(false);
//        dvsNet.getSupport().addPropertyChangeListener(DeepLearnCnnNetwork.EVENT_MADE_DECISION, this);
    }

    @Override
    public synchronized EventPacket<?> filterPacket(EventPacket<?> in) {
        targetLabeler.filterPacket(in);
        EventPacket out = super.filterPacket(in);
        return out;
    }

    public int getPixelErrorAllowedForSteering() {
        return error.getPixelErrorAllowedForSteering();
    }

    public void setPixelErrorAllowedForSteering(int pixelErrorAllowedForSteering) {
        error.setPixelErrorAllowedForSteering(pixelErrorAllowedForSteering);
    }

//    private Boolean correctDescisionFromTargetLabeler(TargetLabeler targetLabeler, DeepLearnCnnNetwork net) {
//        if (targetLabeler.getTargetLocation() == null) {
//            return null; // no location labeled for this time
//        }
//        Point p = targetLabeler.getTargetLocation().location;
//        if (p == null) {
//            if (net.outputLayer.maxActivatedUnit == 3) {
//                return true; // no target seen
//            }
//        } else {
//            int x = p.x;
//            int third = (x * 3) / chip.getSizeX();
//            if (third == net.outputLayer.maxActivatedUnit) {
//                return true;
//            }
//        }
//        return false;
//    }
    @Override
    public void resetFilter() {
        super.resetFilter();
        error.reset();

    }

    @Override
    public synchronized void setFilterEnabled(boolean yes) {
        super.setFilterEnabled(yes);
        if (yes && !targetLabeler.hasLocations()) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    targetLabeler.loadLastLocations();
                }
            };
            SwingUtilities.invokeLater(r);
        }
        if (yes && sendUDPSteeringMessages) {
            try {
                openChannel();
            } catch (IOException ex) {
                log.warning("Caught exception when trying to open datagram channel to host:port - " + ex);
            }
        }
        if (!yes) {
            closeChannel();
        }
    }

    @Override
    public void annotate(GLAutoDrawable drawable) {
        super.annotate(drawable);
        targetLabeler.annotate(drawable);
        if (hideSteeringOutput) {
            return;
        }
        GL2 gl = drawable.getGL().getGL2();
        checkBlend(gl);
        int third = chip.getSizeX() / 3;
        int sy = chip.getSizeY();
        if (apsDvsNet != null && apsDvsNet.outputLayer != null && apsDvsNet.outputLayer.activations != null) {
            drawDecisionOutput(third, gl, sy, apsDvsNet, Color.RED);
        }
//        if (dvsNet != null && dvsNet.outputLayer != null && dvsNet.outputLayer.activations != null && isProcessDVSTimeSlices()) {
//            drawDecisionOutput(third, gl, sy, dvsNet, Color.YELLOW);
//        }
        MultilineAnnotationTextRenderer.resetToYPositionPixels(chip.getSizeY() * .5f);
        MultilineAnnotationTextRenderer.setScale(.3f);
        if (showStatistics) {
            MultilineAnnotationTextRenderer.renderMultilineString(String.format("LCRN states: [L=%6.1f]  [C=%6.1f]  [R%6.1f]  [N=%6.1f]", LCRNstate[0], LCRNstate[1], LCRNstate[2], LCRNstate[3]));
            MultilineAnnotationTextRenderer.setScale(.3f);
            if (dvsSubsampler != null) {
                MultilineAnnotationTextRenderer.renderMultilineString(String.format("DVS subsampler, %d events, inst/avg interval %6.1f/%6.1f ms", getDvsMinEvents(), dvsSubsampler.getLastSubsamplerFrameIntervalUs() * 1e-3f, dvsSubsampler.getFilteredSubsamplerIntervalUs() * 1e-3f));
            }
            if (error.totalCount > 0) {
                MultilineAnnotationTextRenderer.renderMultilineString(error.toString());
            }
        }
        if (behavior != null && flagBehavior == true) {
            int currentBehavior = Integer.parseInt(behavior);
            MultilineAnnotationTextRenderer.setScale(0.8f);
            MultilineAnnotationTextRenderer.resetToYPositionPixels(10);
            MultilineAnnotationTextRenderer.setColor(colorBehavior);
            if (currentBehavior == 4) {
                MultilineAnnotationTextRenderer.renderMultilineString(String.format("Rotating towards prey"));
            }
            if (currentBehavior == 5) {
                MultilineAnnotationTextRenderer.renderMultilineString(String.format("Wandering..."));
            }
            if (currentBehavior == 6) {
                MultilineAnnotationTextRenderer.renderMultilineString(String.format("Prey Caught!"));
            }
            if (currentBehavior == 7) {
                MultilineAnnotationTextRenderer.renderMultilineString(String.format("Chasing"));
            }
            countRender = countRender+1;
            if(countRender > getRenderingCyclesDecision()){
            flagBehavior = false;
            countRender = 0;
            }
        }
        //        if (totalDecisions > 0) {
//            float errorRate = (float) incorrect / totalDecisions;
//            String s = String.format("Error rate %.2f%% (total=%d correct=%d incorrect=%d)\n", errorRate * 100, totalDecisions, correct, incorrect);
//            MultilineAnnotationTextRenderer.renderMultilineString(s);
//        }
    }

    private void drawDecisionOutput(int third, GL2 gl, int sy, DeepLearnCnnNetwork net, Color color) {
        // 0=left, 1=center, 2=right, 3=no target
        int decision = net.outputLayer.maxActivatedUnit;
        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f;
        float[] cv = color.getColorComponents(null);
        if (showAnalogDecisionOutput) {
            final float brightness = .3f; // brightness scale
            for (int i = 0; i < 3; i++) {
                int x0 = third * i;
                int x1 = x0 + third;
                float shade = brightness * chooseOutputToShow(net, i);
                gl.glColor3f((shade * r), (shade * g), (shade * b));
                gl.glRecti(x0, 0, x1, sy);
                gl.glRecti(x0, 0, x1, sy);
            }
            float shade = brightness * chooseOutputToShow(net, 3); // no target
            gl.glColor3f((shade * r), (shade * g), (shade * b));
            gl.glRecti(0, 0, chip.getSizeX(), sy / 8);

        } else if (decision != INVISIBLE) {
            int x0 = third * decision;
            int x1 = x0 + third;
            float shade = .5f;
            gl.glColor3f((shade * r), (shade * g), (shade * b));
            gl.glRecti(x0, 0, x1, sy);
        }
    }

    // returns either network or lowpass filtered output
    private float chooseOutputToShow(DeepLearnCnnNetwork net, int i) {
        if (LCRNstep < 1) {
            return LCRNstate[i];
        } else {
            return net.outputLayer.activations[i];
        }
    }

    public void applyConstraints(DeepLearnCnnNetwork net) {
        int currentDecision = net.outputLayer.maxActivatedUnit;
        float maxLCRN = 0;
        int maxLCRNindex = -1;
        for (int i = 0; i < 4; i++) {
            if (i == currentDecision) {
                LCRNstate[i] = LCRNstate[i] + LCRNstep;
                if (LCRNstate[i] > 1) {
                    LCRNstate[i] = 1;
                }
                if (LCRNstate[i] > maxLCRN) {
                    maxLCRN = LCRNstate[i];
                    maxLCRNindex = i;
                }
            } else {
                LCRNstate[i] = LCRNstate[i] - LCRNstep;
                if (LCRNstate[i] < 0) {
                    LCRNstate[i] = 0;
                }
                if (LCRNstate[i] > maxLCRN) {
                    maxLCRN = LCRNstate[i];
                    maxLCRNindex = i;
                }
            }
        }
        net.outputLayer.maxActivatedUnit = maxLCRNindex;

        if (apply_CN_NC_constraint) {// Cannot switch from C to N and viceversa
            if (currentDecision == 1 && decisionArray[1] == 3) {
                net.outputLayer.maxActivatedUnit = 3;
            } else if (currentDecision == 3 && decisionArray[1] == 1) {
                net.outputLayer.maxActivatedUnit = 1;
            }
        }
        if (apply_LNR_RNL_constraint) { //Remember last position before going to N, the robot will reappear from there
            // Possible transition to N (RN or LN)
            if (currentDecision == 3 && decisionArray[1] == 0) {
                savedDecision = 0;
            } else if (currentDecision == 3 && decisionArray[1] == 2) {
                savedDecision = 2;
            }
            // Possible transition back from N (NR or LN)
            if (savedDecision >= 0) {//Real value saved
                if (currentDecision == 0 && decisionArray[1] == 3) {
                    if (currentDecision != savedDecision) {
                        net.outputLayer.maxActivatedUnit = 3;
                    } else {
                        savedDecision = -1;
                    }
                } else if (currentDecision == 2 && decisionArray[1] == 3) {
                    if (currentDecision != savedDecision) {
                        net.outputLayer.maxActivatedUnit = 3;
                    } else {
                        savedDecision = -1;
                    }
                }
            }
        }
        if (apply_LR_RL_constraint) {// Cannot switch from R to L and viceversa
            if (currentDecision == 0 && decisionArray[1] == 2) {
                net.outputLayer.maxActivatedUnit = 2;
            } else if (currentDecision == 2 && decisionArray[1] == 0) {
                net.outputLayer.maxActivatedUnit = 0;
            }
        }
        //Update decision
        decisionArray[0] = decisionArray[1];
        decisionArray[1] = net.outputLayer.maxActivatedUnit;
    }

    /**
     * @return the hideSteeringOutput
     */
    public boolean isHideSteeringOutput() {
        return hideSteeringOutput;
    }

    /**
     * @param hideSteeringOutput the hideSteeringOutput to set
     */
    public void setHideSteeringOutput(boolean hideSteeringOutput) {
        this.hideSteeringOutput = hideSteeringOutput;
        putBoolean("hideSteeringOutput", hideSteeringOutput);
    }

    /**
     * @return the showAnalogDecisionOutput
     */
    public boolean isShowAnalogDecisionOutput() {
        return showAnalogDecisionOutput;
    }

    /**
     * @param showAnalogDecisionOutput the showAnalogDecisionOutput to set
     */
    public void setShowAnalogDecisionOutput(boolean showAnalogDecisionOutput) {
        this.showAnalogDecisionOutput = showAnalogDecisionOutput;
        putBoolean("showAnalogDecisionOutput", showAnalogDecisionOutput);
    }

    @Override
    synchronized public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() != DeepLearnCnnNetwork.EVENT_MADE_DECISION) {
            super.propertyChange(evt);

        } else {
            DeepLearnCnnNetwork net = (DeepLearnCnnNetwork) evt.getNewValue();
            if (targetLabeler.isLocationsLoadedFromFile()) {
                error.addSample(targetLabeler.getTargetLocation(), net.outputLayer.maxActivatedUnit, net.isLastInputTypeProcessedWasApsFrame());
            }
            applyConstraints(net);
            if (sendUDPSteeringMessages) {
                if (checkClient()) { // if client not there, just continue - maybe it comes back
                    byte msg = (byte) (forceNetworkOutpout ? forcedNetworkOutputValue : net.outputLayer.maxActivatedUnit);
                    if (!sendOnlyNovelSteeringMessages || msg != lastUDPmessage) {
                        lastUDPmessage = msg;
                        udpBuf.clear();
                        udpBuf.put((byte) (seqNum & 0xFF)); // mask bits to cast to unsigned byte value 0-255
                        seqNum++;
                        if (seqNum > 255) {
                            seqNum = 0;
                        }
                        udpBuf.put(msg);
                        String s = String.format("%d\t%d", lastProcessedEventTimestamp, net.outputLayer.maxActivatedUnit);
                        descisionLogger.log(s);
                        try {
//                        log.info("sending buf="+buf+" to client="+client);
//                        log.info("sending seqNum=" + seqNum + " with msg=" + msg);
                            udpBuf.flip();
                            int numBytesSent = channel.send(udpBuf, client);
                            if (numBytesSent != 2) {
                                log.warning("only sent " + numBytesSent);
                            }
                        } catch (IOException e) {
                            log.warning("Exception trying to send UDP datagram to ROS: " + e);
                        }
                    }
                }
            }
        }
    }

    /**
     * @return the sendUDPSteeringMessages
     */
    public boolean isSendUDPSteeringMessages() {
        return sendUDPSteeringMessages;
    }

    /**
     * @param sendUDPSteeringMessages the sendUDPSteeringMessages to set
     */
    synchronized public void setSendUDPSteeringMessages(boolean sendUDPSteeringMessages) {
        this.sendUDPSteeringMessages = sendUDPSteeringMessages;
        putBoolean("sendUDPSteeringMessages", sendUDPSteeringMessages);
        if (sendUDPSteeringMessages) {
            try {
                openChannel();
            } catch (IOException ex) {
                log.warning("Caught exception when trying to open datagram channel to host:port - " + ex);
            }
        } else {
            closeChannel();
        }
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        try {
            InetAddress udpAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            log.warning("can't find " + host + ": caught " + e);
            JOptionPane.showMessageDialog(chip.getAeViewer().getFilterFrame(), e.toString(), "Bad host for UDP steering messages", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.host = host;
        putString("host", host);
    }

    /**
     * @return the remotePort
     */
    public int getRemotePort() {
        return remotePort;
    }

    /**
     * @param remotePort the remotePort to set
     */
    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
        putInt("remotePort", remotePort);
    }

    private class Error {

        int totalCount, totalCorrect, totalIncorrect;
        int[] correct = new int[4], incorrect = new int[4], count = new int[4];
        protected int pixelErrorAllowedForSteering = getInt("pixelErrorAllowedForSteering", 10);
        int dvsTotalCount, dvsCorrect, dvsIncorrect;
        int apsTotalCount, apsCorrect, apsIncorrect;
        char[] outputChars = {'L', 'M', 'R', 'I'};

        public Error() {
            reset();
        }

        void reset() {
            totalCount = 0;
            totalCorrect = 0;
            totalIncorrect = 0;
            Arrays.fill(correct, 0);
            Arrays.fill(incorrect, 0);
            Arrays.fill(count, 0);
            dvsTotalCount = 0;
            dvsCorrect = 0;
            dvsIncorrect = 0;
            apsTotalCount = 0;
            apsCorrect = 0;
            apsIncorrect = 0;
        }

        void addSample(TargetLabeler.TargetLocation gtTargetLocation, int descision, boolean apsType) {
            totalCount++;
            if (apsType) {
                apsTotalCount++;
            } else {
                dvsTotalCount++;
            }

            int third = chip.getSizeX() / 3;

            if (gtTargetLocation != null && gtTargetLocation.location != null) {
                // we have a location that is not null for the target
                int x = (int) Math.floor(gtTargetLocation.location.x);
                int gtDescision = x / third;
                if (gtDescision < 0 || gtDescision > 3) {
                    return; // bad descision output, should not happen
                }
                count[gtDescision]++;
                if (gtDescision == descision) {
                    correct[gtDescision]++;
                    totalCorrect++;
                    if (apsType) {
                        apsCorrect++;
                    } else {
                        dvsCorrect++;
                    }
                } else if (getPixelErrorAllowedForSteering() == 0) {
                    incorrect[gtDescision]++;
                    totalIncorrect++;
                    if (apsType) {
                        apsIncorrect++;
                    } else {
                        dvsIncorrect++;
                    }
                } else {
                    boolean wrong = true;
                    // might be error but maybe not if the descision is e.g. to left and the target location is just over the border to middle
                    float gtX = gtTargetLocation.location.x;
                    if (descision == LEFT && gtX < third + pixelErrorAllowedForSteering) {
                        wrong = false;
                    } else if (descision == CENTER && gtX >= third - pixelErrorAllowedForSteering && gtX <= 2 * third + pixelErrorAllowedForSteering) {
                        wrong = false;
                    } else if (descision == RIGHT && gtX >= 2 * third - pixelErrorAllowedForSteering) {
                        wrong = false;
                    }
                    if (wrong) {
                        incorrect[gtDescision]++;
                        totalIncorrect++;
                        if (apsType) {
                            apsIncorrect++;
                        } else {
                            dvsIncorrect++;
                        }

                    }
                }

            } else { // no target in ground truth (prey out of view)
                count[INVISIBLE]++;
                if (descision == INVISIBLE) {
                    correct[INVISIBLE]++;
                    totalCorrect++;
                    if (apsType) {
                        apsCorrect++;
                    } else {
                        dvsCorrect++;
                    }
                } else {
                    incorrect[INVISIBLE]++;
                    totalIncorrect++;
                    if (apsType) {
                        apsIncorrect++;
                    } else {
                        dvsIncorrect++;
                    }
                }
            }
        }

        @Override
        public String toString() {
//            if (targetLabeler.hasLocations() == false) {
//                return "Error: No ground truth target locations loaded";
//            }
            if (totalCount == 0) {
                return "Error: no samples yet";
            }
            StringBuilder sb = new StringBuilder("Error rates: ");
            sb.append(String.format(" Total=%.1f%% (%d/%d) \n(", (100f * totalIncorrect) / totalCount, totalIncorrect, totalCount));
            for (int i = 0; i < 4; i++) {
                if (count[i] == 0) {
                    sb.append(String.format("%c: 0/0 ", outputChars[i]));
                } else {
                    sb.append(String.format("%c: %.1f%% (%d)  ", outputChars[i], (100f * incorrect[i]) / count[i], count[i]));
                }
            }
            sb.append(String.format("\naps=%.1f%% (%d/%d) dvs=%.1f%% (%d/%d)",
                    (100f * apsIncorrect) / apsTotalCount, apsIncorrect, apsTotalCount,
                    (100f * dvsIncorrect) / dvsTotalCount, dvsIncorrect, dvsTotalCount));

            sb.append(")");
            return sb.toString();
        }

        /**
         * @return the pixelErrorAllowedForSteering
         */
        public int getPixelErrorAllowedForSteering() {
            return pixelErrorAllowedForSteering;
        }

        /**
         * @param pixelErrorAllowedForSteering the pixelErrorAllowedForSteering
         * to set
         */
        public void setPixelErrorAllowedForSteering(int pixelErrorAllowedForSteering) {
            this.pixelErrorAllowedForSteering = pixelErrorAllowedForSteering;
            putInt("pixelErrorAllowedForSteering", pixelErrorAllowedForSteering);
        }

    }

    /**
     * returns true if socket exists and is bound
     */
    private boolean checkClient() {
        if (socket == null) {
            return false;
        }

        try {
            if (socket.isBound()) {
                return true;
            }
            client = new InetSocketAddress(host, remotePort);
//            channel.connect(client); // connecting the channel causes some kind of portunavailable error on linux
            return true;
        } catch (Exception se) { // IllegalArgumentException or SecurityException
            log.warning("While checking client host=" + host + " port=" + remotePort + " caught " + se.toString());
            return false;
        }
    }

    public void openChannel() throws IOException {
        closeChannel();
        channel = DatagramChannel.open();
        socket = channel.socket(); // bind to any available remotePort because we will be sending datagrams with included host:remotePort info
        socket.setTrafficClass(0x10 + 0x08); // low delay
        log.info("opened channel on local port to send UDP messages to ROS.");
    }

    public void closeChannel() {
        if (socket != null) {
            log.info("closing local socket " + socket + " to UDP client");
            socket.close();
            socket = null;
        }

        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ex) {
                Logger.getLogger(VisualiseSteeringConvNet.class.getName()).log(Level.SEVERE, null, ex);
            }
            channel = null;
        }
    }

    /**
     * @return the forceNetworkOutpout
     */
    public boolean isForceNetworkOutpout() {
        return forceNetworkOutpout;
    }

    /**
     * @param forceNetworkOutpout the forceNetworkOutpout to set
     */
    public void setForceNetworkOutpout(boolean forceNetworkOutpout) {
        this.forceNetworkOutpout = forceNetworkOutpout;
        putBoolean("forceNetworkOutpout", forceNetworkOutpout);
    }

    /**
     * @return the forcedNetworkOutputValue
     */
    public int getForcedNetworkOutputValue() {
        return forcedNetworkOutputValue;
    }

    /**
     * @param forcedNetworkOutputValue the forcedNetworkOutputValue to set
     */
    public void setForcedNetworkOutputValue(int forcedNetworkOutputValue) {
        if (forcedNetworkOutputValue < 0) {
            forcedNetworkOutputValue = 0;
        } else if (forcedNetworkOutputValue > 3) {
            forcedNetworkOutputValue = 3;
        }
        this.forcedNetworkOutputValue = forcedNetworkOutputValue;
        putInt("forcedNetworkOutputValue", forcedNetworkOutputValue);
    }

    /**
     * @return the showStatistics
     */
    public boolean isShowStatistics() {
        return showStatistics;
    }

    /**
     * @param showStatistics the showStatistics to set
     */
    public void setShowStatistics(boolean showStatistics) {
        this.showStatistics = showStatistics;
        putBoolean("showStatistics", showStatistics);
    }

    /**
     * @return the LCRNstep
     */
    public float getLCRNstep() {
        return LCRNstep;
    }

    /**
     * @param LCRNstep the LCRNstep to set
     */
    public void setLCRNstep(float LCRNstep) {
        if (LCRNstep > 1) {
            LCRNstep = 1;
        } else if (LCRNstep < .01) {
            LCRNstep = 0.01f;
        }
        this.LCRNstep = LCRNstep;
        putFloat("LCRNstep", LCRNstep);
    }

    /**
     * @return the apply_CN_NC_constraint
     */
    public boolean isApply_CN_NC_constraint() {
        return apply_CN_NC_constraint;
    }

    /**
     * @param apply_CN_NC_constraint the apply_CN_NC_constraint to set
     */
    public void setApply_CN_NC_constraint(boolean apply_CN_NC_constraint) {
        this.apply_CN_NC_constraint = apply_CN_NC_constraint;
        putBoolean("apply_CN_NC_constraint", apply_CN_NC_constraint);
    }

    /**
     * @return the apply_LNR_RNL_constraint
     */
    public boolean isApply_LNR_RNL_constraint() {
        return apply_LNR_RNL_constraint;
    }

    /**
     * @param apply_LNR_RNL_constraint the apply_LNR_RNL_constraint to set
     */
    public void setApply_LNR_RNL_constraint(boolean apply_LNR_RNL_constraint) {
        this.apply_LNR_RNL_constraint = apply_LNR_RNL_constraint;
        putBoolean("apply_LNR_RNL_constraint", apply_LNR_RNL_constraint);
    }

    /**
     * @return the apply_LR_RL_constraint
     */
    public boolean isApply_LR_RL_constraint() {
        return apply_LR_RL_constraint;
    }

    /**
     * @param apply_LR_RL_constraint the apply_LR_RL_constraint to set
     */
    public void setApply_LR_RL_constraint(boolean apply_LR_RL_constraint) {
        this.apply_LR_RL_constraint = apply_LR_RL_constraint;
        putBoolean("apply_LR_RL_constraint", apply_LR_RL_constraint);
    }

    public void doStartLoggingUDPMessages() {
        if (isSendUDPSteeringMessages() == false) {
            JOptionPane.showMessageDialog(chip.getAeViewer().getFilterFrame(), "UDP output is not enabled yet; logging will only occur if sendUDPSteeringMessages is selected");
        }
        if (apsDvsNet != null) {
            if (!apsDvsNet.networkRanOnce) {
                JOptionPane.showMessageDialog(chip.getAeViewer().getFilterFrame(), "Network must run at least once to correctly plot kernels (internal variables for indexing are computed at runtime)");
                return;
            }
            descisionLogger.setEnabled(true);
            descisionLogger.addComment("network is " + apsDvsNet.getXmlFilename());
            descisionLogger.addComment("system.currentTimeMillis lastTimestampUs decisionLCRN");
            behaviorLogger.setEnabled(true);
            behaviorLogger.addComment("network is " + apsDvsNet.getXmlFilename());
            behaviorLogger.addComment("system.currentTimeMillis string_message_from_ROS");
            if (behaviorLoggingThread != null) {
                behaviorLoggingThread.closeChannel();
            }

            behaviorLoggingThread = new BehaviorLoggingThread();
            behaviorLoggingThread.start();
        }
    }

    public void doStopLoggingUDPMessages() {
        if (!descisionLogger.isEnabled()) {
            log.info("Logging not enabled, no effect");
            return;
        }
        descisionLogger.setEnabled(false);
        behaviorLogger.setEnabled(false);
        behaviorLoggingThread.closeChannel();
        behaviorLoggingThread = null;
    }

    /**
     * @return the localPort
     */
    public int getLocalPort() {
        return localPort;
    }

    /**
     * @param localPort the localPort to set
     */
    public void setLocalPort(int localPort) {
        this.localPort = localPort;
        putInt("localPort", localPort);
    }

    /**
     * @return the renderingCyclesDecision
     */
    public int getRenderingCyclesDecision() {
        return renderingCyclesDecision;
    }

    /**
     * @param renderingCyclesDecision the renderingCyclesDecision to set
     */
    public void setRenderingCyclesDecision(int renderingCyclesDecision) {
        this.renderingCyclesDecision = renderingCyclesDecision;
        putInt("renderingCyclesDecision", renderingCyclesDecision);
    }
    /**
     * @return the sendOnlyNovelSteeringMessages
     */
    public boolean isSendOnlyNovelSteeringMessages() {
        return sendOnlyNovelSteeringMessages;
    }

    /**
     * @param sendOnlyNovelSteeringMessages the sendOnlyNovelSteeringMessages to
     * set
     */
    public void setSendOnlyNovelSteeringMessages(boolean sendOnlyNovelSteeringMessages) {
        this.sendOnlyNovelSteeringMessages = sendOnlyNovelSteeringMessages;
        putBoolean("sendOnlyNovelSteeringMessages", sendOnlyNovelSteeringMessages);
    }

    private class BehaviorLoggingThread extends Thread {

//        private DatagramSocket socket = null;
        private InetSocketAddress localSocketAddress = null;
        private DatagramChannel channel = null;
        private ByteBuffer udpBuf = ByteBuffer.allocate(16);
        private int seqNum = 0;
        boolean keepRunning = true;
        int expectedSeqNum = 0;

        @Override
        public void run() {
            try {
                openChannel();

                while (keepRunning) {
                    udpBuf.clear();
                    SocketAddress address = channel.receive(udpBuf);
                    if (udpBuf.limit() == 0) {
                        continue;
                    }
                    seqNum = (int) (udpBuf.get(0));
                    if (seqNum != expectedSeqNum) {
                        log.warning(String.format("dropped %d packets from %s", (seqNum - expectedSeqNum), address.toString()));
                    }
                    behavior = Byte.toString(udpBuf.get(1));
                    flagBehavior = true;
                    behaviorLogger.log(behavior);
                }
                closeChannel();
            } catch (Exception ex) {
                log.warning(ex.toString());
            }
        }

        private void openChannel() throws IOException {
            closeChannel();
            keepRunning = true;
            channel = DatagramChannel.open();
            localSocketAddress = new InetSocketAddress("192.168.1.161", localPort);
            channel.bind(localSocketAddress);
            log.info("opened channel on local port " + localSocketAddress + " to receive UDP messages from ROS.");
        }

        public void closeChannel() {
            keepRunning = false;
            if (channel != null) {
                log.info("closing local channel " + localSocketAddress + " from UDP client");
                try {
                    channel.close();
                    channel = null;
                } catch (IOException ex) {
                    Logger.getLogger(VisualiseSteeringConvNet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

    }

}
