package org.eclipse.kura.example.mymqtt;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mymqtt implements ConfigurableComponent, CloudClientListener{

	private static final Logger s_logger = LoggerFactory.getLogger(Mymqtt.class); //For taking the log
	
	private static final String APP_ID = "org.eclipse.kura.example.mymqtt";
	
    private CloudService m_cloudService;
    private CloudClient m_cloudClient;

    private final ScheduledExecutorService m_worker;
    private ScheduledFuture<?> m_handle;

    private float m_temperature;
    private Map<String, Object> m_properties;
    private final Random m_random;
	
	private static final String   PUBLISH_RATE_PROP_NAME   = "publish.pubrate";
	private static final String   PUBLISH_TOPIC_PROP_NAME  = "publish.topic";
	private static final String   PUBLISH_QOS_PROP_NAME    = "publish.qos";
	private static final String   PUBLISH_RETAIN_PROP_NAME = "publish.retain";
    
	public Mymqtt() {
		super();
		this.m_worker = Executors.newSingleThreadScheduledExecutor();
		this.m_random = new Random();
	}
	
    public void setCloudService(CloudService cloudService) {
        this.m_cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.m_cloudService = null;
    }
    
    
    protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
    	s_logger.info("Activating myMQTT");
    	
    	this.m_properties = properties;
        
    	for (String s : properties.keySet()) {
            s_logger.info("Activate - " + s + ": " + properties.get(s));
        }

      //   get the mqtt client for this application
        try {
            
        	// Acquire a Cloud Application Client for this Application
            s_logger.info("Getting CloudClient for {}...", APP_ID);
            this.m_cloudClient = this.m_cloudService.newCloudClient(APP_ID);
            this.m_cloudClient.addCloudClientListener(this);
//
//            // Don't subscribe because these are handled by the default
//            // subscriptions and we don't want to get messages twice
            doUpdate(false);
        } catch (Exception e) {
            s_logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
        
//        updated(properties);
        s_logger.info("Activating myMQTT... Done.");
		
	}
    
    protected void deactivate(ComponentContext componentContext) {
        s_logger.debug("Deactivating myMQTT...");

        // shutting down the worker and cleaning up the properties
        this.m_worker.shutdown();

        // Releasing the CloudApplicationClient
        s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
        this.m_cloudClient.release();

        s_logger.debug("Deactivating myMQTT... Done.");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated Heater...");

        // store the properties received
        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Update - " + s + ": " + properties.get(s));
        }

        // try to kick off a new job
        doUpdate(true);
        s_logger.info("Updated Heater... Done.");
    }

	//Cloud Service Call Back Functions
	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub
		try {
			this.m_cloudClient.subscribe("helloVib", 0);
			
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void onConnectionLost() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onControlMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
		// TODO Auto-generated method stub
		//s_logger.info("Got a message:"+arg2.getBody());
			
	}

	@Override
	public void onMessageConfirmed(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessagePublished(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate(boolean onUpdate) {
        // cancel a current worker handle if one if active
        if (this.m_handle != null) {
            this.m_handle.cancel(true);
        }

//        if (!this.m_properties.containsKey(TEMP_INITIAL_PROP_NAME)
//                || !this.m_properties.containsKey(PUBLISH_RATE_PROP_NAME)) {
//            s_logger.info(
//                    "Update Heater - Ignore as properties do not contain TEMP_INITIAL_PROP_NAME and PUBLISH_RATE_PROP_NAME.");
//            return;
//        }

        // reset the temperature to the initial value
        if (!onUpdate) {
            this.m_temperature = 50;
        }

        // schedule a new worker based on the properties of the service
        int pubrate = 5; //Rate for publishing the data
        this.m_handle = this.m_worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName(getClass().getSimpleName());
                doPublish();
            }
        }, 0, pubrate, TimeUnit.SECONDS);
    }

    /**
     * Called at the configured rate to publish the next temperature measurement.
     */
    private void doPublish() {
        // fetch the publishing configuration from the publishing properties
        String topic = "hello";
        Integer qos = 0;
        Boolean retain = false;

        KuraPayload payload = new KuraPayload();

        // Timestamp the message
        payload.setTimestamp(new Date());
        this.m_temperature=10+m_random.nextInt();
        String body="Hello MQTT";
        payload.addMetric("temperatureInternal", this.m_temperature);
        s_logger.info(this.m_temperature+" ");
        payload.setBody(body.getBytes());
        // Publish the message
        try {
            this.m_cloudClient.publish(topic, payload, qos, retain);
            s_logger.info("Published to {} message: {}", topic, payload);
        } catch (Exception e) {
            s_logger.error("Cannot publish topic: " + topic, e);
        }
    }



	
	
	
	
	
}
