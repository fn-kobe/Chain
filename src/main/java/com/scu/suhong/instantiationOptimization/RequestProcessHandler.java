package com.scu.suhong.instantiationOptimization;

import util.ThreadHelper;

import java.util.ArrayList;
import java.util.List;

public class RequestProcessHandler {
    private static final int runInterval = 1;// 1 seconds
    RequestProcessingUtility requestProcessingUtility;
    List<String> stateListToBeConfirmed;
    RequestProviderInterface requestProvider;

    public RequestProcessHandler(RequestProviderInterface requestProvider) {
        this.requestProvider = requestProvider;
        requestProcessingUtility = new RequestProcessingUtility(requestProvider);
        stateListToBeConfirmed = new ArrayList<>();
    }

    public ProcessingState processRequest() {
        if (RequestState.ETerminated == requestProcessingUtility.processRequest()) {
            requestProcessingUtility.waitStateToBeConfirmed(stateListToBeConfirmed);
            return ProcessingState.EDone;
        } else {
            return ProcessingState.EProcessing;
        }
    }

    public void setStateToBeConfirmed(String stateToBeConfirmed) {
        stateListToBeConfirmed.add(stateToBeConfirmed);
    }

    // Facilitate for user not to create a new thread
    public void processRequestInSeparateThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.printf("[RequestProcessHandler][processRequestInSeparateThread][%s][Info] Try to process request in separate thread %d",
                        requestProvider.getIdentify(), Thread.currentThread().getId());
                while (true) {
                    if (ProcessingState.EDone == processRequest()){
                        System.out.printf("[RequestProcessHandler][processRequestInSeparateThread][%s][Info] Termination request has been received and states are confirmed in blockchain." +
                                        "Exiting request processing thread\n",
                                requestProvider.getIdentify());
                        break;
                    }
                    ThreadHelper.safeSleepSecond(runInterval);
                }
            }
        });
        thread.start();
    }
}
