package com.dji.GSDemo.GoogleMap;


import dji.sdk.FlightController.DJIFlightControllerDataType;

public class FlightInstructionsWithTime {

    public FlightInstructionsWithTime(DJIFlightControllerDataType.DJIVirtualStickFlightControlData instructions, int time) {
        this.instructions = instructions;
        this.time = time;
    }

    public DJIFlightControllerDataType.DJIVirtualStickFlightControlData getInstructions() {
        return instructions;
    }

    public void setInstructions(DJIFlightControllerDataType.DJIVirtualStickFlightControlData instructions) {
        this.instructions = instructions;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    //Member variables
    private DJIFlightControllerDataType.DJIVirtualStickFlightControlData instructions;
    private int time;
}
