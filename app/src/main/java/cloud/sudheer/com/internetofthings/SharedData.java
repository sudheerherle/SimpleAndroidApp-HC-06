package cloud.sudheer.com.internetofthings;

/**
 * Created by I14746 on 1/26/2015.
 */
public class SharedData {

    private static SharedData ref;
    private boolean isAppRunning = false;
    public static SharedData getSingletonObject()
    {
        if (ref == null)
            // it's ok, we can call this constructor
            ref = new SharedData();
        return ref;
    }

    public boolean isAppRunning(){
        return isAppRunning;
    }

    public void SetAppStatus(boolean running){
        isAppRunning = running;
    }

}
