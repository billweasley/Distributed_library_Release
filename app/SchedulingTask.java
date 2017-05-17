import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
//import scala.concurrent.duration.Duration;

public class SchedulingTask {

	
	final ActorSystem system;
    @Inject
    public SchedulingTask(final ActorSystem system,
                          @Named("update-db-actor") ActorRef updateDbActor) {
    	this.system = system;
    	
    }	
    	
     /*   system.scheduler().schedule(
            Duration.create(0, TimeUnit.MILLISECONDS), //Initial delay
            Duration.create(1, TimeUnit.HOURS),     //Frequency
            updateDbActor,
            "update",
            system.dispatcher(),
            null);
    }*/
}