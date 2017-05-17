package actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class ChatActor extends UntypedActor {
	public static Props props(ActorRef out) {
        return Props.create(ChatActor.class, out);
    }
	
	private final ActorRef out;
	private final long userID;
	
	
    public ChatActor(ActorRef out,long userID) {
        this.out = out;
        this.userID = userID;
    }
    
	@Override
	public void onReceive(Object message) throws Throwable {
		// TODO Auto-generated method stub

	}

}
