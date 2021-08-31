package com.amazonaws.samples;

import java.util.Timer;
import java.util.TimerTask;

public class InstaceSetTimer {
	Timer timer;
	public InstaceSetTimer(String instanceId,String inputValue) {
		timer = new Timer();
		timer.schedule(new RunTask(instanceId, inputValue),30000); // Timer of 30 seconds
	}
	
	public class RunTask extends TimerTask {
		String instaceId,inputValue = null;
		 public RunTask(String id, String ipValue) {
			 instaceId = id;
			 inputValue = ipValue;
		 }
		 public void run() {
			System.out.println("Time's up!"+instaceId);
			timer.cancel(); //Terminate the timer thread
		}
	}
}
