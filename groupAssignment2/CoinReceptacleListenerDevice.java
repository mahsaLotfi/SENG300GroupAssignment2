package groupAssignment2;

import org.lsmr.vending.Coin;
import org.lsmr.vending.hardware.AbstractHardware;
import org.lsmr.vending.hardware.AbstractHardwareListener;
import org.lsmr.vending.hardware.CoinReceptacle;
import org.lsmr.vending.hardware.CoinReceptacleListener;

public class CoinReceptacleListenerDevice implements CoinReceptacleListener{

	private VendingLogic logic;
	public int enabledCount = 0;
	public int disabledCount = 0;
	public int coinValue = 0;
	public int coinCount = 0;
	public boolean receptaclesFull = false;
	
	public CoinReceptacleListenerDevice(VendingLogic logic)
	{
		this.logic = logic;
	}
	
	
	@Override
	public void enabled(AbstractHardware<? extends AbstractHardwareListener> hardware) {
	    enabledCount++;
	}

	@Override
	public void disabled(AbstractHardware<? extends AbstractHardwareListener> hardware) {
	    disabledCount++;
	}

	/**
     * An event that announces that the indicated coin has been added to the
     * indicated receptacle.
     * 
     * @param receptacle
     *            The receptacle where the event happened.
     * @param coin
     *            The coin added.
     */
	@Override
	public void coinAdded(CoinReceptacle receptacle, Coin coin) {
		coinValue += coin.getValue();
		coinCount++;
		
	}

	/**
     * An event that announces that all coins have been removed from the
     * indicated receptacle.
     * 
     * @param receptacle
     *            The receptacle where the event happened.
     */
	@Override
	public void coinsRemoved(CoinReceptacle receptacle) {
		coinValue = 0;
		coinCount = 0;
		
	}

	/**
     * An event that announces that the indicated receptacle is now full.
     * 
     * @param receptacle
     *            The receptacle where the event happened.
     */
	@Override
	public void coinsFull(CoinReceptacle receptacle) {
		if(receptacle.getCapacity()<=receptacle.size()) {
			receptaclesFull = true;
		}
		else receptaclesFull = false;
	}

	/**
     * Announces that the indicated sequence of coins has been added to the
     * indicated coin receptacle. Used to simulate direct, physical loading of
     * the receptacle.
     * 
     * @param receptacle
     *            The receptacle where the event occurred.
     * @param coins
     *            The coins that were loaded.
     */
	@Override
	public void coinsLoaded(CoinReceptacle receptacle, Coin... coins) {
		for(Coin coin:coins) {
			coinValue += coin.getValue();
			coinCount ++;
		}
		
	}

	/**
     * Announces that the indicated sequence of coins has been removed to the
     * indicated coin receptacle. Used to simulate direct, physical unloading of
     * the receptacle.
     * 
     * @param receptacle
     *            The receptacle where the event occurred.
     * @param coins
     *            The coins that were unloaded.
     */
	@Override
	public void coinsUnloaded(CoinReceptacle receptacle, Coin... coins) {
		for(Coin coin: coins) {
			coinValue -= coin.getValue();
			coinCount --;
		}
		
	}

}
