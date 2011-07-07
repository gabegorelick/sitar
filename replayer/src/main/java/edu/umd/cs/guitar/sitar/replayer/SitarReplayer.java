/*  
 *  Copyright (c) 2009-@year@. The GUITAR group at the University of Maryland. Names of owners of this group may
 *  be obtained by sending an e-mail to atif@cs.umd.edu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *  documentation files (the "Software"), to deal in the Software without restriction, including without 
 *  limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *  the Software, and to permit persons to whom the Software is furnished to do so, subject to the following 
 *  conditions:
 * 
 *  The above copyright notice and this permission notice shall be included in all copies or substantial 
 *  portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO 
 *  EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 */
package edu.umd.cs.guitar.sitar.replayer;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.umd.cs.guitar.model.GIDGenerator;
import edu.umd.cs.guitar.model.IO;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.umd.cs.guitar.replayer.Replayer;
import edu.umd.cs.guitar.replayer.monitor.GTestMonitor;
import edu.umd.cs.guitar.replayer.monitor.PauseMonitor;
import edu.umd.cs.guitar.sitar.model.SitarDefaultIDGenerator;
import edu.umd.cs.guitar.sitar.replayer.monitor.SitarStateMonitorFull;
import edu.umd.cs.guitar.sitar.ripper.SitarExecutor;

/**
 * Adapts a {@link Replayer} for use with SWT GUIs. 
 * 
 * @author Gabe Gorelick
 *
 */
public class SitarReplayer extends SitarExecutor {

	private static Logger logger = LoggerFactory.getLogger(SitarReplayer.class);
	
	private final SitarReplayerConfiguration config;
	private final SitarReplayerMonitor monitor;
	private final Replayer replayer;

	/**
	 * Constructs a new <code>SitarReplayer</code>. This constructor is equivalent
	 * to
	 * 
	 * <pre>
	 * SitarReplayer(config, Thread.currentThread())
	 * </pre>
	 * 
	 * Consequently, this constructor must be called on the same thread that the
	 * application under test is running on (usually the <code>main</code>
	 * thread).
	 * 
	 * @param config
	 *            configuration
	 * @throws TestCaseNotFoundException if no test case was found
	 * @see SitarRunner
	 */
	public SitarReplayer(SitarReplayerConfiguration config) {
		this(config, Thread.currentThread());
	}
	
	/**
	 * Constructs a new <code>SWTRreplayer</code>. The thread passed in is the
	 * thread on which the SWT application under test runs. This is almost
	 * always the <code>main</code> thread (and actually must be the
	 * <code>main</code> thread on Cocoa).
	 * 
	 * @param config
	 *            configuration
	 * @param guiThread
	 *            thread the GUI runs on
	 * @throws TestCaseNotFoundException if no test case was found
	 * @see SitarRunner
	 */
	public SitarReplayer(SitarReplayerConfiguration config, Thread guiThread) {
		super(config, guiThread);
		this.config = config;
		this.monitor = new SitarReplayerMonitor(config, getApplication());
		replayer = initReplayer();
	}

	// initialize the replayer
	private Replayer initReplayer() {
		InputStream stream = getClass().getResourceAsStream(config.getTestcase());
		TestCase tc = (TestCase) IO.readObjFromFile(stream, TestCase.class);
		if (tc == null) {
			throw new TestCaseNotFoundException("Test case " + config.getTestcase() + " not found");
		}
		
		Replayer replayer = null;
		
		try {
			String guiFile = getClass().getResource(config.getGuiFile()).toString();
			String efgFile = getClass().getResource(config.getEfgFile()).toString();
			
			// TODO clean this up when we refactor GUITAR-Core
			replayer = new Replayer(tc, guiFile, efgFile);
			
			SitarStateMonitorFull stateMonitor = new SitarStateMonitorFull(
					config.getGuiStateFile(), config.getDelay());

			GIDGenerator idGenerator = SitarDefaultIDGenerator.getInstance();
			stateMonitor.setIdGenerator(idGenerator);

			replayer.addTestMonitor(stateMonitor);
			
			// Add a pause monitor and ignore time out monitor if needed
			if (config.getPause()) {
				GTestMonitor pauseMonitor = new PauseMonitor();
				replayer.addTestMonitor(pauseMonitor);
			} 
						
			replayer.setMonitor(monitor);
			
			// TODO enable this
//			replayer.setTimeOut(config.getTestCaseTimeout());
			
		} catch (ParserConfigurationException e) {
			logger.error("Exception caught", e);
		} catch (SAXException e) {
			logger.error("Exception caught", e);
		} catch (IOException e) {
			logger.error("Exception caught", e);
		}
		
		return replayer;		
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This implementation calls its parent's {@code onBeforeExecute} and then
	 * just does some logging.
	 */
	@Override
	protected void onBeforeExecute() {
		// do setup
		super.onBeforeExecute();
		
		logger.info("Testcase: " + config.getTestcase());
		logger.info("GUI state file: " + config.getGuiStateFile());
	}
	
	/**
	 * Execute the replayer.
	 * 
	 * @see Replayer#execute()
	 */
	@Override
	public void onExecute() {
		try {		
			replayer.execute();
		} catch (Exception e) {
			logger.error("Exception during replay", e);
		} 		
	}
	
	/**
	 * Do some logging after the replayer has finished.
	 */
	@Override
	protected void onAfterExecute() {		
		// print time elapsed
		super.onAfterExecute();		
	}

	/**
	 * Get the <code>SitarReplayerMonitor</code> associated with this
	 * <code>SitarReplayer</code>.
	 * 
	 * @return the monitor used to communicate with the GUI
	 */
	@Override
	public SitarReplayerMonitor getMonitor() {
		return monitor;
	}

}