package com.amazonaws.samples;
public class main {
	 public static void main(String[] args) throws Exception {
		 
		  final LaunchController launchThread = new LaunchController();
		  final MonitorController monitorThread = new MonitorController();
		  Runnable a = new Runnable() {
		    public void run() {
		    	launchThread.start();
		    }
		  };
		  Runnable b = new Runnable() {
		    public void run() {
		    	monitorThread.start();
		    }
		  };
		    new Thread(a).start();
		    new Thread(b).start();
	  }
}
