package edu.umd.cs.guitar.sitar.replayer.monitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.guitar.model.GApplication;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.IO;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.data.StepType;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;
import edu.umd.cs.guitar.replayer.monitor.StateMonitorFull;
import edu.umd.cs.guitar.replayer.monitor.TestStepEndEventArgs;

/** 
 * A {@code StateMonitorFull} without the dependency on Jemmy.
 */
public class SitarStateMonitorFull extends StateMonitorFull {

	private static final Logger logger = LoggerFactory.getLogger(SitarStateMonitorFull.class);
	
	/**
	 * Constructor.
	 * @param stateFile path to state file 
	 * @param delay the delay to use
	 */
	public SitarStateMonitorFull(String stateFile, int delay) {
		super(stateFile, delay);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * This method is identical to {@code StateMonitorFull#afterStep(TestStepEndEventArgs)}
	 * except that it does not use Jemmy to wait until the event queue is empty, since 
	 * SWTGuitar uses blocking actions on the GUI.
	 */
	@Override
	public void afterStep(TestStepEndEventArgs eStep) {
		List<StepType> lSteps = getTestCase().getStep();
		StepType step = eStep.getStep();

		GApplication app = getMonitor().getApplication();
		GUIStructure guiState = app.getCurrentState();

		// Check opened window
		Set<String> windowsAfterStep = app.getCurrentWinID();

		Set<String> windowsNew = new HashSet<String>(windowsAfterStep);

		windowsNew.removeAll(getInitialWindows());

		GUIStructureWrapper guiStateAdapter = new GUIStructureWrapper(guiState);
		// guiStateAdapter.generateID(hashcodeGenerator);
		if (windowsNew.size() > 0) {
			List<PropertyType> ID = getMonitor().selectIDProperties(eStep.getComponentType());
			AttributesType signature = new ObjectFactory().createAttributesType();
			signature.setProperty(ID);

			guiStateAdapter.addValueBySignature(signature,
					GUITARConstants.INVOKELIST_TAG_NAME, windowsNew);

		}

		if (getIdGenerator() != null) {
			getIdGenerator().generateID(guiState);
		} else {
			logger.warn("No ID Generator assigned");
		}

		step.setGUIStructure(guiState);

		lSteps.add(step);
		getTestCase().setStep(lSteps);
		IO.writeObjToFile(getTestCase(), getStateFile());
	}


}
