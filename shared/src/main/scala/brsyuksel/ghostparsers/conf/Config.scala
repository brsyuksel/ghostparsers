package brsyuksel.ghostparsers.conf

case class Queue(capacity: Int, ttl: Long)
case class Worker(size: Int, output: String)
case class HTTP(host: String, port: Int)

case class Config(queue: Queue, worker: Worker, http: HTTP)
