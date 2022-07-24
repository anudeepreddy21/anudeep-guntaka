// Simulator.class  This class defines the graphical layout,
//                  relationships between thermodynamic components,
//                  and simulator loop.
//
// Developed for:  University Of Toronto
//                 Faculty of Applied Science and Engineering
//                 Department of Mechanical and Industrial Engineering
//                 Cognitive Engineering Laboratory
//
//    Written by:  Bruno Cosentino
//                 &  Alex S. Ross
//   Last Update:  March 31, 1999
//
//    Modified by: Signe Bray
//		  Advanced Interface Design Lab
//		  University of Waterloo
//
//    Date: April, 2003
//
//    Modifications: 
//	1 - added a Log member to the Simulator class
//	2 - added a startLog method to start a log file with the initial settings
//	3 - when a simulation is  started opens dialog to prompt for log information,
//	    uses ParamDialog class
//	4 - writes the reason for simulation termination to log file
//	5 - changed errorDialog to terminationDialog
//
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

final class Simulator extends Panel implements Runnable {

   private static final int CONFIGURATION_FILE_LINES = 250;
   public final static int NEVER = -60000;
   public final static Color COLOR_BACKGROUND = new Color(204, 204, 204);  // gray color
   public final static Color COLOR_MASSFLOW = Color.yellow;
   public final static Color COLOR_TEMPERATURE = new Color(255, 80, 0);    // red color
   public final static Color COLOR_GOAL = Color.green;
   public final static Color COLOR_RESERVOIR = new Color(42, 200, 210);   // blue color
   public final static Color COLOR_ENERGY = Color.orange;
   public final static Color COLOR_INOUTENERGY = COLOR_TEMPERATURE;
   public final static int PHYSICAL = 0;
   public final static int PHYSICALandFUNCTIONAL = 1;
   public final static int SETTINGS = 2;
   public final static int FLOWS = 3;
   public final static int PRINCIPLES = 4;
   public final static int GOALS = 5;
   private String[] inputFile;          // array used to hold each line of config file
   private int userInterfaceType;
   private int dt;
   private int steadyLimit;
   private int steadyMinTime;
   private double temperatureMargin;
   private double demandMargin;
   private Score score;
   private TimerCanvas timerCanvas;
   private HiddenHeater HH0;
   private TemperatureMeterCanvas T0;
   private TemperatureNameCanvas T0Name;
   Pump PA, PB;
    Valve VA, VB;
   private Splitter SA, SB;
   private Valve VA1, VA2, VB1, VB2;
   private Mixer M1, M2;
   private Heater H1, H2;
   private HiddenHeater HH1, HH2;
   private Reservoir R1, R2;
   private FirstSplitterCanvas firstSplitterCanvas;
   private MixerCanvas mixerCanvas;
   private PFMixerCanvas PFmixerCanvas;
   private CardLayout layout = new CardLayout();
   private Panel physicalPanel = new Panel();
   private Panel physicalAndFunctionalPanel = new Panel();
   private Panel settingsPanel = new Panel();
   private Panel flowsPanel = new Panel();
   private Panel principlesPanel = new Panel();
   private Panel goalsPanel = new Panel();
   private Thread runner = null;

   //Signe: added static log member, can be updated by other classes.
   public static Log log;
   public static boolean log_started = false;
   String conName;

   public Simulator() {
      Frame dummyFrame = new Frame();  // dummy frame (invisible) needed to show file-dialog
      FileDialog openConfigurationFileDialog;
      openConfigurationFileDialog = new FileDialog(dummyFrame, "Load Scenario...", FileDialog.LOAD);
      openConfigurationFileDialog.setDirectory("config");
      openConfigurationFileDialog.show();
      String configurationFilename = ".//config" + File.separatorChar;
      conName = openConfigurationFileDialog.getFile();
      configurationFilename += conName;
      
      dummyFrame.dispose();
      // read in config file and assign each line to an array entry
      try {
         LineNumberReader lnr = new LineNumberReader(new FileReader(configurationFilename));
         inputFile = new String[CONFIGURATION_FILE_LINES+1];
         String s;
         while((s=lnr.readLine()) != null)
            inputFile[lnr.getLineNumber()] = s;
         lnr.close();
         }
      catch(IOException e) {
         System.out.print("Error: " + e);
         System.exit(1);
         }
      dt = getTime(12);
      steadyLimit = getTime(13)*60;
      steadyMinTime = getTime(14)*60;
      temperatureMargin = getDouble(15);
      demandMargin = getDouble(16);
      score = new Score();
      timerCanvas = new TimerCanvas();
      HH0 = new HiddenHeater(getString(42), getDouble(43), getDouble(44),
                             getDouble(45), getTime(46), getDouble(47),
                             getTime(48)*60, getDouble(49), getTime(50)*60);
      T0 = new TemperatureMeterCanvas(HH0.getHeatFlowOut(), HH0.getMaximumHeatFlowOut());
      T0Name = new TemperatureNameCanvas("T0");
      PA = new Pump(getString(76), getBoolean(77), getDouble(78), getDouble(79),
                    getTime(80), getTime(81)*60, getTime(82));
      PB = new Pump(getString(118), getBoolean(119), getDouble(120), getDouble(121),
                    getTime(122), getTime(123)*60, getTime(124));
      VA = new Valve(getString(84), getDouble(85), getDouble(86),
                     getDouble(87), getTime(88), getDouble(89),
                     getTime(90)*60, getDouble(91), getTime(92)*60);
      VB = new Valve(getString(126), getDouble(127), getDouble(128),
                     getDouble(129), getTime(130), getDouble(131),
                     getTime(132)*60, getDouble(133), getTime(134)*60);
      SA = new Splitter("SA", getDouble(114));
      SB = new Splitter("SA", getDouble(156));
      VA1 = new Valve(getString(94), getDouble(95), getDouble(96),
                     getDouble(97), getTime(98), getDouble(99),
                     getTime(100)*60, getDouble(101), getTime(102)*60);
      VA2 = new Valve(getString(104), getDouble(105), getDouble(106),
                     getDouble(107), getTime(108), getDouble(109),
                     getTime(110)*60, getDouble(111), getTime(112)*60);
      VB1 = new Valve(getString(136), getDouble(137), getDouble(138),
                     getDouble(139), getTime(140), getDouble(141),
                     getTime(142)*60, getDouble(143), getTime(144)*60);
      VB2 = new Valve(getString(146), getDouble(147), getDouble(148),
                     getDouble(149), getTime(150), getDouble(151),
                     getTime(152)*60, getDouble(153), getTime(154)*60);
      M1 = new Mixer("M1");
      M2 = new Mixer("M2");
      H1 = new Heater("H1", getDouble(160), getDouble(161), getDouble(162),
                      getDouble(163), getTime(164), getTime(165)*60, getDouble(166));
      H2 = new Heater("H2", getDouble(169), getDouble(170), getDouble(171),
                      getDouble(172), getTime(173), getTime(174)*60, getDouble(175));
      HH1 = new HiddenHeater(getString(53), getDouble(54), getDouble(55),
                     getDouble(56), getTime(57), getDouble(58),
                     getTime(59)*60, getDouble(60), getTime(61)*60);
      HH2 = new HiddenHeater(getString(64), getDouble(65), getDouble(66),
                     getDouble(67), getTime(68), getDouble(69),
                     getTime(70)*60, getDouble(71), getTime(72)*60);
      R1 = new Reservoir("Reservoir 1", getDouble(180), getDouble(181), getDouble(182),
                         getDouble(183), getDouble(184), demandMargin, getDouble(185),
                         temperatureMargin, getDouble(186), getDouble(187), getDouble(188),
                         getDouble(189), getDouble(190), getDouble(191), getDouble(192),
                         getDouble(193), getDouble(194), getDouble(195), getDouble(197),
                         getDouble(199), getTime(201)*60, getTime(202),
                         // outflow valve (NOTE: DuressJ does not consider name or maxflow)
                         getDouble(234), getDouble(235), getTime(236),
                         getDouble(237), getTime(238)*60, getDouble(239), getTime(240)*60,
                         // demand inialization (modellled as valve)
                         getString(20), getDouble(21), getDouble(22), getDouble(23), getTime(24),
                         getDouble(25), getTime(26)*60, getDouble(27), getTime(28)*60);
      R2 = new Reservoir("Reservoir 2", getDouble(206), getDouble(207), getDouble(208),
                         getDouble(209), getDouble(210), demandMargin, getDouble(211),
                         temperatureMargin, getDouble(212), getDouble(213), getDouble(214),
                         getDouble(215), getDouble(216), getDouble(217), getDouble(218),
                         getDouble(219), getDouble(220), getDouble(221), getDouble(223),
                         getDouble(225), getTime(227)*60, getTime(228),
                         // outflow valve (NOTE: DuressJ does not consider name or maxflow)
                         getDouble(244), getDouble(245), getTime(246),
                         getDouble(247), getTime(248)*60, getDouble(249), getTime(250)*60,
                         // demand inialization (modellled as valve)
                         getString(30), getDouble(31), getDouble(32), getDouble(33), getTime(34),
                         getDouble(35), getTime(36)*60, getDouble(37), getTime(38)*60);
      firstSplitterCanvas = new FirstSplitterCanvas();
      mixerCanvas = new MixerCanvas();
      PFmixerCanvas = new PFMixerCanvas();

      setLayout(layout);
      setBackground(COLOR_BACKGROUND);
      addComponentListener(new ComponentAdapter() {
                              public void componentResized(ComponentEvent event) {
                                 changeUserInterface(userInterfaceType);
                                 }
                              });
      physicalPanel.setLayout(null);
      physicalAndFunctionalPanel.setLayout(null);
      settingsPanel.setLayout(null);
      flowsPanel.setLayout(null);
      principlesPanel.setLayout(null);
      goalsPanel.setLayout(null);
            
           
      }

   //Signe: create log file, with initial settings...
   public final void startLog(String name, String trial) {

      log = new Log(name, trial, conName, userInterfaceType, getDouble(86), getDouble(96), getDouble(106), getDouble(128), getDouble(138), getDouble(148), getDouble(234), getDouble(244), getBoolean(77), getBoolean(119), getDouble(161), getDouble(170), getDouble(182), getDouble(208)); 
      log_started = true;
   }

   public final double getDouble(int lineNumber) {
      StringTokenizer t = new StringTokenizer(inputFile[lineNumber]);
      double d = new Double(t.nextToken()).doubleValue();
      return d;
      }

   public final int getTime(int lineNumber) {
      StringTokenizer t = new StringTokenizer(inputFile[lineNumber]);
      double d = new Double(t.nextToken()).doubleValue()*1000;
      return (int)d;
      }

   public final String getString(int lineNumber) {
      StringTokenizer t = new StringTokenizer(inputFile[lineNumber]);
      return t.nextToken();
      }

   public final int getInteger(int lineNumber) {
      StringTokenizer t = new StringTokenizer(inputFile[lineNumber]);
      int i = new Integer(t.nextToken()).intValue();
      return i;
      }

   public final boolean getBoolean(int lineNumber) {
       StringTokenizer t = new StringTokenizer(inputFile[lineNumber]);
       boolean b = new Boolean(t.nextToken()).booleanValue();
       return b;
   }

   public final Insets getInsets() {
      return new Insets(5, 5, 0, 0);
      }

   public void start() {
       
       //Signe: added a dialog to prompt for log information
      openParameterDialog();
      
      if (runner == null) {
         runner = new Thread(this);
         runner.start();
         }
      else{System.out.println("RUNNER NOT NULL.");}
       
      }

   public final void pause() {
      runner.suspend();
      }

   public final void resume() {
      runner.resume();
      }

   public void stop() {
      if (runner != null && runner.isAlive())
         runner.stop();
      runner = null;
      //runner = new Thread(this);
      }

   public void run() {
      
      int t = 0;
      int steadyTime = 0;
      while(runner != null) {
            
          
//          System.out.println("Timer = " + t);
    	  
    	  System.out.println("Pump A status:"+PA.pumpState+";  VA Mass Flow out: "+ VA.getMassFlowOut()+ ";  VA1 Mass Flow out:"+VA1.getMassFlowOut()+";  VA2 Mass Flow out:"+VA2.getMassFlowOut());

          if(log_started)
            log.writeLog(t);
          log.printLog("Hi this is where you enter your steps");

          
         // increment screen timer
         if(t%1000 == 0)
            timerCanvas.setTime(t/1000);
         //////////////////////////////////////////////////////////////////
         //// calculate hidden heater outputs ////////////////////////////
         HH0.checkForFault(t, dt);
         HH1.checkForFault(t, dt);
         HH2.checkForFault(t, dt);
         /////////////////////////////////////////////////////////////////
         //// update incoming water temperature meter ///////////////////
         T0.setTemperatureSetting(HH0.getHeatFlowOut());
         /////////////////////////////////////////////////////////////////
         ////////////////////////////////////////////////////////////////
         ////// calculate demand ///////////////////////////////////////
         R1.calculateDemand(t, dt);
         R2.calculateDemand(t, dt);
         /////////////////////////////////////////////////////////////////
         ////////////////////////////////////////////////////////////////
         ////// calculate allowable mass flow through stream 'A' ///////
         VA1.calculateResistance(t, dt);
         VA2.calculateResistance(t, dt);
         SA.calculateResistance(VA1.getValveOpening(), VA2.getValveOpening());
         VA.calculateResistance(t, dt);
         PA.setMaximumPipeFlow(VA.getValveOpening(), SA.getMaximumAllowableMassFlow());
         ///////////////////////////////////////////////////////////////////
         ////// set mass flow and temperature through stream 'A' //////////
         PA.setMassFlowOut(t, dt);
         PA.setTemperatureOut(HH0.getHeatFlowOut());
         VA.setMassFlowOut(PA.getMassFlowOut());
         VA.setTemperatureOut(PA.getTemperatureOut());
         SA.setMassFlowOut(VA.getMassFlowOut(), VA1.getValveOpening(), VA2.getValveOpening());
         SA.setTemperatureOut(VA.getTemperatureOut());
         VA1.setMassFlowOut(SA.getMassFlowOut());
         VA1.setTemperatureOut(SA.getTemperatureOut());
         VA2.setMassFlowOut(SA.getMassFlowOut2());
         VA2.setTemperatureOut(SA.getTemperatureOut2());
         //////////////////////////////////////////////////////////////////////////
         /////////////////////////////////////////////////////////////////////////
         ////// calculate allowable mass flow through stream 'B' ////////////////
         VB1.calculateResistance(t, dt);
         VB2.calculateResistance(t, dt);
         SB.calculateResistance(VB1.getValveOpening(), VB2.getValveOpening());
         VB.calculateResistance(t, dt);
         PB.setMaximumPipeFlow(VB.getValveOpening(), SB.getMaximumAllowableMassFlow());
         ///////////////////////////////////////////////////////////////////
         ////// set mass flow and temperatute through stream 'B' //////////
         PB.setMassFlowOut(t, dt);
         PB.setTemperatureOut(HH0.getHeatFlowOut());
         VB.setMassFlowOut(PB.getMassFlowOut());
         VB.setTemperatureOut(PB.getTemperatureOut());
         SB.setMassFlowOut(VB.getMassFlowOut(), VB1.getValveOpening(), VB2.getValveOpening());
         SB.setTemperatureOut(VB.getTemperatureOut());
         VB1.setMassFlowOut(SB.getMassFlowOut());
         VB1.setTemperatureOut(SB.getTemperatureOut());
         VB2.setMassFlowOut(SB.getMassFlowOut2());
         VB2.setTemperatureOut(SB.getTemperatureOut2());
         //////////////////////////////////////////////////////////////////////////
         /////////////////////////////////////////////////////////////////////////
         //// check to see if pumps are broken //////////////////////////////////
         if(PA.getMaximumMassFlowOut() == 0)
            displayError(PA.getName() + " blew up because valves were closed.");
         if(PB.getMaximumMassFlowOut() == 0)
            displayError(PB.getName() + " blew up because valves were closed.");
         ///////////////////////////////////////////////////////////////////////
         //////////////////////////////////////////////////////////////////////
         ////// set mass flow and temperature through mixers '1' and '2' /////
         M1.setMassFlowOut(VA1.getMassFlowOut(), VB1.getMassFlowOut());
         M1.setTemperatureOut(VA1.getTemperatureOut(), VB1.getTemperatureOut());
         M2.setMassFlowOut(VA2.getMassFlowOut(), VB2.getMassFlowOut());
         M2.setTemperatureOut(VA2.getTemperatureOut(), VB2.getTemperatureOut());
         /////////////////////////////////////////////////////////////////////////////
         ////////////////////////////////////////////////////////////////////////////
         ////// calculate visible heaters 'H1' and 'H2' ////////////////////////////
         H1.setHeatFlowOut(t, dt);
         H2.setHeatFlowOut(t, dt);
         ///////////////////////////////////////////////////////////////////////////
         //////////////////////////////////////////////////////////////////////////
         ////// calculate allowable mass flow through reservoir output valves ////
         R1.calculateResistance(t, dt);
         R2.calculateResistance(t, dt);
         //////////////////////////////////////////////////////////////////////
         /////////////////////////////////////////////////////////////////////
         ////// calculate Reservoir settings ////////////////////////////////
         R1.setMassFlowIn(M1.getMassFlowOut());
         R1.setTemperatureIn(M1.getTemperatureOut());
         R1.setMassFlowOut(R1.getValveOpening());
         R1.setHeaterEnergyIn(H1.getHeatFlowOut());
         R1.setHiddenHeaterEnergyIn(HH1.getHeatFlowOut());
         R1.calculateReservoir(t, dt);
         if(R1.getError() != 0) {
            if(R1.getError() == Reservoir.ERROR_BOIL)
               displayError("The water in " + R1.getName() + " reached boiling point.");
            else if(R1.getError() == Reservoir.ERROR_OVERHEAT)
               displayError(R1.getName() + " was heated empty.");
            else if(R1.getError() == Reservoir.ERROR_OVERFLOW)
               displayError(R1.getName() + " overflowed.");
            }
         //////////////////////////////////////////////////////////////
         R2.setMassFlowIn(M2.getMassFlowOut());
         R2.setTemperatureIn(M2.getTemperatureOut());
         R2.setMassFlowOut(R2.getValveOpening());
         R2.setHeaterEnergyIn(H2.getHeatFlowOut());
         R2.setHiddenHeaterEnergyIn(HH2.getHeatFlowOut());
         R2.calculateReservoir(t, dt);
         if(R2.getError() != 0) {
            if(R2.getError() == Reservoir.ERROR_BOIL)
               displayError("The water in " + R2.getName() + " reached boiling point.");
            else if(R2.getError() == Reservoir.ERROR_OVERHEAT)
               displayError(R2.getName() + " was heated empty.");
            else if(R2.getError() == Reservoir.ERROR_OVERFLOW)
               displayError(R2.getName() + " overflowed.");
            }
         ///////////////////////////////////////////////////////////////
         //////////////////////////////////////////////////////////////
         //////  update scores ///////////////////////////////////////
         // low temperature
         if(R1.getTemperatureOut() < R1.getDemandTemperature() - temperatureMargin)
            score.setScore(0, 0, R1.getMassFlowOut()*dt/1000);
         // high temperature
         else if(R1.getTemperatureOut() > R1.getDemandTemperature() + temperatureMargin)
            score.setScore(2, 0, R1.getMassFlowOut()*dt/1000);
         // good temperature and low flow
         else if(R1.getMassFlowOut() < R1.getDemand() - demandMargin)
            score.setScore(1, 0, R1.getMassFlowOut()*dt/1000);
         // good temperature and high flow
         else if(R1.getMassFlowOut() > R1.getDemand() + demandMargin) {
            // desired level
            score.setScore(1, 1, (R1.getDemand() + demandMargin)*dt/1000);
            // excess flow
            score.setScore(1, 2, (R1.getMassFlowOut() - R1.getDemand() - demandMargin)*dt/1000);
            }
         // good temperature and good flow
         else
            score.setScore(1, 1, R1.getMassFlowOut()*dt/1000);
         /////////////////////////////////////////////////////////////
         // low temperature
         if(R2.getTemperatureOut() < R2.getDemandTemperature() - temperatureMargin)
            score.setScore(0, 0, R2.getMassFlowOut()*dt/1000);
         // high temperature
         else if(R2.getTemperatureOut() > R2.getDemandTemperature() + temperatureMargin)
            score.setScore(2, 0, R2.getMassFlowOut()*dt/1000);
         // good temperature and low flow
         else if(R2.getMassFlowOut() < R2.getDemand() - demandMargin)
            score.setScore(1, 0, R2.getMassFlowOut()*dt/1000);
         // good temperature and high flow
         else if(R2.getMassFlowOut() > R2.getDemand() + demandMargin) {
            // desired level
            score.setScore(1, 1, (R2.getDemand() + demandMargin)*dt/1000);
            // excess flow
            score.setScore(1, 2, (R2.getMassFlowOut() - R2.getDemand() - demandMargin)*dt/1000);
            }
         // good temperature and good flow
         else
            score.setScore(1, 1, R2.getMassFlowOut()*dt/1000);
         ////////////////////////////////////////////////////////////
         ///////////////////////////////////////////////////////////
         ////// calculate steady state time////////////////////////
         if(R1.getMassFlowOut() >= R1.getDemand() - demandMargin &&
            R1.getMassFlowOut() <= R1.getDemand() + demandMargin &&
            R1.getTemperatureOut() >= R1.getDemandTemperature() - temperatureMargin &&
            R1.getTemperatureOut() <= R1.getDemandTemperature() + temperatureMargin &&
            R2.getMassFlowOut() >= R2.getDemand() - demandMargin &&
            R2.getMassFlowOut() <= R2.getDemand() + demandMargin &&
            R2.getTemperatureOut() >= R2.getDemandTemperature() - temperatureMargin &&
            R2.getTemperatureOut() <= R2.getDemandTemperature() + temperatureMargin)
         {
             steadyTime += dt;
             System.out.println("STEADY STATE time-> " +steadyTime + " limit-> " + steadyLimit);
             if(log_started)
                log.updateState(steadyTime);
         }
         else
         {

             steadyTime = 0;   
             if(log_started)
                log.updateState(0);

         }
        if(steadyMinTime != NEVER && steadyTime >= steadyLimit)
            displayEnd();
         ////////////////////////////////////////////////////////////
         System.out.println();
         System.out.println("----------------------------");

         
         try {
            Thread.currentThread().sleep(dt);
            }
         catch(InterruptedException e) {
            }
         t += dt;
         }
      }

   public final void displayError(String errorMessage) {
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Dimension screenSize = toolkit.getScreenSize();
      Frame dummyErrorFrame = new Frame();
      TerminationDialog errorDialog = new TerminationDialog(dummyErrorFrame, errorMessage);
      errorDialog.setLocation(screenSize.width/2 - errorDialog.getSize().width/2,
                               screenSize.height/2 - errorDialog.getSize().height/2);
      errorDialog.show();
      errorDialog.dispose();
      dummyErrorFrame.dispose();
      //Signe: write reason for termination to log file
      if(log_started)
        log.endSimulation(errorMessage);
      stop();
      }

   public final void displayEnd() {
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Dimension screenSize = toolkit.getScreenSize();
      Frame dummyErrorFrame = new Frame();
      String congratsMessage = "Congratulations, Steady state was reached.";
      TerminationDialog endDialog = new TerminationDialog(dummyErrorFrame, congratsMessage);
      endDialog.setLocation(screenSize.width/2 - endDialog.getSize().width/2,
                               screenSize.height/2 - endDialog.getSize().height/2);
      endDialog.show();
      endDialog.dispose();
      dummyErrorFrame.dispose();
      //Signe: write reason for termination to log file
      if(log_started)
        log.endSimulation(congratsMessage);
      stop();
      
      }

   public void changeUserInterface(int UIType) {
      userInterfaceType = UIType;
      int x = getSize().width;
      int y = getSize().height;

      if(userInterfaceType == PHYSICAL) {
         setupPhysicalLayout(x, y);
         layout.show(this, "physical");
         }
      else if(userInterfaceType == PHYSICALandFUNCTIONAL) {
         setupPhysicalAndFunctionalLayout(x, y);
         layout.show(this, "physicalAndFunctional");
         }
      else if(userInterfaceType == SETTINGS) {
         setupSettingsLayout(x, y);
         layout.show(this, "settings");
         }
      else if(userInterfaceType == FLOWS) {
         setupFlowsLayout(x, y);
         layout.show(this, "flows");
         }
      else if(userInterfaceType == PRINCIPLES) {
         setupPrinciplesLayout(x, y);
         layout.show(this, "principles");
         }
      else if(userInterfaceType == GOALS) {
         setupGoalsLayout(x, y);
         layout.show(this, "goals");
         }
      if(log_started)
        log.setIfType(userInterfaceType);
      }

   public void setupPhysicalLayout(int x, int y)  {
      ADD(timerCanvas, 0, 0, x*3/20, y*7/90);
      ADD(T0Name, 0, y*36/90, x/20, y*2/90);
      ADD(T0, 0, y*38/90, x/20, y*16/90);
      ADD(firstSplitterCanvas, x/20, 0, x*2/20, y);
      ADD(PA.getPumpCanvas(), x*3/20, y*9/90, x/20, y*18/90);
      ADD(PB.getPumpCanvas(), x*3/20, y*54/90, x/20, y*18/90);
      ADD(VA.getNameCanvas(), x*4/20, y*9/90, x*3/80, y*2/90);
      ADD(VA.getValveSliderCanvas(), x*4/20, y*11/90, x*3/80, y*12/90);
      ADD(VA.getValveCanvas(), x*4/20, y*23/90, x*3/80, y*4/90);
      ADD(VA.getValveLabelCanvas(), x*19/80, y*11/90, x/60, y*16/90);
      ADD(VB.getNameCanvas(), x*4/20, y*54/90, x*3/80, y*2/90);
      ADD(VB.getValveSliderCanvas(), x*4/20, y*56/90 , x*3/80, y*12/90);
      ADD(VB.getValveCanvas(), x*4/20, y*68/90, x*3/80, y*4/90);
      ADD(VB.getValveLabelCanvas(), x*19/80, y*56/90, x/60, y*16/90);
      ADD(SA.getSplitterCanvas(), x*61/240, 0, x*23/240, y*45/90);
      ADD(SB.getSplitterCanvas(), x*61/240, y*45/90, x*23/240, y*45/90);
      ADD(VA1.getNameCanvas(), x*7/20, 0, x*3/80, y*2/90);
      ADD(VA1.getValveSliderCanvas(), x*7/20, y*2/90, x*3/80, y*12/90);
      ADD(VA1.getValveCanvas(), x*7/20, y*14/90, x*3/80, y*4/90);
      ADD(VA1.getValveLabelCanvas(), x*31/80, y*2/90, x/60, y*16/90);
      ADD(VA2.getNameCanvas(), x*7/20, y*18/90, x*3/80, y*2/90);
      ADD(VA2.getValveSliderCanvas(), x*7/20, y*20/90, x*3/80, y*12/90);
      ADD(VA2.getValveCanvas(), x*7/20, y*32/90, x*3/80, y*4/90);
      ADD(VA2.getValveLabelCanvas(), x*31/80, y*20/90, x/60, y*16/90);
      ADD(VB1.getNameCanvas(), x*7/20, y*45/90, x*3/80, y*2/90);
      ADD(VB1.getValveSliderCanvas(), x*7/20, y*47/90, x*3/80, y*12/90);
      ADD(VB1.getValveCanvas(), x*7/20, y*59/90, x*3/80, y*4/90);
      ADD(VB1.getValveLabelCanvas(), x*31/80, y*47/90, x/60, y*16/90);
      ADD(VB2.getNameCanvas(), x*7/20, y*63/90, x*3/80, y*2/90);
      ADD(VB2.getValveSliderCanvas(), x*7/20, y*65/90, x*3/80, y*12/90);
      ADD(VB2.getValveCanvas(), x*7/20, y*77/90, x*3/80, y*4/90);
      ADD(VB2.getValveLabelCanvas(), x*31/80, y*65/90, x/60, y*16/90);
      ADD(mixerCanvas, x*97/240, 0, x*47/240, y);
      ADD(R1.getSimpleNameCanvas(), x*49/80, y*5/90, x*3/20, y*2/90);
      ADD(R1.getSimpleReservoirCanvas(), x*12/20, y*9/90, x*4/20, y*18/90);
      ADD(R1.getValveNameCanvas(), x*16/20, y*9/90, x*3/80, y*2/90);
      ADD(R1.getValveSliderCanvas(), x*16/20, y*11/90, x*3/80, y*12/90);
      ADD(R1.getValveCanvas(), x*16/20, y*23/90, x*3/80, y*4/90);
      ADD(R1.getValveLabelCanvas(), x*67/80, y*11/90, x/60, y*16/90);
      ADD(R1.getFloatCanvas(), x*205/240, y*11/90, x*11/240, y*16/90);
      ADD(R1.getTemperatureNameCanvas(), x*18/20, y*9/90, x/20, y*2/90);
      ADD(R1.getTempDemandCanvas(), x*18/20, y*11/90, x/20, y*16/90);
      ADD(H1.getHeaterCanvas(), x*25/40, y*27/90, x*17/160, y*2/90);
      H1.getHeaterSliderCanvas().getHeaterSlider().setPaintTicks(true);
      ADD(H1.getHeaterSliderCanvas(), x*25/40, y*29/90, x*17/160, 29);
      ADD(H1.getHeaterLabelCanvas(), x*25/40, y*29/90+29, x*17/160, y*2/90);
      ADD(R2.getSimpleNameCanvas(), x*49/80, y*50/90, x*3/20, y*2/90);
      ADD(R2.getSimpleReservoirCanvas(), x*12/20, y*54/90, x*4/20, y*18/90);
      ADD(R2.getValveNameCanvas(), x*16/20, y*54/90, x*3/80, y*2/90);
      ADD(R2.getValveSliderCanvas(), x*16/20, y*56/90, x*3/80, y*12/90);
      ADD(R2.getValveCanvas(), x*16/20, y*68/90, x*3/80, y*4/90);
      ADD(R2.getValveLabelCanvas(), x*67/80, y*56/90, x/60, y*16/90);
      ADD(R2.getFloatCanvas(), x*205/240, y*56/90, x*11/240, y*16/90);
      ADD(R2.getTemperatureNameCanvas(), x*18/20, y*54/90, x/20, y*2/90);
      ADD(R2.getTempDemandCanvas(), x*18/20, y*56/90, x/20, y*16/90);
      ADD(H2.getHeaterCanvas(), x*25/40, y*72/90, x*17/160, y*2/90);
      H2.getHeaterSliderCanvas().getHeaterSlider().setPaintTicks(true);
      ADD(H2.getHeaterSliderCanvas(), x*25/40, y*74/90, x*17/160, 29);
      ADD(H2.getHeaterLabelCanvas(), x*25/40, y*74/90+29, x*17/160, y*2/90);
      add(physicalPanel, "physical");
      }

   public void setupPhysicalAndFunctionalLayout(int x, int y) {
      ADD(timerCanvas, 0, 0, x*3/20, y*7/90);
      ADD(T0Name, 0, y*36/90, x/20, y*2/90);
      ADD(T0, 0, y*38/90, x/20, y*16/90);
      ADD(firstSplitterCanvas, x/20, 0, x/20, y);
      ADD(PA.getPumpCanvas(), x*2/20, y*9/90, x/20, y*18/90);
      ADD(PB.getPumpCanvas(), x*2/20, y*54/90, x/20, y*18/90);
      ADD(VA.getNameCanvas(), x*3/20, y*9/90, x*3/80, y*2/90);
      ADD(VA.getValveSliderCanvas(), x*3/20, y*11/90, x*3/80, y*12/90);
      ADD(VA.getValveCanvas(), x*3/20, y*23/90, x*3/80, y*4/90);
      ADD(VA.getValveLabelCanvas(), x*15/80, y*11/90, x/60, y*16/90);
      ADD(VA.getMassFlowMeterCanvas(), x*49/240, y*11/90, x/20, y*16/90);
      ADD(VB.getNameCanvas(), x*3/20, y*54/90, x*3/80, y*2/90);
      ADD(VB.getValveSliderCanvas(), x*3/20, y*56/90 , x*3/80, y*12/90);
      ADD(VB.getValveCanvas(), x*3/20, y*68/90, x*3/80, y*4/90);
      ADD(VB.getValveLabelCanvas(), x*15/80, y*56/90, x/60, y*16/90);
      ADD(VB.getMassFlowMeterCanvas(), x*49/240, y*56/90, x/20, y*16/90);
      ADD(SA.getSplitterCanvas(), x*61/240, 0, x*11/240, y*45/90);
      ADD(SB.getSplitterCanvas(), x*61/240, y*45/90, x*11/240, y*45/90);
      ADD(VA1.getNameCanvas(), x*6/20, 0, x*3/80, y*2/90);
      ADD(VA1.getValveSliderCanvas(), x*6/20, y*2/90, x*3/80, y*12/90);
      ADD(VA1.getValveCanvas(), x*6/20, y*14/90, x*3/80, y*4/90);
      ADD(VA1.getValveLabelCanvas(), x*27/80, y*2/90, x/60, y*16/90);
      ADD(VA1.getMassFlowMeterCanvas(), x*85/240, y*2/90, x/20, y*16/90);
      ADD(VA2.getNameCanvas(), x*6/20, y*18/90, x*3/80, y*2/90);
      ADD(VA2.getValveSliderCanvas(), x*6/20, y*20/90, x*3/80, y*12/90);
      ADD(VA2.getValveCanvas(), x*6/20, y*32/90, x*3/80, y*4/90);
      ADD(VA2.getValveLabelCanvas(), x*27/80, y*20/90, x/60, y*16/90);
      ADD(VA2.getMassFlowMeterCanvas(), x*85/240, y*20/90, x/20, y*16/90);
      ADD(VB1.getNameCanvas(), x*6/20, y*45/90, x*3/80, y*2/90);
      ADD(VB1.getValveSliderCanvas(), x*6/20, y*47/90, x*3/80, y*12/90);
      ADD(VB1.getValveCanvas(), x*6/20, y*59/90, x*3/80, y*4/90);
      ADD(VB1.getValveLabelCanvas(), x*27/80, y*47/90, x/60, y*16/90);
      ADD(VB1.getMassFlowMeterCanvas(), x*85/240, y*47/90, x/20, y*16/90);
      ADD(VB2.getNameCanvas(), x*6/20, y*63/90, x*3/80, y*2/90);
      ADD(VB2.getValveSliderCanvas(), x*6/20, y*65/90, x*3/80, y*12/90);
      ADD(VB2.getValveCanvas(), x*6/20, y*77/90, x*3/80, y*4/90);
      ADD(VB2.getValveLabelCanvas(), x*27/80, y*65/90, x/60, y*16/90);
      ADD(VB2.getMassFlowMeterCanvas(), x*85/240, y*65/90, x/20, y*16/90);
      ADD(PFmixerCanvas, x*97/240, 0, x*23/240, y);
      ADD(H1.getHeaterCanvas(), x*27/40, 0, x*17/160, y*2/90);
      H1.getHeaterSliderCanvas().getHeaterSlider().setPaintTicks(false);
      ADD(H1.getHeaterSliderCanvas(), x*27/40, y*2/90, x*17/160, 18);
      ADD(H1.getHeaterMeterCanvas(), x*27/40, y*2/90+19, x*17/160, y*4/90);
      ADD(H1.getHeaterLabelCanvas(), x*27/40, y*6/90+19, x*17/160, y*2/90);
      ADD(R1.getHeaterMapCanvas(), x*125/160, 0, x*2/20, y*10/90);
      ADD(R1.getComplexReservoirCanvas(), x*10/20, y*9/90+19, x*9/20, y*25/90);
      ADD(R1.getValveCanvas(), x*715/1280, y*34/90+19, x*3/80, y*4/90);
      ADD(R1.getValveNameCanvas(), x*42/80, y*36/90+19, x*3/80, y*2/90);
      ADD(R1.getReservoirSliderCanvas(), x*42/80, y*38/90+19, x*17/160, 29);
      ADD(R1.getHorizonLabelCanvas(), x*42/80, y*38/90+19+29, x*17/160, y*2/90);
      ADD(H2.getHeaterCanvas(), x*27/40, y*40/90, x*2/20, y*2/90);
      H2.getHeaterSliderCanvas().getHeaterSlider().setPaintTicks(false);
      ADD(H2.getHeaterSliderCanvas(), x*27/40, y*42/90, x*17/160, 18);
      ADD(H2.getHeaterMeterCanvas(), x*27/40, y*42/90+19, x*17/160, y*4/90);
      ADD(H2.getHeaterLabelCanvas(), x*27/40, y*46/90+19, x*17/160, y*2/90);
      ADD(R2.getHeaterMapCanvas(), x*125/160, y*40/90, x*2/20, y*10/90);
      ADD(R2.getComplexReservoirCanvas(), x*10/20, y*49/90+19, x*9/20, y*25/90);
      ADD(R2.getValveCanvas(), x*715/1280, y*74/90+19, x*3/80, y*4/90);
      ADD(R2.getValveNameCanvas(), x*42/80, y*76/90+19, x*3/80, y*2/90);
      ADD(R2.getReservoirSliderCanvas(), x*42/80, y*78/90+19, x*17/160, 29);
      ADD(R2.getHorizonLabelCanvas(), x*42/80, y*78/90+19+29, x*17/160, y*2/90);

      add(physicalAndFunctionalPanel, "physicalAndFunctional");
      }

   public void setupSettingsLayout(int x, int y) {  
      ADD(timerCanvas, 0, 0, x*3/20, y*7/90);
      ADD(T0Name, 0, y*36/90, x/20, y*2/90);
      ADD(T0, 0, y*38/90, x/20, y*16/90);
      ADD(firstSplitterCanvas, x/20, 0, x*2/20, y);
      ADD(PA.getPumpCanvas(), x*3/20, y*9/90, x/20, y*18/90);
      ADD(PB.getPumpCanvas(), x*3/20, y*54/90, x/20, y*18/90);
      ADD(VA.getNameCanvas(), x*4/20, y*9/90, x*3/80, y*2/90);
      ADD(VA.getValveSliderCanvas(), x*4/20, y*11/90, x*3/80, y*12/90);
      ADD(VA.getValveCanvas(), x*4/20, y*23/90, x*3/80, y*4/90);
      ADD(VA.getValveLabelCanvas(), x*19/80, y*11/90, x/60, y*16/90);
      ADD(VB.getNameCanvas(), x*4/20, y*54/90, x*3/80, y*2/90);
      ADD(VB.getValveSliderCanvas(), x*4/20, y*56/90 , x*3/80, y*12/90);
      ADD(VB.getValveCanvas(), x*4/20, y*68/90, x*3/80, y*4/90);
      ADD(VB.getValveLabelCanvas(), x*19/80, y*56/90, x/60, y*16/90);
      ADD(SA.getSplitterCanvas(), x*61/240, 0, x*23/240, y*45/90);
      ADD(SB.getSplitterCanvas(), x*61/240, y*45/90, x*23/240, y*45/90);
      ADD(VA1.getNameCanvas(), x*7/20, 0, x*3/80, y*2/90);
      ADD(VA1.getValveSliderCanvas(), x*7/20, y*2/90, x*3/80, y*12/90);
      ADD(VA1.getValveCanvas(), x*7/20, y*14/90, x*3/80, y*4/90);
      ADD(VA1.getValveLabelCanvas(), x*31/80, y*2/90, x/60, y*16/90);
      ADD(VA2.getNameCanvas(), x*7/20, y*18/90, x*3/80, y*2/90);
      ADD(VA2.getValveSliderCanvas(), x*7/20, y*20/90, x*3/80, y*12/90);
      ADD(VA2.getValveCanvas(), x*7/20, y*32/90, x*3/80, y*4/90);
      ADD(VA2.getValveLabelCanvas(), x*31/80, y*20/90, x/60, y*16/90);
      ADD(VB1.getNameCanvas(), x*7/20, y*45/90, x*3/80, y*2/90);
      ADD(VB1.getValveSliderCanvas(), x*7/20, y*47/90, x*3/80, y*12/90);
      ADD(VB1.getValveCanvas(), x*7/20, y*59/90, x*3/80, y*4/90);
      ADD(VB1.getValveLabelCanvas(), x*31/80, y*47/90, x/60, y*16/90);
      ADD(VB2.getNameCanvas(), x*7/20, y*63/90, x*3/80, y*2/90);
      ADD(VB2.getValveSliderCanvas(), x*7/20, y*65/90, x*3/80, y*12/90);
      ADD(VB2.getValveCanvas(), x*7/20, y*77/90, x*3/80, y*4/90);
      ADD(VB2.getValveLabelCanvas(), x*31/80, y*65/90, x/60, y*16/90);
      ADD(mixerCanvas, x*97/240, 0, x*47/240, y);
      ADD(R1.getNameCanvas(), x*51/80, y*5/90, x*4/20, y*2/90);
      ADD(R1.getSimpleReservoirCanvas(), x*12/20, y*9/90, x*4/20, y*18/90);
      ADD(R1.getValveNameCanvas(), x*16/20, y*9/90, x*3/80, y*2/90);
      ADD(R1.getValveSliderCanvas(), x*16/20, y*11/90, x*3/80, y*12/90);
      ADD(R1.getValveCanvas(), x*16/20, y*23/90, x*3/80, y*4/90);
      ADD(R1.getValveLabelCanvas(), x*67/80, y*11/90, x/60, y*16/90);
      ADD(R1.getFlowCanvas(), x*205/240, y*11/90, x*11/240, y*16/90);
      ADD(R1.getTemperatureNameCanvas(), x*18/20, y*9/90, x/20, y*2/90);
      ADD(R1.getDemandTemperatureMeterCanvas(), x*18/20, y*11/90, x/20, y*16/90);
      ADD(H1.getHeaterCanvas(), x*25/40, y*27/90, x*17/160, y*2/90);
      H1.getHeaterSliderCanvas().getHeaterSlider().setPaintTicks(true);
      ADD(H1.getHeaterSliderCanvas(), x*25/40, y*29/90, x*17/160, 29);
      ADD(H1.getHeaterLabelCanvas(), x*25/40, y*29/90+29, x*17/160, y*2/90);
      ADD(R2.getNameCanvas(), x*51/80, y*50/90, x*4/20, y*2/90);
      ADD(R2.getSimpleReservoirCanvas(), x*12/20, y*54/90, x*4/20, y*18/90);
      ADD(R2.getValveNameCanvas(), x*16/20, y*54/90, x*3/80, y*2/90);
      ADD(R2.getValveSliderCanvas(), x*16/20, y*56/90, x*3/80, y*12/90);
      ADD(R2.getValveCanvas(), x*16/20, y*68/90, x*3/80, y*4/90);
      ADD(R2.getValveLabelCanvas(), x*67/80, y*56/90, x/60, y*16/90);
      ADD(R2.getFlowCanvas(), x*205/240, y*56/90, x*11/240, y*16/90);
      ADD(R2.getTemperatureNameCanvas(), x*18/20, y*54/90, x/20, y*2/90);
      ADD(R2.getDemandTemperatureMeterCanvas(), x*18/20, y*56/90, x/20, y*16/90);
      ADD(H2.getHeaterCanvas(), x*25/40, y*72/90, x*17/160, y*2/90);
      H2.getHeaterSliderCanvas().getHeaterSlider().setPaintTicks(true);
      ADD(H2.getHeaterSliderCanvas(), x*25/40, y*74/90, x*17/160, 29);
      ADD(H2.getHeaterLabelCanvas(), x*25/40, y*74/90+29, x*17/160, y*2/90);
      add(settingsPanel, "settings");
      }

   public void setupFlowsLayout(int x, int y) {
      ADD(timerCanvas, 0, 0, x*3/20, y*7/90);
      ADD(firstSplitterCanvas, 0, 0, x*3/20, y);
      ADD(VA.getFlowCanvas(), x*3/20, y*11/90, x/20, y*16/90);
      ADD(VA.getNameCanvas(), x*4/20, y*9/90, x*3/80, y*2/90);
      ADD(VA.getMassFlowMeterCanvas(), x*4/20, y*11/90, x/20, y*16/90);
      ADD(VB.getFlowCanvas(), x*3/20, y*56/90, x/20, y*16/90);
      ADD(VB.getNameCanvas(), x*4/20, y*54/90, x*3/80, y*2/90);
      ADD(VB.getMassFlowMeterCanvas(), x*4/20, y*56/90, x/20, y*16/90);
      ADD(SA.getSplitterCanvas(), x*5/20, 0, x*2/20, y*45/90);
      ADD(SB.getSplitterCanvas(), x*5/20, y*45/90, x*2/20, y*45/90);
      ADD(VA1.getNameCanvas(), x*7/20, 0, x*3/80, y*2/90);
      ADD(VA1.getMassFlowMeterCanvas(), x*7/20, y*2/90, x/20, y*16/90);
      ADD(VA2.getNameCanvas(), x*7/20, y*18/90, x*3/80, y*2/90);
      ADD(VA2.getMassFlowMeterCanvas(), x*7/20, y*20/90, x/20, y*16/90);
      ADD(VB1.getNameCanvas(), x*7/20, y*45/90, x*3/80, y*2/90);
      ADD(VB1.getMassFlowMeterCanvas(), x*7/20, y*47/90, x/20, y*16/90);
      ADD(VB2.getNameCanvas(), x*7/20, y*63/90, x*3/80, y*2/90);
      ADD(VB2.getMassFlowMeterCanvas(), x*7/20, y*65/90, x/20, y*16/90);
      ADD(mixerCanvas, x*8/20, 0, x*4/20, y);
      ADD(R1.getNameCanvas(), x*51/80, y*5/90, x*4/20, y*2/90);
      ADD(R1.getFlowReservoirCanvas(), x*12/20, y*9/90, x*4/20, y*18/90);
      ADD(R1.getFlowNameCanvas(), x*16/20, y*9/90, x/20, y*2/90);
      ADD(R1.getMassFlowMeterCanvas(), x*16/20, y*11/90, x/20, y*16/90);
      ADD(R1.getFlowCanvas(), x*17/20, y*9/90, x*2/20, y*18/90);
      ADD(H1.getHeaterMeterCanvas(), x*25/40, y*27/90, x*17/160, y*4/90);
      ADD(H1.getHeaterLabelCanvas(), x*25/40, y*31/90, x*17/160, y*2/90);
      ADD(R2.getNameCanvas(), x*51/80, y*50/90, x*4/20, y*2/90);
      ADD(R2.getFlowReservoirCanvas(), x*12/20, y*54/90, x*4/20, y*18/90);
      ADD(R2.getFlowNameCanvas(), x*16/20, y*54/90, x/20, y*2/90);
      ADD(R2.getMassFlowMeterCanvas(), x*16/20, y*56/90, x/20, y*16/90);
      ADD(R2.getFlowCanvas(), x*17/20, y*56/90, x*2/20, y*16/90);
      ADD(H2.getHeaterMeterCanvas(), x*25/40, y*72/90, x*17/160, y*4/90);
      ADD(H2.getHeaterLabelCanvas(), x*25/40, y*76/90, x*17/160, y*2/90);
      add(flowsPanel, "flows");
      }

   public void setupPrinciplesLayout(int x, int y) {
      ADD(timerCanvas, 0, 0, x*3/20, y*7/90);
      ADD(R1.getPrinciplesCanvas(), x*10/20, y*9/90+19, x*9/20, y*25/90);
      ADD(R2.getPrinciplesCanvas(), x*10/20, y*49/90+19, x*9/20, y*25/90);
      add(principlesPanel, "principles");
      }

   public void setupGoalsLayout(int x, int y) {
      ADD(timerCanvas, 0, 0, x*3/20, y*7/90);
      ADD(R1.getNameCanvas(), x*15/20, y*9/90, x*3/20, y*2/90);
      ADD(R1.getDemandNameCanvas(), x*14/20, y*13/90, x/20, y*2/90);
      ADD(R1.getDemandCanvas(), x*14/20, y*15/90, x/20, y*18/90);
      ADD(R1.getTemperatureNameCanvas(), x*17/20, y*13/90, x/20, y*2/90);
      ADD(R1.getTempDemandCanvas(), x*17/20, y*15/90, x/20, y*18/90);

      ADD(R2.getNameCanvas(), x*15/20, y*39/90, x*3/20, y*2/90);
      ADD(R2.getDemandNameCanvas(), x*14/20, y*43/90, x/20, y*2/90);
      ADD(R2.getDemandCanvas(), x*14/20, y*45/90, x/20, y*18/90);
      ADD(R2.getTemperatureNameCanvas(), x*17/20, y*43/90, x/20, y*2/90);
      ADD(R2.getTempDemandCanvas(), x*17/20, y*45/90, x/20, y*18/90);
      add(goalsPanel, "goals");
      }

   public void ADD(Component c, int x, int y, int w, int h) {
      if(userInterfaceType == PHYSICAL)
         physicalPanel.add(c);
      else if(userInterfaceType == PHYSICALandFUNCTIONAL)
         physicalAndFunctionalPanel.add(c);
      else if(userInterfaceType == SETTINGS)
         settingsPanel.add(c);
      else if(userInterfaceType == FLOWS)
         flowsPanel.add(c);
      else if(userInterfaceType == PRINCIPLES)
         principlesPanel.add(c);
      else if(userInterfaceType == GOALS)
         goalsPanel.add(c);
      c.setBounds(x, y, w, h);
      }

   
   public final void openParameterDialog()
   {
       Toolkit toolkit = Toolkit.getDefaultToolkit();
       Dimension screenSize = toolkit.getScreenSize();
       Frame dummyParamFrame = new Frame();
       ParamDialog paramDialog = new ParamDialog(this, dummyParamFrame);
       paramDialog.setLocation(screenSize.width/2 - paramDialog.getSize().width/2,
                               screenSize.height/2 - paramDialog.getSize().height/2);
       paramDialog.show();
       paramDialog.dispose();
       dummyParamFrame.dispose();
       //new ParamDialog(this);
   }
   
   
   }


final class Score {

   private double[][] matrix;

   public Score() {
      matrix = new double[3][3];
      for(int i=0; i<3; i++)
         for(int j=0; j<3; j++)
            matrix[i][j] = 0;
      }

   public final void setScore(int row, int column, double value) {
      matrix[row][column] += value;
      }

 }

//Signe: added class to collect parameters for log file
final class ParamDialog extends Dialog {

    private TextField name;
    private TextField trial;
    private Simulator simulator;
    
    public ParamDialog(Simulator SIMULATOR, Frame parent) {

        super(parent, "Trial Info", true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                setVisible(false);
            }
        });

        simulator = SIMULATOR;
        
        final JPanel p1 = new JPanel();
        GridLayout layout = new GridLayout(2,2, 10, 10);
        p1.setLayout(layout);
        p1.add(new Label("Name"), "1");
        name = new TextField(20);
        p1.add(name, "2");
        p1.add(new Label("Trial #"), "3");
        trial = new TextField(20);
        p1.add(trial, "4");
        p1.setBorder(BorderFactory.createEmptyBorder(10,10, 10, 10));
        add(p1, "Center");
        final Panel p2 = new Panel();
        Button ok = new Button("Ok");
        final JPanel p3 = new JPanel();
        final JPanel p4 = new JPanel();
        p3.add(new Label("Log file exists for those parameters."));
        Button owrite = new Button("Overwrite");
        Button reenter = new Button("Re-Enter");

        owrite.addActionListener(new ActionListener(){
             public void actionPerformed(ActionEvent event) {
                  simulator.startLog(getName(), getTrial());
                  setVisible(false);
             }
        });
        reenter.addActionListener(new ActionListener(){
              public void actionPerformed(ActionEvent event) {
                    name.setText("");
                    trial.setText("");
                    remove(p3);
                    remove(p4);
                    add(p1, "Center");
                    add(p2, "South");
                    setTitle("Trial Info");
                    setSize(500,150);
             }
        });
        p4.add(owrite);
        p4.add(reenter);
        ok.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {

                String filename = ".\\log_files\\log_"+getName()+"_"+getTrial()+".txt";
                File file = new File(filename);
                //if file already exists for those parameters, force entry of new parameters
                if(!file.exists())
                {
                    simulator.startLog(getName(), getTrial());
                    setVisible(false);
                }
                else
                {
                    //If a log file already exists for the given parameters, this allows the user
                    //to choose whether they would like to overwrite the existing file or enter new
                    //parameters
                    setVisible(false);
                    remove(p1);
                    remove(p2);
                    add(p3, "Center");
                    add(p4, "South");
                    setTitle("WARNING");
                    setSize(300,150);
                    setVisible(true);
                    
                }
            }
        });

        p2.add(ok);
        add(p2, "South");
        setSize(500, 150);
    }

    public String getName(){

        return name.getText();
    }

    public String getTrial(){
        return trial.getText();
    }

    public void writeNameTrial(String name, String trial) {

        simulator.log.writeNameTrial(name, trial);
    }
}

//Signe: changed error dialog to termination dialog
final class TerminationDialog extends Dialog {

   public TerminationDialog(Frame parent, String errorMessage) {
      super(parent, "Simulation Terminated", true);
      addWindowListener(new WindowAdapter() {
                           public void windowClosing(WindowEvent event) {
                              setVisible(false);
                              }
                           });
      Panel p1 = new Panel();
      p1.add(new Label(errorMessage));
      add(p1, "Center");
      Panel p2 = new Panel();
         Button ok = new Button("Ok");
         ok.addActionListener(new ActionListener() {
                                 public void actionPerformed(ActionEvent event) {
                                    setVisible(false);
                                    }
                                 });
         p2.add(ok);
      add(p2, "South");
      setSize(300, 150);
      }

   }

