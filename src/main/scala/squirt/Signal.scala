package main.scala.squirt

class Signal {
  private var flag = false
  def signal {
    this.synchronized {
      flag = true
      notifyAll
    }
  }
  def await {
    this.synchronized {
      while(!flag) wait
    }
  }
}
