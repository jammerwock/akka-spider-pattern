
package patterns.spider

trait Node { actor:Actor =>
  def send(actorRef:ActorRef, m:Any) { actorRef.tell(m) }
  def reply(m:Any) { sender ! m }
  def forward(actorRef:ActorRef, m:Any) { actorRef.forward(m) }
  def actorOf(props:Props):ActorRef = actor.context.actorOf(props)
  def actorFor(actorPath:ActorPath):ActorRef = actor.context.actorFor(actorPath)
}
