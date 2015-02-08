
package patterns

package object spider {

  trait WebNode[Data, Request] extends Actor with Node {

    // pathways coming into the node
    protected val in = mutable.Set[ActorRef]()
    // pathways going out of the node
    protected val out = mutable.Set[ActorRef]()
    // used to only handle a request once that travels
    // through the web
    protected var lastId:Option[UUID] = None

    def collect(req:Request): Option[Data]

    def selfNode = WebNodeRef(self, in.toList, out.toList)

    override def send(actorRef:ActorRef, m:Any) {
      recordOutput(actorRef)
      actorRef tell (m, self)
    }
    override def forward(actorRef:ActorRef, m:Any) {
      recordOutput(actorRef)
      actorRef forward m
    }
    override def actorOf(props:Props):ActorRef = {
      val actorRef = context.actorOf(props)
      recordOutput(actorRef)
      actorRef
    }
    override def reply(m:Any) {
      recordOutput(sender)
      sender ! m
    }
    def recordOutput(actorRef:ActorRef) {
      out.add(actorRef)
    }
    def recordInput(actorRef:ActorRef) {
      if (actorRef != context.system.deadLetters){
        in.add(actorRef)
      }
    }

    def wrappedReceive:Receive = {
      case m:Any if ! m.isInstanceOf[(Request,Spider)] =>
        recordInput(sender)
        before(m)
        super.receive(m)
        after(m)
    }

    abstract override def receive = handleRequest orElse wrappedReceive

    def before:Receive

    def after:Receive

    def sendSpiders(ref: ActorRef, data: Data, msg: (Request,Spider), collected: Set[ActorRef]) {
      val (request, spider) = msg
      val newTrail = spider.trail.copy(collected = collected + self)
      val newSpider = spider.copy(trail = newTrail)
      in.filterNot(in => collected.contains(in)).foreach(_ ! (request,newSpider))
      out.filterNot(out => collected.contains(out)) foreach (_ ! (request,newSpider))
    }

    def handleRequest:Receive = {
      case (req:Request, spider @ Spider(ref,WebTrail(collected, uuid))) if !lastId.exists(_ == uuid) =>
        lastId = Some(uuid)
        collect(req).map { data =>
          sendSpiders(ref, data, (req,spider), collected)
        }
    }
  }
}
