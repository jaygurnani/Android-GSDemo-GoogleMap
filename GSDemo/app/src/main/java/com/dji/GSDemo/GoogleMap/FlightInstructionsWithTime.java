package com.dji.GSDemo.GoogleMap;


import dji.sdk.FlightController.DJIFlightControllerDataType;

public class FlightInstructionsWithTime {

    public FlightInstructionsWithTime(DJIFlightControllerDataType.DJIVirtualStickFlightControlData instructions, int time) {
        this.instructions = instructions;
        this.time = (double) time;
    }

    public FlightInstructionsWithTime(DJIFlightControllerDataType.DJIVirtualStickFlightControlData instructions, double time) {
        this.instructions = instructions;
        this.time = time;
    }

    public DJIFlightControllerDataType.DJIVirtualStickFlightControlData getInstructions() {
        return instructions;
    }

    public void setInstructions(DJIFlightControllerDataType.DJIVirtualStickFlightControlData instructions) {
        this.instructions = instructions;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    //Member variables
    private DJIFlightControllerDataType.DJIVirtualStickFlightControlData instructions;
    private double time;
}
