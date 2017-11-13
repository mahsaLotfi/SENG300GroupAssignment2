package ca.ucalgary.seng300.a2;

import org.lsmr.vending.*;
import org.lsmr.vending.hardware.*;
import java.util.Timer;
import java.util.TimerTask;



public class VendingLogic implements VendingLogicInterface {
	private VendingMachine vm;				// The vending machine that this logic program is installed on
	private int credit;					// credit is saved in terms of cents 
	private EventLogInterface EL;				// An even logger used to track vending machine interactions
	private Boolean[] circuitEnabled;			// an array used for custom configurations
	private boolean debug = true;
	private String currentMessage ="";	
	/**
	*This constructor uses a vending machine as a paramter, then creates and assigns listeners to it.
	*
	*@param VendingMachine vend is the the machine that the listeners will be registered to.
	*@return a new instance of a VendingLogic object
	*
	*/
	public VendingLogic(VendingMachine vend)
	{
		//Set up attributes
		this.vm = vend;
		credit = 0;
		EL = new EventLog();
		registerListeners();
		
		//Set up the custom configuration
		circuitEnabled = new Boolean[vm.getNumberOfSelectionButtons()];
		for (int i = 0; i < circuitEnabled.length; i++) {
			circuitEnabled[i] = false;
		}
		
	}
	
	/**
	* This method returns the event logger
	* @param None
	* @return EventLogInterface El
	*/
	public EventLogInterface getEventLog(){
		return EL;
	}
	
	/**
	* This method returns the the credit total that the vending machine has
	* @param None
	* @return Int credit
	*/
	public int getCurrencyValue(){
		return credit;
	}
	
	/**
	* This method creates and registers listeners for the vending machine.
	* @param None
	* @return None
	*/
	private void registerListeners()
	{
		//Register each of our listener objects here
		vm.getCoinSlot().register(new CoinSlotListenerDevice(this));
		vm.getDisplay().register(new DisplayListenerDevice(this));
		
		//For each coin rack create and register a listener
		for (int i = 0; i < vm.getNumberOfCoinRacks(); i++) {
			vm.getCoinRack(i).register(new CoinRackListenerDevice(this));
		}
		vm.getCoinReceptacle().register(new CoinReceptacleListenerDevice(this));
		
		//!!The current version of the vending machine is bugged. The coin return is never instantiated.!!
		// This means we are unable to register to the coin return, as we get a null pointer.
		try {
			vm.getCoinReturn().register(new CoinReturnListenerDevice(this));}
		catch(Exception e)
		{
			//This will print out the null pointer error
			if (debug) System.out.println("Coin return not instantiated! " + e);
		}
		
		//For each button create and register a listener
		for (int i = 0; i < vm.getNumberOfSelectionButtons(); i++) {
			vm.getSelectionButton(i).register(new PushButtonListenerDevice(this));
		}
		try {
		// Configuration Panel has 37 buttons.  This is a hard coded value.
		for (int i = 0; i < 37; i++) {
			vm.getConfigurationPanel().getButton(i).register(new PushButtonListenerDevice(this));
		}
		
		vm.getConfigurationPanel().getEnterButton().register(new PushButtonListenerDevice(this));
		}catch(Exception e)
		{
			if (debug)System.out.println("Invalid config setup");
		}
	}
	
	/**
	* Method for displaying a message for 5 seconds and erase it for 10s, if credit in VM is zero.
	* @param None
	* @return None
	*/
	public void welcomeMessageTimer(){
		TimerTask task = new MyTimer(vm);
		Timer timer = new Timer();
		
		//Default message timer
		while (credit == 0){
			timer.schedule(task, 10000, 5000);
		}
	}

	/**
	 * A method to push a welcome message to the display
	 */
	public void welcomeMessage() {
		vm.getDisplay().display("Welcome");
	}
	
	/**
	 * A method to send an OutOfOrder message to the display
	 */
	public void vendOutOfOrder() {
		//vm.enableSafety(); NOTE: Due to a current bug in the Vending Machine, this results in a stack overflow error
		vm.getDisplay().display("OutOfOrder");
	}
	
	/**
	 * A method to push the currently accumulated credit to the display
	 */
	public void displayCredit() {
		vm.getDisplay().display("Current Credit: $" + (((double) credit)/100));
	}
	
	/**
	 * A method to display the price of the pop at a specific index 
	 * @param index - the selection number that corresponds to the desired pop
	 */
	public void displayPrice(int index) {
		vm.getDisplay().display("Price of " + vm.getPopKindName(index) + ": $" + (((double) vm.getPopKindCost(index)) / 100));
		this.displayCredit();
	}
	
	/**
	 * Method to show that an invalid coin was inserted
	 * TODO is this an acceptible way to wait for 5 seconds?
	 */
	public void invalidCoinInserted() {
		vm.getDisplay().display("Invalid coin!");
		try {
			if(!debug) Thread.sleep(5000);			// wait for 5 seconds
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.displayCredit();
	}
	
	/**
	 * Method called by the coinSlotListener to accumulate credit when valid coins are inserted.
	 * Update the credit and update the display.  Recalculate if the exact change is possible
	 * @param coin  The Coin that was inserted
	 */
	public void validCoinInserted(Coin coin) {
		credit += coin.getValue();
		
		//Light the exact change light based on attempted change output
		if (!isExactChangePossible())
			vm.getExactChangeLight().activate();
		else 
			vm.getExactChangeLight().deactivate();
		
		this.displayCredit();
	}
	
	/**
	 * Method to confirm that the product is being dispensed 
	 */
	public void dispensingMessage() {
		vm.getDisplay().display("Despensing. Enjoy!");
	}
	
	/**
	* this method returns the current contents of the display
	* @param none
	* @return String currentMessage
	*/
	public String getCurrentMessage(){
		return currentMessage;
	}
	
	/**
	* this method sets the contents of the display, called by displayListenerDevice
	* @param String x is the new message
	* @return void
	*/
	public void setCurrentMessage(String x){
		currentMessage = x;	
	}
	
	/**
	 * A method to return change to the user
	 */
	public void returnChange() {
		if (vm.getCoinReturn() != null) {
			int[] coinKinds = getVmCoinKinds(); //vm.getCoinKindForCoinRack(0);// {200, 100, 25, 10, 5};		// legal value of Canadian coins. only types returned
			for (int i = 0; i < coinKinds.length; i++) {
				CoinRack rack = vm.getCoinRackForCoinKind(coinKinds[i]);		// the coin rack for the coin value indicated by the loop
				if (rack != null) {									// if rack = null. coin kind is not a valid change option
					while ((!vm.isSafetyEnabled()) && (credit > coinKinds[i]) && (!rack.isDisabled()) && (rack.size() > 0)) {
						try {
							rack.releaseCoin();
							credit -= coinKinds[i];			// subtracting (i) cents from the credit
						} catch (CapacityExceededException e) {
							// should never happen, receptacle full should enable the safety, which is in the loop guard
							e.printStackTrace();
						} catch (EmptyException e) {
							// should never happen, checked for in the loop guard
							e.printStackTrace();
						} catch (DisabledException e) {
							// should never happen, checked for in the loop guard
							e.printStackTrace();
						}
					}
				}
			}
		}
		else
			vm.getDisplay().display("Unable to return any changed");
		
		if (!isExactChangePossible())
			vm.getExactChangeLight().activate();
		else 
			vm.getExactChangeLight().deactivate();
	}
	
	
	/**
	 * Method finds out what coin kinds are used in the vending machine based on the number of coin racks.
	 * @return int[] coinKinds, for example {5, 10, 25, 100, 200} for canadaian currency
	 */
	public int[] getVmCoinKinds()
	{
		//first we find how many coin kinds there are
		int coinTypes = 0;
		for(int i = 0; i <100; i++) {
			//when we catch an exception we have ran out of racks, and thus coin types
			try {
			vm.getCoinKindForCoinRack(i);
			
			}catch(Exception e)
			{
				break;
			}
			coinTypes++;
			
		}
		//We use coinTypes to build an array of each coin kind
		int[] coinKinds = new int[coinTypes];
		for(int i = 0; i<coinTypes; i++)
		{
			coinKinds[i] = vm.getCoinKindForCoinRack(i);
		}
		if (debug)
		{
			for(int i = 0; i<coinKinds.length; i++) {
			System.out.println(coinKinds[i]);	
			}
		}
		return coinKinds;
	}
	
	/**
	 * a Method to determine if exact change is possible given the prices of the pop and the current credit
	 * Checks if the credit - price can be created using the available coins is the racks
	 * checks for every pop price in the machine.
	 *   
	 * @return possible - A boolean describing if it is possible to create change for every possible transaction.
	 */
	public boolean isExactChangePossible() {
		boolean possible = true;
		if (vm.getCoinReturn() != null) {
			for (int i = 0; i < vm.getNumberOfSelectionButtons(); i++) {		// get the price for every possible pop
				int credRemaining = credit;
				int price = vm.getPopKindCost(i);
				if (credRemaining >= price) {
					credRemaining -= price;
					int changePossible = 0;

					int[] coinKinds = {200, 100, 25, 10, 5};		// legal value of Canadian coins. only types returned
					for (int value = 0; value < coinKinds.length; value++) {
						CoinRack rack = vm.getCoinRackForCoinKind(coinKinds[value]);		// the coin rack for the coin value indicated by the loop
						if (rack != null) {									// if rack = null. coin kind is not a valid change option
							int coinsNeeded = 0;
							while ((!rack.isDisabled()) && (credRemaining > changePossible) && (rack.size() > coinsNeeded)) {
								coinsNeeded++;
								changePossible += coinKinds[value];			// sum of available coins
							}
						}
					}
					if (credRemaining != changePossible)		// if after going through all the coin racks, the exact change cannot be created
						possible = false;			//  return that it is not possible to 
				}
			}
		}
		else 
			possible = false;			// if the CoinReturn is not there (null) return false.
		
		return possible;
	}
	
	/** 
	 * A method to determine what action should be done when a button is pressed 
	 * TODO how is disabling a part going to affect what action is taken?
	 * @param button
	 */
	public void determineButtonAction(PushButton button) {
		boolean found = false;
		
		if(vm.isSafetyEnabled() == false) {
			// search through the selection buttons to see if the parameter button is a selection button
			for (int index = 0; (found == false) && (index < vm.getNumberOfSelectionButtons()); index++) {
				if (vm.getSelectionButton(index) == button) {
					selectionButtonAction(index);
					found = true;
				}
			}
		}
		
		// search through the configuration panel to see if the parameter button is part of these buttons
		// NOTE!!! the configuration panel has a hard coded list of 37 buttons.  If this changes it could cause an error here!
		for (int index = 0; (found == false) && (index < 37); index++) {
			if (vm.getConfigurationPanel().getButton(index) == button) {
				// TODO figure out how to configure
				found = true;
			}
		}
		
		// check to see if the button is the configuration panels enter button.
		if ((found == false) && (button == vm.getConfigurationPanel().getEnterButton())) {
			// TODO figure out how to configure
			found = true;
			
		}
		
		if (found == false) {
			throw new SimulationException("Unknown Button pressed! Could not determine action");
		}
			
	}

	/**
	 * Method to react to the press of a selection button
	 * @param index - the index of the selection button that was pressed
	 */
	public void selectionButtonAction(int index) {
		if ((vm.getPopKindCost(index) <= credit) && (circuitEnabled[index] == true)) {
			try {
				vm.getPopCanRack(index).dispensePopCan();
				this.dispensingMessage();
				credit -= vm.getPopKindCost(index);		// deduct the price of the pop
				returnChange();
				if (credit == 0)
					this.welcomeMessage();
				else
					this.displayCredit();
			} catch (DisabledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EmptyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CapacityExceededException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (circuitEnabled[index] != true) {
			vm.getDisplay().display("Option unavailable");
		}
		else {
			this.displayPrice(index);
			this.displayCredit();
		}
	}
	
	/**
	 * A method to determine which pop can rack or push button an event has occurred on
	 * needed for EventLog information
	 * 
	 * @param hardware - the hardware that the event occurred on 
	 * @return The index of the hardware according to the vending machine. -1 means error could not find
	 */
	public int findHardwareIndex(AbstractHardware<? extends AbstractHardwareListener> hardware) {
		if (hardware instanceof PopCanRack) {
			for (int index = 0; index < vm.getNumberOfPopCanRacks(); index++) {
				if (vm.getPopCanRack(index) == hardware) {
					return index;
				}
			}
		}
		
		else if (hardware instanceof PushButton) {
			for (int index = 0; index < vm.getNumberOfSelectionButtons(); index++) {
				if (vm.getSelectionButton(index) == hardware) {
					return index;
				}
			}
			
			for (int index = 0; index < 37; index++) {
				if (vm.getConfigurationPanel().getButton(index) == hardware) {
					return index;
				}
			}
		}
		
		else if (hardware instanceof CoinRack)
			for (int i = 0; i < vm.getNumberOfCoinRacks(); i++) {
				if (hardware == vm.getCoinRack(i)) {
					return i;
				}
			}
		
		return -1; // -1 will be the error index
	}
	
	/**
	 * Method to disable a piece of hardware. If hardware is a selection button or pop rack, machine can remain 
	 *   operational, otherwise, disable vending machine 
	 * @param hardware
	 */
	public void disableHardware(AbstractHardware<? extends AbstractHardwareListener> hardware) {
		if (hardware instanceof PopCanRack) {
			circuitEnabled[findHardwareIndex(hardware)] = false;
		}
		else if (hardware instanceof PushButton) {
			for (int i = 0; i < vm.getNumberOfSelectionButtons(); i++) {
				if (hardware == vm.getSelectionButton(i)) {
					circuitEnabled[i] = false;
				}
			}
		}
		else {
			vm.getOutOfOrderLight().activate();
			returnChange();
			//vm.enableSafety(); NOTE: calling enableSafety() will result in a stack overflow exception
		}
	}
	
	/**
	 * Method to disable a piece of hardware. If hardware is a selection button or pop rack, machine can remain 
	 *   operational, otherwise, disable vending machine 
	 * @param hardware
	 */
	public void enableHardware(AbstractHardware<? extends AbstractHardwareListener> hardware) {
		if (hardware instanceof PopCanRack) {
			int index = findHardwareIndex(hardware);
			if ((vm.getSelectionButton(index).isDisabled() == false) && (vm.isSafetyEnabled() == false))
				circuitEnabled[index] = true;
		}
		else if (hardware instanceof PushButton) {
			for (int i = 0; i < vm.getNumberOfSelectionButtons(); i++) {
				if (hardware == vm.getSelectionButton(i)) {
					circuitEnabled[i] = true;
				}
			}
		}
		else {
			vm.getOutOfOrderLight().deactivate();
			//vm.disableSafety(); NOTE: This may result in a stack overflow exception
			
		}
	}
	
}

