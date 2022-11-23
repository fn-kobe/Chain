package com.scu.suhong.instantiationOptimization;

// State to indicate the processing state
// If the client request termination and the states are confirmed,
// then the process should be finished.
public enum ProcessingState {
    ENone,
    EProcessing,// No "RequestState.ETerminated" is not received
    EWaitingForConfirmation,// "RequestState.ETerminated" is received, while states are not confirmed
    EDone,//"RequestState.ETerminated" is received and states are confirmed
}
