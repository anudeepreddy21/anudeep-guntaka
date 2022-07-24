// Reservoir.class   This class defines all UI components,
//                   drawing routines and thermodynamic equations
//                   for the 'reservoir' component used throughout the
//                   DuressJ interface.
//
// Developed for:  University Of Toronto
//                 Faculty of Applied Science and Engineering
//                 Department of Mechanical and Industrial Engineering
//                 Cognitive Engineering Laboratory
//
//    Written by:  Bruno Cosentino
//                 &  Alex S. Ross
//
//   Last Update:  March 31, 1999
//
//    Modified by: Signe Bray
//		  Advanced Interface Design Lab
//		  University of Waterloo
//
//    Date: April, 2003
//
//    Modifications:
//	1 - changed ValveLabelCanvas member to the inheriting ValbeSpecialLabelCanvas
//	    in order to move the demand bar closer to the meter
//	2 - added writing to a log when settings are changes
//	3 - adjusted scale of sliders to add precision, they still appear
//	    to be 0-10 although in actuality they are 0-100
//
//

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public final class Reservoir extends Valve {

   public static final int ERROR_BOIL = 1;
   public static final int ERROR_OVERHEAT = 2;
   public static final int ERROR_OVERFLOW = 3;
   private int error;
   private double maximumMassFlowIn;
   private double waterLevel;
   private double minimumWaterLevel;
   private double maximumWaterLevel;
   private double demandMargin;
   private double demandTemperature;
   private double demandTemperatureMargin;
   private double maximumTemperature;
   private double minimumEnergyIn;
   private double maximumEnergyIn;
   private double energy;
   private double maximumEnergy;
   private double maximumEnergyOut;
   private double tankArea;
   private double waterDensity;
   private double waterHeatCapacity;
   private double waterBoilingTemperature;
   private double reservoirFaultMassFlow;
   private double reservoirFaultTemperature;
   private int reservoirFaultTime;
   private int breakTime;
   private double massFlowIn; // not specified in config. file (during simulation)
   private double temperatureIn; //                       "
   private double energyIn;      //                       "
   private double heaterEnergyIn;  //                     "
   private double hiddenHeaterEnergyIn; //                "
   private double temperature;   //                       "
   private double energyOut;           //                 "
   private int reservoirFaultTimeLeft; //                 "

   private Demand demand;
   private SimpleReservoirCanvas simpleReservoirCanvas;
   private FlowReservoirCanvas flowReservoirCanvas;
   private HeaterMapCanvas heaterMapCanvas;
   private ComplexReservoirCanvas complexReservoirCanvas;
   private ReservoirSliderCanvas reservoirSliderCanvas;
   private JSlider reservoirSlider;
   private PrinciplesCanvas principlesCanvas;
   private TemperatureMeterCanvas temperatureMeterCanvas;
   private SimpleNameCanvas simpleNameCanvas;
   private ValveNameCanvas valveNameCanvas;
   private FlowNameCanvas flowNameCanvas;
   private DemandNameCanvas demandNameCanvas;
   private TemperatureNameCanvas temperatureNameCanvas;
   private HorizonLabelCanvas horizonLabelCanvas;
   private DemandCanvas demandCanvas;
   private TempDemandCanvas tempDemandCanvas;
   private FloatCanvas floatCanvas;

   public Reservoir(String name, double maxFlowIn, double maxFlowOut, double initLevel,
                    double minLevel, double maxLevel,
                    double demMarg, double demTemp, double tempMarg, double maxTemp,
                    double minEnergyIn, double maxEnergyIn, double maxEnergyOut, double initEnergy,
                    double maxEnergy, double area, double density, double heatCap, double boilTemp,
                    double addFlow, double addTemp, int addTime, int timeLeft,
                    double initSet, double initFlow, double vtC,
                    double fsp1, int ft1, double fsp2, int ft2,
                    String demName, double maxDem, double initDemSet, double initDemOpen,
                    double dtC, double df1sp, int df1t, double df2sp, int df2t) {
      // outflow valve
      super(name, maxFlowOut, initSet, initFlow, vtC, fsp1, ft1, fsp2, ft2);
      maximumMassFlowIn = maxFlowIn;
      waterLevel = initLevel;
      minimumWaterLevel = minLevel;
      maximumWaterLevel = maxLevel;
      demandMargin = demMarg;
      demandTemperature = demTemp;
      demandTemperatureMargin = tempMarg;
      maximumTemperature = maxTemp;
      minimumEnergyIn = minEnergyIn;
      maximumEnergyIn = maxEnergyIn;
      maximumEnergyOut = maxEnergyOut;
      energy = initEnergy;
      maximumEnergy = maxEnergy;
      tankArea = area;
      waterDensity = density;
      waterHeatCapacity = heatCap;
      waterBoilingTemperature = boilTemp;
      reservoirFaultMassFlow = addFlow;
      reservoirFaultTemperature = addTemp;
      reservoirFaultTime = addTime;
      breakTime = timeLeft;
      // demand (modeled as a valve)
      demand = new Demand(demName, maxDem, initDemSet, initDemOpen,
                          dtC, df1sp, df1t, df2sp, df2t);
      simpleReservoirCanvas = new SimpleReservoirCanvas(waterLevel, maximumWaterLevel);
      flowReservoirCanvas = new FlowReservoirCanvas(waterLevel, maximumWaterLevel);
      heaterMapCanvas = new HeaterMapCanvas();
      complexReservoirCanvas = new
         ComplexReservoirCanvas(massFlowIn, maximumMassFlowIn, waterLevel*tankArea, maxLevel*tankArea,
                                getMassFlowOut(), getMaximumMassFlowOut(), demand.getFlow(), demandMargin,
                                temperature, maximumTemperature, demandTemperature, demandTemperatureMargin,
                                energyIn, heaterEnergyIn+hiddenHeaterEnergyIn, maximumEnergyIn,
                                energy, maximumEnergy, energyOut, maximumEnergyOut,
                                waterHeatCapacity, waterDensity, getName());
      reservoirSliderCanvas = new ReservoirSliderCanvas(this, (int)getMaximumMassFlowOut());
      reservoirSliderCanvas.setReservoirSlider((int)getValveSetting());
      principlesCanvas = new
         PrinciplesCanvas(massFlowIn, maximumMassFlowIn, waterLevel*tankArea, maxLevel*tankArea,
                                getMassFlowOut(), getMaximumMassFlowOut(),
                                energyIn, heaterEnergyIn+hiddenHeaterEnergyIn, maximumEnergyIn,
                                energy, maximumEnergy, energyOut, maximumEnergyOut, getName());
      temperatureMeterCanvas = new TemperatureMeterCanvas(getTemperatureOut(),
                                                          maximumTemperature);
      simpleNameCanvas = new SimpleNameCanvas(getName() + " (" + (int)demandTemperature + " C)");
      if(getName() == "Reservoir 1") {
         valveNameCanvas = new ValveNameCanvas("VO1");
         flowNameCanvas = new FlowNameCanvas("FO1");
         demandNameCanvas = new DemandNameCanvas("D1");
         temperatureNameCanvas = new TemperatureNameCanvas("T1");
         }
      else {
         valveNameCanvas = new ValveNameCanvas("VO2");
         flowNameCanvas = new FlowNameCanvas("FO2");
         demandNameCanvas = new DemandNameCanvas("D2");
         temperatureNameCanvas = new TemperatureNameCanvas("T2");
         }

      horizonLabelCanvas = new HorizonLabelCanvas((int)getMaximumMassFlowOut());
      demandCanvas = new DemandCanvas(getMassFlowOut(), maxFlowOut, demand.getFlow(), demandMargin);
      tempDemandCanvas = new TempDemandCanvas(getTemperatureOut(), maximumTemperature, demandTemperature, demandTemperatureMargin);
      floatCanvas = new FloatCanvas(getMassFlowOut(), getMaximumMassFlowOut(), demand.getFlow(), demandMargin);
      //Signe: changed to special label canvas, to move demand lines closer to meter
      valveLabelCanvas = new ValveSpecialLabelCanvas(getMaximumMassFlowOut(), demand.getFlow(), demandMargin);
    }

   public final void calculateDemand(int t, int dt) {
      demand.calculateDemand(t, dt);
      // update demand on meter
      // setMeterGoal(demand.getFlow());
      }

   public final void calculateReservoir(int t, int dt) {
      double addInFlow, addOutFlow, addInEnergy, addOutEnergy, tmp, energyDerivative;

      // check for leaks or extra inflow (configuration fault)
      if(reservoirFaultTime != Simulator.NEVER && reservoirFaultTime <= t)
         if(reservoirFaultMassFlow > 0) {
            addInFlow = reservoirFaultMassFlow;
            addOutFlow = 0;
            addInEnergy = addInFlow*waterHeatCapacity*reservoirFaultTemperature;
            addOutEnergy = 0;
            }
         else {
            addInFlow = 0;
            addOutFlow = -reservoirFaultMassFlow;
            addInEnergy = 0;
            addOutEnergy = addOutFlow*waterHeatCapacity*temperature;
            }
      else {
         addInFlow = addOutFlow = 0;
         addInEnergy = addOutEnergy = 0;
         }

      // adjust massflow and volume
      if(waterLevel == 0 && massFlowIn + addInFlow < getMassFlowOut() + addOutFlow) {
         setMassFlowOut(massFlowIn+addInFlow-addOutFlow);
         if(getMassFlowOut() < 0)
            setMassFlowOut(0);
         }
      // add water level derivative
      if(waterLevel >= 0 && waterLevel <= maximumWaterLevel)
         // Note: no time constant in this formaula, therfore a scaling factor was used
         // since time (dt) is measured in 1000's of seconds
         waterLevel += (massFlowIn+addInFlow-getMassFlowOut()-addOutFlow)*dt/1000/tankArea/waterDensity;
      // check for overflow error
      if(waterLevel > maximumWaterLevel)
         setError(ERROR_OVERFLOW);
      // calculate water level
      waterLevel = max(0, min(waterLevel, maximumWaterLevel));
      // update simple reservoir display (physical & settings)
      simpleReservoirCanvas.setWaterLevel(waterLevel);
      //Signe: update log file
      if(Simulator.log_started)
        Simulator.log.updateWaterLevel(waterLevel, super.getName());
      // update flow reservoir display
      flowReservoirCanvas.setWaterLevel(waterLevel);

      // energy calculations
      energyIn = massFlowIn*waterHeatCapacity*temperatureIn;
      energyOut = getMassFlowOut()*waterHeatCapacity*temperature;
      // add energy derivative
      if(energy >=0 && energy <= maximumEnergy)
         // (note: no time constant in this formaula, therfore a scaling factor was used
         // since time (dt) is in 1000's of seconds)
         energyDerivative = (energyIn+heaterEnergyIn+hiddenHeaterEnergyIn-energyOut+addInEnergy-addOutEnergy)*dt/1000;
      else
         energyDerivative = 0;
      energy += energyDerivative;
      if(energy < 0)
         energy = 0;
      if(waterLevel == 0)
         energy = 0;

      // temperature calculations
      tmp = waterLevel*tankArea*waterDensity*waterHeatCapacity;
      // make sure denominator not equal to zero
      if(tmp != 0)
         temperature = energy/tmp;
      else
         temperature = temperatureIn;
      // check for boiling error
      if(temperature > waterBoilingTemperature && waterLevel > minimumWaterLevel)
         setError(ERROR_BOIL);
      // stop from freezing
      if(temperature < 0)
         temperature = 0;
      setTemperatureOut(temperature);       //Note:  last minute fix  (be my guest)
      temperatureMeterCanvas.setTemperatureSetting(temperature);

      // check for overheat error
      if(heaterEnergyIn > minimumEnergyIn && waterLevel < minimumWaterLevel)
         if(reservoirFaultTimeLeft == Simulator.NEVER)
            reservoirFaultTimeLeft = breakTime;
         else if(reservoirFaultTimeLeft <= 0)
            setError(ERROR_OVERHEAT);
         else
            reservoirFaultTimeLeft -= dt;

      // update complex reservoir display
      complexReservoirCanvas.setSettings(massFlowIn, waterLevel*tankArea, getMassFlowOut(),
                                         demand.getFlow(), temperature,
                                         energyIn, heaterEnergyIn+hiddenHeaterEnergyIn,
                                         energy, energyOut);
      // update principles display
      principlesCanvas.setSettings(massFlowIn, waterLevel*tankArea, getMassFlowOut(),
                                         energyIn, heaterEnergyIn+hiddenHeaterEnergyIn,
                                         energy, energyOut);
      // update demand displays
      demandCanvas.setMeter(getMassFlowOut(), demand.getFlow());
      tempDemandCanvas.setMeter(temperature);

      }

   public final void setSlider(double newValue) {
      super.setSlider(newValue);
      reservoirSliderCanvas.setReservoirSlider((int)(newValue*10));
      }

   public final void setError(int errorType) {
      if(error == 0)
         error=errorType;
      }

   public final int getError() {
      return error;
      }

   public final void setMassFlowIn(double massIn) {
      massFlowIn = massIn;
      }

   public final void setTemperatureIn(double tempIn) {
      if(massFlowIn > 0.00005)
         temperatureIn = tempIn;
      else
         temperatureIn = 0;
      }

   public final void setHeaterEnergyIn(double heaterIn) {
      heaterEnergyIn = heaterIn;
      }

   public final void setHiddenHeaterEnergyIn(double hiddenHeaterIn) {
      hiddenHeaterEnergyIn = hiddenHeaterIn;
      }

   public final double getDemand() {
      return demand.getFlow();
      }

   public final double getDemandTemperature() {
      return demandTemperature;
      }

   public final SimpleReservoirCanvas getSimpleReservoirCanvas() {
      return simpleReservoirCanvas;
      }

   public final FlowReservoirCanvas getFlowReservoirCanvas() {
      return flowReservoirCanvas;
      }

   public final HeaterMapCanvas getHeaterMapCanvas() {
      return heaterMapCanvas;
      }

   public final ComplexReservoirCanvas getComplexReservoirCanvas() {
      return complexReservoirCanvas;
      }

   public final ReservoirSliderCanvas getReservoirSliderCanvas() {
      return reservoirSliderCanvas;
      }

   public final PrinciplesCanvas getPrinciplesCanvas() {
      return principlesCanvas;
      }

   public final TemperatureMeterCanvas getDemandTemperatureMeterCanvas() {
      return temperatureMeterCanvas;
      }

   public final SimpleNameCanvas getSimpleNameCanvas() {
      return simpleNameCanvas;
      }

   public final ValveNameCanvas getValveNameCanvas() {
      return valveNameCanvas;
      }

   public final FlowNameCanvas getFlowNameCanvas() {
      return flowNameCanvas;
      }

   public final DemandNameCanvas getDemandNameCanvas() {
      return demandNameCanvas;
      }

   public final TemperatureNameCanvas getTemperatureNameCanvas() {
      return temperatureNameCanvas;
      }

   public final HorizonLabelCanvas getHorizonLabelCanvas() {
      return horizonLabelCanvas;
      }

   public final DemandCanvas getDemandCanvas() {
      return demandCanvas;
      }

   public final TempDemandCanvas getTempDemandCanvas() {
      return tempDemandCanvas;
      }

   public final FloatCanvas getFloatCanvas() {
      return floatCanvas;
      }

   }


final class DemandCanvas extends Canvas {

   private double setting;
   private double maximum;
   private double demand;
   private double margin;

   public DemandCanvas(double initSetting, double max, double initDemand, double margin) {
      setting = initSetting;
      maximum = max;
      demand = initDemand;
      this.margin = margin;
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      // draw demand marker
      g.setColor(Simulator.COLOR_GOAL);
      g.fillRect(x/7-2, y*14/18-9-(int)((demand+margin)*(y-y*4/18-18)/maximum), x*3/7+2,
                 (int)(2*margin*(y-y*4/18-18)/maximum));
      // draw normal meter
      g.setColor(Color.black);
      g.drawRect(0, 0, x*5/7, y*14/18);
      // draw meter labels
      g.setFont(new Font("SansSerif", Font.PLAIN, y/11));
      g.drawString("" + (int)maximum, x*5/7+1, y*14/18-6-(y-y*4/18-18));
      g.drawString("0", x*5/7+2, y*14/18-6);
      // draw 'ticks' on the meter
      for(int i=0; i<=10; i++)
         if(i%5==0)
            g.drawLine(x*4/7, y*14/18-9-i*(y-y*4/18-18)/10, x*5/7, y*14/18-9-i*(y-y*4/18-18)/10);
         else
            g.drawLine(x*9/14, y*14/18-9-i*(y-y*4/18-18)/10, x*5/7, y*14/18-9-i*(y-y*4/18-18)/10);
      // draw 'pipe' underneath the meter
      g.fillOval(x*3/7-1, y*16/18-1, 3, 3);
      g.drawLine(x*3/7, y*14/18, x*3/7, y*16/18);
      g.drawLine(0, y*16/18, x, y*16/18);
      // draw the yellow massflow indicator bar
      if(setting != 0) {
         g.drawRect(x/7, (int)(y*14/18-9-setting*(y-y*4/18-18)/maximum)+1, x*2/7, (int)(setting*(y-y*4/18-18)/maximum)+1);
         g.setColor(Simulator.COLOR_MASSFLOW);
         g.fillRect(x/7+1, (int)(y*14/18-9-setting*(y-y*4/18-18)/maximum)+2, x*2/7-1, (int)(setting*(y-y*4/18-18)/maximum));
         }
      }

   public final void setMeter(double newSetting, double demand) {
      int x = getSize().width;
      int y = getSize().height;

      if((int)(setting*(y-y*4/18-18)/maximum) != (int)(newSetting*(y-y*4/18-18)/maximum)) {
         setting = newSetting;
         this.demand = demand;
         repaint();
         }
      }

   }


final class FloatCanvas extends Canvas {

   private double setting;
   private double maximum;
   private double demand;
   private double margin;

   public FloatCanvas(double initSetting, double max, double initDemand, double margin) {
      setting = initSetting;
      maximum = max;
      demand = initDemand;
      this.margin = margin;
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      // draw demand marker
      g.setColor(Simulator.COLOR_GOAL);
      g.fillRect(0/*x/7-2*/, y*14/18-9-(int)((demand+margin)*(y-y*4/18-18)/maximum), x/7-2/*x*3/7+2*/,
                 (int)(2*margin*(y-y*4/18-18)/maximum));
      // draw ghost meter
      g.setColor(Color.black);
      g.drawLine(0, y*16/18, x, y*16/18);
            
      }

   public final void setMeter(double newSetting, double demand) {
      int x = getSize().width;
      int y = getSize().height;

      if((int)(setting*(y-y*4/18-18)/maximum) != (int)(newSetting*(y-y*4/18-18)/maximum)) {
         setting = newSetting;
         repaint();
         }
      this.demand = demand;
      }

   }


final class TempDemandCanvas extends Canvas {

   private double setting;
   private double maximum;
   private double demand;
   private double margin;

   public TempDemandCanvas(double initSetting, double max, double initDemand, double margin) {
      setting = initSetting;
      maximum = max;
      demand = initDemand;
      this.margin = margin;
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      // draw demand marker
      g.setColor(Simulator.COLOR_GOAL);
      g.fillRect(x/7-2, y*14/18-9-(int)((demand+margin)*(y-y*4/18-18)/maximum), x*3/7+2,
                 (int)(2*margin*(y-y*4/18-18)/maximum));
      // draw normal meter
      g.setColor(Color.black);
      g.drawRect(0, 0, x*5/7, y*14/18);
      // draw meter labels
      g.setFont(new Font("SansSerif", Font.PLAIN, y/11+1));
      g.drawString("" + (int)maximum, x*5/7+1, y*14/18-6-(y-y*4/18-18));
      g.drawString("0", x*5/7+2, y*14/18-6);
      // draw 'ticks' on the meter
      for(int i=0; i<=10; i++)
         if(i%5==0)
            g.drawLine(x*4/7, y*14/18-9-i*(y-y*4/18-18)/10, x*5/7, y*14/18-9-i*(y-y*4/18-18)/10);
         else
            g.drawLine(x*9/14, y*14/18-9-i*(y-y*4/18-18)/10, x*5/7, y*14/18-9-i*(y-y*4/18-18)/10);
      // draw 'pipe' underneath the meter
      g.fillOval(x*3/7-1, y*16/18-1, 3, 3);
      g.drawLine(x*3/7, y*14/18, x*3/7, y*16/18);
      g.drawLine(0, y*16/18, x, y*16/18);
      // draw the yellow massflow indicator bar
      if(setting != 0) {
         g.drawRect(x/7, (int)(y*14/18-9-setting*(y-y*4/18-18)/maximum)+1, x*2/7, (int)(setting*(y-y*4/18-18)/maximum)+1);
         g.setColor(Simulator.COLOR_TEMPERATURE);
         g.fillRect(x/7+1, (int)(y*14/18-9-setting*(y-y*4/18-18)/maximum)+2, x*2/7-1, (int)(setting*(y-y*4/18-18)/maximum));
         }
      }

   public final void setMeter(double newSetting) {
      int x = getSize().width;
      int y = getSize().height;

      if((int)(setting*(y-y*4/18-18)/maximum) != (int)(newSetting*(y-y*4/18-18)/maximum)) {
         setting = newSetting;
         repaint();
         }
      }

   }



final class Demand {

   private String name;
   private double maximum;
   private double setting;
   private double flow;
   private double timeConstant;
   private double fault1Setpoint;
   private int fault1Time;
   private double fault2Setpoint;
   private int fault2Time;

   public Demand(String name, double max, double initSet, double initOpen,
                 double tC, double f1sp, int f1t, double f2sp, int f2t) {
      this.name = name;
      maximum = max;
      setting = initSet;
      flow = initOpen;
      timeConstant = tC;
      fault1Setpoint = f1sp;
      fault1Time = f1t;
      fault2Setpoint = f2sp;
      fault2Time = f2t;
      }

   public final void calculateDemand(int t, int dt) {
      // check to see if demand is in one of the fault modes
      if(fault2Time != Simulator.NEVER && fault2Time <= t)
         setting = fault2Setpoint;
      else if(fault1Time != Simulator.NEVER && fault1Time <= t)
         setting = fault1Setpoint;
      // calculate opening
      flow = Flow.max(0, Flow.min(flow, maximum));
      // add the derivative
      flow += (setting-flow)*dt/timeConstant;
      }

   public final double getFlow() {
      return flow;
      }

   }


final class SimpleNameCanvas extends Canvas {

   private String name;

   public SimpleNameCanvas(String name) {
      this.name = name;
      }

   public void paint(Graphics g) {
      int y = getSize().height;

      g.setFont(new Font("SansSerif", Font.PLAIN, y));
      g.drawString(name, 0, y);
      }

   }


final class SimpleReservoirCanvas extends Canvas {

   private double level;
   private double maximum;

   public SimpleReservoirCanvas(double level, double maxLevel) {
      this.level = level;
      maximum = maxLevel;
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      g.setFont(new Font("SansSerif", Font.PLAIN, y/11));
      g.drawLine(0, 0, x*2/21, 0);
      g.drawLine(x*2/21, 0, x*2/21, y*14/18);
      g.drawLine(x*2/21, y*14/18, x*14/21, y*14/18);
      g.drawLine(x*14/21, y*14/18, x*14/21, 0);
      g.drawLine(x*14/21, 0, x*16/21, 0);
      g.drawString("" + (int)(maximum*100), x*16/21+2, y*3/36-1);
      g.drawString("V", x*16/21+2, y*15/36);
      g.drawString("0", x*16/21+2, y*14/18);
      g.drawLine(x*8/21, y*14/18, x*8/21, y*16/18);
      g.drawLine(x*8/21, y*16/18, x, y*16/18);
      // draw 'ticks' on side of reservoir
      for(int i=0; i<=10; i++)
         if(i%5==0)
            g.drawLine(x*14/21, y*14/18-i*(y-y*4/18)/10, x*16/21, y*14/18-i*(y-y*4/18)/10);
         else
            g.drawLine(x*14/21, y*14/18-i*(y-y*4/18)/10, x*15/21, y*14/18-i*(y-y*4/18)/10);
      // fill the tank
      g.setColor(Simulator.COLOR_RESERVOIR);
      g.fillRect(x*2/21+1, y*14/18-(int)(level*(y-y*4/18)/maximum),
                 x*12/21-1, (int)(level*(y-y*4/18)/maximum));
      }

   public final void setWaterLevel(double newLevel) {
      int x = getSize().width;
      int y = getSize().height;

      if((int)(level*(y-y*4/18)/maximum) != (int)(newLevel*(y-y*4/18)/maximum)) {
         level = newLevel;
         repaint();
         }
      }

   }


final class FlowReservoirCanvas extends Canvas {

   private double level;
   private double maximum;

   public FlowReservoirCanvas(double level, double maxLevel) {
      this.level = level;
      maximum = maxLevel;
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      g.drawLine(0, 0, x*2/21, 0);
      g.drawLine(x*2/21, 0, x*2/21, y*14/18);
      g.drawLine(x*2/21, y*14/18, x*14/21, y*14/18);
      g.drawLine(x*14/21, y*14/18, x*14/21, 0);
      g.drawLine(x*14/21, 0, x*16/21, 0);
      g.drawLine(x*8/21, y*14/18, x*8/21, y*16/18);
      g.drawLine(x*8/21, y*16/18, x, y*16/18);
      // fill the tank
      g.setColor(Simulator.COLOR_RESERVOIR);
      g.fillRect(x*2/21+1, y*14/18-(int)(level*(y-y*4/18)/maximum),
                 x*12/21-1, (int)(level*(y-y*4/18)/maximum));
      }

   public final void setWaterLevel(double newLevel) {
      int x = getSize().width;
      int y = getSize().height;

      if((int)(level*(y-y*4/18)/maximum) != (int)(newLevel*(y-y*4/18)/maximum)) {
         level = newLevel;
         repaint();
         }
      }

   }


final class ValveNameCanvas extends Canvas {

   private String name;

   public ValveNameCanvas(String name) {
      this.name = name;
      }

   public void paint(Graphics g) {
      int y = getSize().height;

      g.setFont(new Font("SansSerif", Font.PLAIN, y));
      g.drawString(name, 0, y-2);
      }

   }


final class FlowNameCanvas extends Canvas {

   private String name;

   public FlowNameCanvas(String name) {
      this.name = name;
      }

   public void paint(Graphics g) {
      int y = getSize().height;

      g.setFont(new Font("SansSerif", Font.PLAIN, y));
      g.drawString(name, 0, y-2);
      }

   }


final class DemandNameCanvas extends Canvas {

   private String name;

   public DemandNameCanvas(String name) {
      this.name = name;
      }

   public void paint(Graphics g) {
      int y = getSize().height;

      g.setFont(new Font("SansSerif", Font.PLAIN, y));
      g.drawString(name, 0, y-2);
      }

   }


final class HeaterMapCanvas extends Canvas {

   private Polygon arrow;

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      g.drawLine(x*2/11, y/9, x*3/11, y/9);
      g.drawLine(x*2/11, y*8/9, x*3/11, y*8/9);
      g.drawLine(x*3/11, y/9, x*3/11, y*8/9);
      g.drawLine(x*3/11, y*4/9, x*9/11, y*4/9);
      g.drawLine(x*9/11, y*4/9, x*9/11, y*8/9);
      arrow = new Polygon();
      arrow.addPoint(x*17/22, y*7/9);
      arrow.addPoint(x*19/22, y*7/9);
      arrow.addPoint(x*9/11, y*8/9);
      g.fillPolygon(arrow);
      }

   }

final class ComplexReservoirCanvas extends Canvas {

   private double massFlowIn;
   private double massFlowInMaximum;
   private double volume;
   private double volumeMaximum;
   private double massFlowOut;
   private double massFlowOutMaximum;
   private double demand;
   private double demandMargin;
   private double temperature;
   private double temperatureMaximum;
   private double demandTemperature;
   private double demandTemperatureMargin;
   private double energyIn;
   private double heaterEnergy;        //  Note: this includes hidden heater energy
   private double energyInMaximum;
   private double energy;
   private double energyMaximum;
   private double energyOut;
   private double energyOutMaximum;
   private double heatCapacity;
   private double density;
   private String name;

   private Polygon arrow;

   public ComplexReservoirCanvas(double mI, double mIM, double v, double vM,double mO, double mOM,
                                 double d, double dM, double t, double tM, double dT, double dTM,
                                 double eI, double hE, double eIM, double e, double eM,
                                 double eO, double eOM, double hC, double dens, String name) {
      massFlowIn = mI;
      massFlowInMaximum = mIM;
      volume = v;
      volumeMaximum = vM;
      massFlowOut = mO;
      massFlowOutMaximum = mOM;
      demand = d;
      demandMargin = dM;
      temperature = t;
      temperatureMaximum = tM;
      demandTemperature = dT;
      demandTemperatureMargin = dTM;
      energyIn = eI;
      heaterEnergy = hE;
      energyInMaximum = eIM;
      energy = e;
      energyMaximum = eM;
      energyOut = eO;
      energyOutMaximum = eOM;
      heatCapacity = hC;
      density = dens;
      this.name = name;
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;


      g.setFont(new Font("SansSerif", Font.PLAIN, y/17));
      g.drawString(name + " (" + (int)demandTemperature + " C)", x*19/49, y*3/26-2);
      g.drawString("MASS IN", x*3/49, y*2/26-2);
      g.drawString("" + (int)volumeMaximum*100, x/98-2, y*7/26);
      g.drawString("M", x/49, y*13/26);
      g.drawString("0", x/49, y*18/26+5);
      g.drawString("MASS OUT", x*3/49, y*24/26+3);
      g.drawString("0", x*19/49, y*24/26+2);
      if(name == "Reservoir 1")
         g.drawString("T1", x*24/49, y*24/26+2);           // I have no idea
      else                                                 // why this code does that
         g.drawString("T2", x*24/49, y*24/26+2);
      g.drawString("" + (int)temperatureMaximum, x*30/49-3, y*24/26+2);
      g.drawString("ENERGY IN", x*35/49, y*2/26-2);
      g.drawString("E", x*46/49+2, y*13/26);
      g.drawString("ENERGY OUT", x*35/49, y*24/26+3);

      g.drawRect(0, 0, x-1, y-1);
      // draw demand markers
      g.setColor(Simulator.COLOR_GOAL);
      // massflow demand
      g.fillRect(x*3/49+5+(int)((demand-demandMargin)*(x*11/49-10)/massFlowOutMaximum)-1,
                 y*19/26+5,
                 (int)(2*demandMargin*(x*11/49-10)/massFlowOutMaximum)-1, y*2/26);
      // temperature demand
      g.fillRect(x*19/49+5+(int)((demandTemperature-demandTemperatureMargin)*(x*11/49-10)/temperatureMaximum),
                 y*19/26+5,
                 (int)(2*demandTemperatureMargin*(x*11/49-10)/temperatureMaximum)+1, y*2/26);
      // draw Demand Temp and energy intersection lines
      int j = 0; 
      for(int i=(int)((demandTemperature-demandTemperatureMargin)*(x*11/49-10)/temperatureMaximum);
          i<(int)((demandTemperature+demandTemperatureMargin)*(x*11/49-10)/temperatureMaximum);
          i++) {
          double energyGoal = calculateEnergyGoal(demandTemperature-demandTemperatureMargin+j);
          // vertical line
          g.drawLine(x*19/49+5+i, y*20/26,
                     x*19/49+5+i, y*19/26-5-(int)(energyGoal*(y*13/26-10)/energyMaximum));
          // horizontal line
          g.drawLine(x*19/49+5+i, y*19/26-5-(int)(energyGoal*(y*13/26-10)/energyMaximum),
                     x*43/49, y*19/26-5-(int)(energyGoal*(y*13/26-10)/energyMaximum));
          j++;
          }
      g.setColor(Color.black);
      // draw inflow arrow
      arrow = new Polygon();
      arrow.addPoint(0, y*4/52);
      arrow.addPoint(0, y*10/52);
      arrow.addPoint(x*2/49, y*7/52);
      g.fillPolygon(arrow);
      // massflow meter bounds
      g.drawRect(x*3/49, y*2/26, x*11/49, y*21/26);
      g.drawLine(x*3/49, y*6/26, x*14/49, y*6/26);
      g.drawLine(x*3/49, y*19/26, x*14/49, y*19/26);
      // temperature meter bounds
      g.drawRect(x*19/49, y*19/26, x*11/49, y*4/26);
      // energy meter bounds
      g.drawRect(x*35/49, y*2/26, x*11/49, y*21/26);
      g.drawLine(x*35/49, y*6/26, x*46/49, y*6/26);
      g.drawLine(x*35/49, y*19/26, x*46/49, y*19/26);
      // interconnecting lines
      g.drawLine(x*17/98-1, y*23/26, x*17/98-1, y*26/26);
      g.drawLine(x*49/98-1, y*23/26, x*49/98-1, y*25/26);
      g.drawLine(x*81/98-1, y*23/26, x*81/98-1, y*25/26);
      for(int i=17; i<81; i+=2)
         g.drawLine(x*i/98, y*25/26, x*(i+1)/98, y*25/26);
      g.fillOval(x*49/98-1, y*25/26-1, 3, 3);
      // draw ticks on all the meters
      for(int i=0; i<=10; i++) {
         if(i%5==0) {
            g.drawLine(x*3/49+5+i*(x*11/49-10)/10, y*2/26, x*3/49+5+i*(x*11/49-10)/10, y*7/52);
            g.drawLine(x*3/49, y*6/26+5+i*(y*13/26-10)/10, x*5/49, y*6/26+5+i*(y*13/26-10)/10);
            g.drawLine(x*3/49+5+i*(x*11/49-10)/10, y*86/104, x*3/49+5+i*(x*11/49-10)/10, y*23/26);
            g.drawLine(x*19/49+5+i*(x*11/49-10)/10, y*86/104, x*19/49+5+i*(x*11/49-10)/10, y*23/26);
            g.drawLine(x*35/49+5+i*(x*11/49-10)/10, y*2/26, x*35/49+5+i*(x*11/49-10)/10, y*7/52);
            g.drawLine(x*44/49, y*6/26+5+i*(y*13/26-10)/10, x*46/49, y*6/26+5+i*(y*13/26-10)/10);
            g.drawLine(x*35/49+5+i*(x*11/49-10)/10, y*86/104, x*35/49+5+i*(x*11/49-10)/10, y*23/26);
            }
         else {
            g.drawLine(x*3/49+5+i*(x*11/49-10)/10, y*2/26, x*3/49+5+i*(x*11/49-10)/10, y*3/26);
            g.drawLine(x*3/49, y*6/26+5+i*(y*13/26-10)/10, x*4/49, y*6/26+5+i*(y*13/26-10)/10);
            g.drawLine(x*3/49+5+i*(x*11/49-10)/10, y*22/26, x*3/49+5+i*(x*11/49-10)/10, y*23/26);
            g.drawLine(x*19/49+5+i*(x*11/49-10)/10, y*22/26, x*19/49+5+i*(x*11/49-10)/10, y*23/26);
            g.drawLine(x*35/49+5+i*(x*11/49-10)/10, y*2/26, x*35/49+5+i*(x*11/49-10)/10, y*3/26);
            g.drawLine(x*45/49, y*6/26+5+i*(y*13/26-10)/10, x*46/49, y*6/26+5+i*(y*13/26-10)/10);
            g.drawLine(x*35/49+5+i*(x*11/49-10)/10, y*22/26, x*35/49+5+i*(x*11/49-10)/10, y*23/26);
            }
         }

      // draw massflow and energy-in levels
      g.setColor(Color.black);
      g.drawRect(x*3/49+4, y*4/26-1, (int)(massFlowIn*(x*11/49-10)/massFlowInMaximum)+1, y/26+1);
      g.drawRect(x*3/49+4, y*20/26-1,
                 (int)(massFlowOut*(x*11/49-10)/massFlowOutMaximum)+1, y/26+1);
      g.drawRect(x*35/49+4, y*4/26-1, (int)(energyIn*(x*11/49-10)/energyInMaximum)+1, y/26+1);
      g.setColor(Simulator.COLOR_MASSFLOW);
      g.fillRect(x*3/49+5, y*4/26, (int)(massFlowIn*(x*11/49-10)/massFlowInMaximum), y/26);
      g.fillRect(x*3/49+5, y*20/26, (int)(massFlowOut*(x*11/49-10)/massFlowOutMaximum), y/26);
      g.fillRect(x*35/49+5, y*4/26, (int)(energyIn*(x*11/49-10)/energyInMaximum), y/26);
      // draw water level
      g.setColor(Color.black);
      g.drawRect(x*6/49-1, y*19/26-6-(int)(volume*(y*13/26-10)/volumeMaximum),
                 x*7/49+1, (int)(volume*(y*13/26-10)/volumeMaximum)+1);
      g.setColor(Simulator.COLOR_RESERVOIR);
      g.fillRect(x*6/49, y*19/26-5-(int)(volume*(y*13/26-10)/volumeMaximum),
                 x*7/49, (int)(volume*(y*13/26-10)/volumeMaximum));
      // draw temperature, heater-energy-in, and energy-out levels
      g.setColor(Color.black);
      if(temperature < temperatureMaximum)
         g.drawRect(x*19/49+4, y*20/26-1,
                    (int)(temperature*(x*11/49-10)/temperatureMaximum)+1, y/26+1);
      else
         g.drawRect(x*19/49+4, y*20/26-1, x*11/49-10+1, y/26+1);
      if(energyIn < energyInMaximum)
         g.drawRect(x*35/49+4+(int)(energyIn*(x*11/49-10)/energyInMaximum), y*4/26-1,
                    (int)(heaterEnergy*(x*11/49-10)/energyInMaximum)+1, y/26+1);
      else 
         g.drawRect(x*35/49+4+(int)(energyIn*(x*11/49-10)/energyInMaximum), y*4/26-1,
                    x*11/49-10+1, y/26+1);
      if(energyOut < energyOutMaximum)
         g.drawRect(x*35/49+4, y*20/26-1,
                    (int)(energyOut*(x*11/49-10)/energyOutMaximum)+1, y/26+1);
      else
         g.drawRect(x*35/49+4, y*20/26-1, x*11/49-10+1, y/26+1);
      g.setColor(Simulator.COLOR_INOUTENERGY);
      if(temperature < temperatureMaximum)
         g.fillRect(x*19/49+5, y*20/26,
                    (int)(temperature*(x*11/49-10)/temperatureMaximum), y/26);
      else
         g.fillRect(x*19/49+5, y*20/26, x*11/49-10, y/26);
      if(energyIn < energyInMaximum)
         g.fillRect(x*35/49+5+(int)(energyIn*(x*11/49-10)/energyInMaximum), y*4/26,
                    (int)(heaterEnergy*(x*11/49-10)/energyInMaximum), y/26);
      else
         g.fillRect(x*35/49+5+(int)(energyIn*(x*11/49-10)/energyInMaximum), y*4/26,
                    x*11/49-10, y/26);
      if(energyOut < energyOutMaximum)
         g.fillRect(x*35/49+5, y*20/26, (int)(energyOut*(x*11/49-10)/energyOutMaximum), y/26);
      else
         g.fillRect(x*35/49+5, y*20/26, x*11/49-10, y/26);
      // draw energy level
      g.setColor(Color.black);
      if(energy < energyMaximum)
         g.drawRect(x*36/49-1, y*19/26-6-(int)(energy*(y*13/26-10)/energyMaximum),
                    x*7/49+1, (int)(energy*(y*13/26-10)/energyMaximum)+1);
      else
         g.drawRect(x*36/49-1, y*19/26-6-y*13/26-10, x*7/49+1, y*13/26-10+1);
      g.setColor(Simulator.COLOR_ENERGY);
      if(energy < energyMaximum)
         g.fillRect(x*36/49, y*19/26-5-(int)(energy*(y*13/26-10)/energyMaximum),
                    x*7/49, (int)(energy*(y*13/26-10)/energyMaximum));
      else
         g.fillRect(x*36/49, y*19/26-5-y*13/26-10, x*7/49, y*13/26-10);
      // draw input-output lines
      g.setColor(Color.black);
      g.drawLine(x*3/49+5+(int)(massFlowIn*(x*11/49-10)/massFlowInMaximum), y*5/26,
                 x*3/49+5+(int)(massFlowOut*(x*11/49-10)/massFlowOutMaximum), y*20/26);
      if(energyIn+heaterEnergy <= energyInMaximum && energyOut <= energyOutMaximum)
         g.drawLine(x*35/49+5+(int)((energyIn+heaterEnergy)*(x*11/49-10)/energyInMaximum),
                    y*5/26, x*35/49+5+(int)(energyOut*(x*11/49-10)/energyOutMaximum), y*20/26);
      else if(energyIn+heaterEnergy > energyInMaximum && energyOut <= energyOutMaximum)
         g.drawLine(x*35/49+5+x*11/49-10, y*5/26,
                    x*35/49+5+(int)(energyOut*(x*11/49-10)/energyOutMaximum), y*20/26);
      else if(energyIn+heaterEnergy <= energyInMaximum && energyOut > energyOutMaximum)
         g.drawLine(x*35/49+5+(int)((energyIn+heaterEnergy)*(x*11/49-10)/energyInMaximum),
                    y*5/26, x*35/49+5+x*11/49-10, y*20/26);
      // draw diagonal line
      double energyPrime = temperatureMaximum*volume*heatCapacity*density;
      g.drawLine(x*19/49, y*19/26, x*19/49, y*19/26-5);
      g.drawLine(x*19/49, y*19/26-5,
                 x*30/49-5, y*19/26-5-(int)(energyPrime*(y*13/26-10)/energyMaximum));
      // draw volume intersection line and ball
      double energyBall = volume/volumeMaximum*energyMaximum;
      double temperaturePrime = energyBall/volume/heatCapacity/density;
         g.fillOval(x*19/49+5+(int)(temperaturePrime*(x*11/49-10)/temperatureMaximum)-1,
                    y*19/26-5-(int)(volume*(y*13/26-10)/volumeMaximum)-3, 6, 6);
         g.drawLine(x*14/49-6, y*19/26-5-(int)(volume*(y*13/26-10)/volumeMaximum)-1,
                    x*19/49+5+(int)(temperaturePrime*(x*11/49-10)/temperatureMaximum)-1,
                    y*19/26-5-(int)(volume*(y*13/26-10)/volumeMaximum)-1);
      // draw current temp and energy intersection
      g.drawLine(x*19/49+5+(int)(temperature*(x*11/49-10)/temperatureMaximum)+1, y*19/26,
                 x*19/49+5+(int)(temperature*(x*11/49-10)/temperatureMaximum)+1,
                 y*19/26-5-(int)(energy*(y*13/26-10)/energyMaximum));
      g.drawLine(x*19/49+5+(int)(temperature*(x*11/49-10)/temperatureMaximum)+1,
                 y*19/26-5-(int)(energy*(y*13/26-10)/energyMaximum)-1, x*35/49+5,
                 y*19/26-5-(int)(energy*(y*13/26-10)/energyMaximum)-1);
      }

      private final double calculateEnergyGoal(double temp) {
         double energyGoal = temp*volume*heatCapacity*density;
         return energyGoal;
         }

      public final void setSettings(double mI, double v, double mO, double d, double t,
                                    double eI, double hE, double e, double eO) {
         massFlowIn = mI;
         volume = v;
         massFlowOut = mO;
         demand = d;
         temperature = t;
         energyIn = eI;
         heaterEnergy = hE;
         energy = e;
         energyOut = eO;
         repaint();
         }

   }


final class ReservoirSliderCanvas extends JPanel {

   private JSlider reservoirSlider;
   private Valve parent;
   
   //Signe: setting value for demand line
   public int setting;

   public ReservoirSliderCanvas(Valve p, int maxSet) {
      parent = p;
      
      //Signe: initialise to zero
      setting = 0;
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      reservoirSlider = new JSlider(JSlider.HORIZONTAL, 0, maxSet*10, 0); //Signe: adjusted scale to add precision
      reservoirSlider.addChangeListener(new ChangeListener() {
                                       public void stateChanged(ChangeEvent event) {
                                          JSlider source = (JSlider)event.getSource();
                                          if(!source.getValueIsAdjusting()) {
                                             //Signe: scale down values
                                             parent.setValveSetting((double)(source.getValue()/10.0));
                                             parent.setSlider((double)(source.getValue()/10.0));
                                             }
                                          }
                                       });
      reservoirSlider.setMajorTickSpacing(10*maxSet/2); //Signe: modified tick spacing.
      reservoirSlider.setMinorTickSpacing(10*maxSet/10);
      reservoirSlider.setPaintTicks(true);
      reservoirSlider.setPaintTrack(false);
      reservoirSlider.setBorder(LineBorder.createBlackLineBorder());
      add(reservoirSlider);
      }

   public final void setReservoirSlider(int newValue) {
       
       //Signe: update when value is changed
       setting = newValue;
      reservoirSlider.setValue(newValue);
      }

   }


final class HorizonLabelCanvas extends Canvas {

   private int maximum;


   public HorizonLabelCanvas(int max) {
      maximum = max;
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      g.setFont(new Font("SansSerif", Font.PLAIN, y));
      g.drawString("0", 3, y);
      g.drawString("" + maximum, x-14, y);
      
      }

   }


final class PrinciplesCanvas extends Canvas {

   private double massFlowIn;
   private double massFlowInMaximum;
   private double volume;
   private double volumeMaximum;
   private double massFlowOut;
   private double massFlowOutMaximum;
   private double energyIn;
   private double heaterEnergy;        //  Note: this includes hidden heater energy
   private double energyInMaximum;
   private double energy;
   private double energyMaximum;
   private double energyOut;
   private double energyOutMaximum;
   private String name;

   public PrinciplesCanvas(double mI, double mIM, double v, double vM,double mO, double mOM,
                                 double eI, double hE, double eIM, double e, double eM,
                                 double eO, double eOM, String name) {
      massFlowIn = mI;
      massFlowInMaximum = mIM;
      volume = v;
      volumeMaximum = vM;
      massFlowOut = mO;
      massFlowOutMaximum = mOM;
      energyIn = eI;
      heaterEnergy = hE;
      energyInMaximum = eIM;
      energy = e;
      energyMaximum = eM;
      energyOut = eO;
      energyOutMaximum = eOM;
      this.name = name;
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      g.setFont(new Font("SansSerif", Font.PLAIN, y/17));
      g.drawString(name, x*20/49, y*2/26-2);
      g.drawString("MASS IN", x*3/49, y*2/26-2);
      g.drawString("" + (int)volumeMaximum*100, x/98-2, y*7/26);
      g.drawString("M", x/49, y*13/26);
      g.drawString("0", x/49, y*18/26+5);
      g.drawString("MASS OUT", x*3/49, y*24/26+3);
      g.drawString("0", x*3/49, y-2);
      g.drawString("" + (int)massFlowOutMaximum, x*13/49-3, y-2);
      g.drawString("ENERGY IN", x*35/49, y*2/26-2);
      g.drawString("E", x*46/49+2, y*13/26);
      g.drawString("ENERGY OUT", x*35/49, y*24/26+3);

      g.drawRect(0, 0, x-1, y-1);
      // massflow meter bounds
      g.drawRect(x*3/49, y*2/26, x*11/49, y*21/26);
      g.drawLine(x*3/49, y*6/26, x*14/49, y*6/26);
      g.drawLine(x*3/49, y*19/26, x*14/49, y*19/26);
      // energy meter bounds
      g.drawRect(x*35/49, y*2/26, x*11/49, y*21/26);
      g.drawLine(x*35/49, y*6/26, x*46/49, y*6/26);
      g.drawLine(x*35/49, y*19/26, x*46/49, y*19/26);
      // draw ticks on all the meters
      for(int i=0; i<=10; i++)
         if(i%5==0) {
            g.drawLine(x*3/49+5+i*(x*11/49-10)/10, y*2/26, x*3/49+5+i*(x*11/49-10)/10, y*7/52);
            g.drawLine(x*3/49, y*6/26+5+i*(y*13/26-10)/10, x*5/49, y*6/26+5+i*(y*13/26-10)/10);
            g.drawLine(x*3/49+5+i*(x*11/49-10)/10, y*43/52, x*3/49+5+i*(x*11/49-10)/10, y*23/26);
            g.drawLine(x*35/49+5+i*(x*11/49-10)/10, y*2/26, x*35/49+5+i*(x*11/49-10)/10, y*7/52);
            g.drawLine(x*44/49, y*6/26+5+i*(y*13/26-10)/10, x*46/49, y*6/26+5+i*(y*13/26-10)/10);
            g.drawLine(x*35/49+5+i*(x*11/49-10)/10, y*43/52, x*35/49+5+i*(x*11/49-10)/10, y*23/26);
            }
         else {
            g.drawLine(x*3/49+5+i*(x*11/49-10)/10, y*2/26, x*3/49+5+i*(x*11/49-10)/10, y*3/26);
            g.drawLine(x*3/49, y*6/26+5+i*(y*13/26-10)/10, x*4/49, y*6/26+5+i*(y*13/26-10)/10);
            g.drawLine(x*3/49+5+i*(x*11/49-10)/10, y*22/26, x*3/49+5+i*(x*11/49-10)/10, y*23/26);
            g.drawLine(x*35/49+5+i*(x*11/49-10)/10, y*2/26, x*35/49+5+i*(x*11/49-10)/10, y*3/26);
            g.drawLine(x*45/49, y*6/26+5+i*(y*13/26-10)/10, x*46/49, y*6/26+5+i*(y*13/26-10)/10);
            g.drawLine(x*35/49+5+i*(x*11/49-10)/10, y*22/26, x*35/49+5+i*(x*11/49-10)/10, y*23/26);
            }
      // draw massflow and energy-in levels
      g.setColor(Color.black);
      g.drawRect(x*3/49+4, y*4/26-1, (int)(massFlowIn*(x*11/49-10)/massFlowInMaximum)+1, y/26+1);
      g.drawRect(x*3/49+4, y*20/26-1,
                 (int)(massFlowOut*(x*11/49-10)/massFlowOutMaximum)+1, y/26+1);
      g.drawRect(x*35/49+4, y*4/26-1, (int)(energyIn*(x*11/49-10)/energyInMaximum)+1, y/26+1);
      g.setColor(Simulator.COLOR_MASSFLOW);
      g.fillRect(x*3/49+5, y*4/26, (int)(massFlowIn*(x*11/49-10)/massFlowInMaximum), y/26);
      g.fillRect(x*3/49+5, y*20/26, (int)(massFlowOut*(x*11/49-10)/massFlowOutMaximum), y/26);
      g.fillRect(x*35/49+5, y*4/26, (int)(energyIn*(x*11/49-10)/energyInMaximum), y/26);
      // draw water level
      g.setColor(Color.black);
      g.drawRect(x*6/49-1, y*19/26-6-(int)(volume*(y*13/26-10)/volumeMaximum),
                 x*7/49+1, (int)(volume*(y*13/26-10)/volumeMaximum)+1);
      g.setColor(Simulator.COLOR_RESERVOIR);
      g.fillRect(x*6/49, y*19/26-5-(int)(volume*(y*13/26-10)/volumeMaximum),
                 x*7/49, (int)(volume*(y*13/26-10)/volumeMaximum));
      // draw heater-energy-in and energy-out levels
      g.setColor(Color.black);
      if(energyIn < energyInMaximum)
         g.drawRect(x*35/49+4+(int)(energyIn*(x*11/49-10)/energyInMaximum), y*4/26-1,
                    (int)(heaterEnergy*(x*11/49-10)/energyInMaximum)+1, y/26+1);
      else 
         g.drawRect(x*35/49+4+(int)(energyIn*(x*11/49-10)/energyInMaximum), y*4/26-1,
                    x*11/49-10+1, y/26+1);
      if(energyOut < energyOutMaximum)
         g.drawRect(x*35/49+4, y*20/26-1,
                    (int)(energyOut*(x*11/49-10)/energyOutMaximum)+1, y/26+1);
      else
         g.drawRect(x*35/49+4, y*20/26-1, x*11/49-10+1, y/26+1);
      g.setColor(Simulator.COLOR_INOUTENERGY);
      if(energyIn < energyInMaximum)
         g.fillRect(x*35/49+5+(int)(energyIn*(x*11/49-10)/energyInMaximum), y*4/26,
                    (int)(heaterEnergy*(x*11/49-10)/energyInMaximum), y/26);
      else
         g.fillRect(x*35/49+5+(int)(energyIn*(x*11/49-10)/energyInMaximum), y*4/26,
                    x*11/49-10, y/26);
      if(energyOut < energyOutMaximum)
         g.fillRect(x*35/49+5, y*20/26, (int)(energyOut*(x*11/49-10)/energyOutMaximum), y/26);
      else
         g.fillRect(x*35/49+5, y*20/26, x*11/49-10, y/26);
      // draw energy level
      g.setColor(Color.black);
      if(energy < energyMaximum)
         g.drawRect(x*36/49-1, y*19/26-6-(int)(energy*(y*13/26-10)/energyMaximum),
                    x*7/49+1, (int)(energy*(y*13/26-10)/energyMaximum)+1);
      else
         g.drawRect(x*36/49-1, y*19/26-6-y*13/26-10, x*7/49+1, y*13/26-10+1);
      g.setColor(Simulator.COLOR_ENERGY);
      if(energy < energyMaximum)
         g.fillRect(x*36/49, y*19/26-5-(int)(energy*(y*13/26-10)/energyMaximum),
                    x*7/49, (int)(energy*(y*13/26-10)/energyMaximum));
      else
         g.fillRect(x*36/49, y*19/26-5-y*13/26-10, x*7/49, y*13/26-10); 
      // draw input-output lines
      g.setColor(Color.black);
      g.drawLine(x*3/49+5+(int)(massFlowIn*(x*11/49-10)/massFlowInMaximum), y*5/26,
                 x*3/49+5+(int)(massFlowOut*(x*11/49-10)/massFlowOutMaximum), y*20/26);
      if(energyIn+heaterEnergy <= energyInMaximum && energyOut <= energyOutMaximum)
         g.drawLine(x*35/49+5+(int)((energyIn+heaterEnergy)*(x*11/49-10)/energyInMaximum),
                    y*5/26, x*35/49+5+(int)(energyOut*(x*11/49-10)/energyOutMaximum), y*20/26);
      else if(energyIn+heaterEnergy > energyInMaximum && energyOut <= energyOutMaximum)
         g.drawLine(x*35/49+5+x*11/49-10, y*5/26,
                    x*35/49+5+(int)(energyOut*(x*11/49-10)/energyOutMaximum), y*20/26);
      else if(energyIn+heaterEnergy <= energyInMaximum && energyOut > energyOutMaximum)
         g.drawLine(x*35/49+5+(int)((energyIn+heaterEnergy)*(x*11/49-10)/energyInMaximum),
                    y*5/26, x*35/49+5+x*11/49-10, y*20/26);
      }

      public final void setSettings(double mI, double v, double mO,
                                    double eI, double hE, double e, double eO) {
         massFlowIn = mI;
         volume = v;
         massFlowOut = mO;
         energyIn = eI;
         heaterEnergy = hE;
         energy = e;
         energyOut = eO;
         repaint();
         }

   }

final class ValveSpecialLabelCanvas extends ValveLabelCanvas {

   private double maximum;
   private double demand;
   private double margin;


   public ValveSpecialLabelCanvas(double max, double dem, double mar) {
       
       super(max);
        maximum = max;
        demand = dem;
        margin = mar;
        
      }

   public void paint(Graphics g) {
      int x = getSize().width;
      int y = getSize().height;

      g.setFont(new Font("SansSerif", Font.PLAIN, y/11));
      g.drawString("" + (int)maximum, 1, y*14/18-6-(y-y*4/18-18));
      g.drawString("0", 1, y*14/18-6);
      g.drawLine(0, y*16/18, x, y*16/18);
      
      // draw demand marker
      g.setColor(Simulator.COLOR_GOAL);
      g.fillRect(x/7-2, y*14/18-9-(int)((demand+margin)*(y-y*4/18-18)/maximum), x+2/**3/7+2*/,
                 (int)(2*margin*(y-y*4/18-18)/maximum));
      
      }

   }
